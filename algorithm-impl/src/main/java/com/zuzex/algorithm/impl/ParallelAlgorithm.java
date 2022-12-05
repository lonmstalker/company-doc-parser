package com.zuzex.algorithm.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.zuzex.algorithm.ParseAlgorithm;
import com.zuzex.data.algorithm.ArchiveData;
import com.zuzex.data.algorithm.CompanyData;
import com.zuzex.parse.DirectoryParser;
import com.zuzex.reader.XmlReader;
import com.zuzex.service.DataService;
import com.zuzex.thread.BatchSaver;
import com.zuzex.utils.LockUtils;
import com.zuzex.utils.NodeUtils;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.zuzex.constants.Constants.UNZIP_PREFIX;
import static com.zuzex.utils.LockUtils.WAITING_QUEUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Component
@AllArgsConstructor
public class ParallelAlgorithm implements ParseAlgorithm {
    private final XmlReader xmlReader;
    private final DataService dataService;
    private final DirectoryParser directoryParser;
    private final ExecutorCompletionService<Object> executor;
    private static final boolean AWAIT_AFTER_PARSE_ALL = false;
    private static final boolean LOG_INFO = log.isInfoEnabled();
    private static final boolean LOG_DEBUG = log.isDebugEnabled();
    private static final int PARALLELISM = Runtime.getRuntime().availableProcessors() - 1; // мастер и батчер - отдельно
    private static final AtomicInteger CURRENT_WORKERS = new AtomicInteger(PARALLELISM);
    private static final ConcurrentHashMap.KeySetView<String, Boolean> SUCCESS_TRANSACTIONS =
            ConcurrentHashMap.newKeySet();

    @Override
    public void parse(final LinkedBlockingQueue<File> archives) {
        try {
            final var archive = archives.peek();
            final var end = new AtomicBoolean(false);
            final var await = new AtomicBoolean(true);

            final var pathQueue = new LinkedBlockingQueue<Path>();
            final var jobs = new ArrayList<Future<Object>>(PARALLELISM + (PARALLELISM * 3 / 2));

            if (nonNull(archive)) {
                Files.createDirectories(Path.of(archive.getParent() + UNZIP_PREFIX));
            }

            // параллельно разархивируем и кладем в первый слой, в ответ только очередь с путями архивов и их метадатой из csv
            long startTime = System.nanoTime();
            final var unzipData = this.unzip(jobs, archives);
            long endTime = System.nanoTime() - startTime;
            log.info("Unzip time. ns='{}', ms ='{}'", endTime, TimeUnit.NANOSECONDS.toMillis(endTime));

            // параллельно парсим
            startTime = System.nanoTime();
            this.executor.submit(this.getMaster(unzipData, pathQueue, end, await), null);
            this.awaitJobs(this.startJobs(jobs, () -> this.executor.submit(this.getParseJob(pathQueue, end, await), null)), false);
            endTime = System.nanoTime() - startTime;
            log.info("Parse time. ns='{}', ms ='{}'", endTime, TimeUnit.NANOSECONDS.toMillis(endTime));

            if (LOG_INFO) {
                log.info("Parsed unique ogrn and ogrip: '{}'", SUCCESS_TRANSACTIONS.size());
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        } finally {
            SUCCESS_TRANSACTIONS.clear();
        }
    }

    // ожидание джобов
    @SuppressWarnings("unchecked")
    private <T> PriorityQueue<T> awaitJobs(final List<Future<Object>> jobs, boolean result)
            throws ExecutionException, InterruptedException {
        final var res = result ? new PriorityQueue<T>() : null;
        if (result) {
            for (final var job : jobs) {
                res.addAll((PriorityQueue<T>) job.get());
            }
        } else {
            for (final var job : jobs) {
                job.get();
            }
        }
        return res;
    }

    private List<Future<Object>> startJobs(
            final List<Future<Object>> jobs, final Supplier<Future<Object>> task) {
        for (int i = 0; i < PARALLELISM; i++) {
            jobs.add(task.get());
        }
        return jobs;
    }

    private PriorityQueue<ArchiveData> unzip(final List<Future<Object>> jobs, final Queue<File> archives) throws ExecutionException, InterruptedException {
        final var endSaver = new AtomicBoolean(false);
        final var archiveBarrier = new LinkedBlockingQueue<ArchiveData>();

        executor.submit(new BatchSaver(endSaver, this.dataService, archiveBarrier), null);
        final Callable<Object> unzipRunnable = () ->
                this.directoryParser.unzipAndReadMeta(archives, archiveData -> this.checkUnzip(archiveData, archiveBarrier));
        final var res = this.<ArchiveData>awaitJobs(this.startJobs(jobs, () -> this.executor.submit(unzipRunnable)), true);

        endSaver.set(true);
        jobs.clear();

        return res;
    }

    // помечаем в базе разархивированный архив
    // https://www.baeldung.com/java-longadder-and-longaccumulator
    private void checkUnzip(final ArchiveData archiveData, final LinkedBlockingQueue<ArchiveData> archiveBarrier) {
        if (LOG_DEBUG) {
            log.debug("Unpacked '{}'", archiveData.archiveName());
        }
        archiveBarrier.add(archiveData);
    }

    private Runnable getParseJob(final LinkedBlockingQueue<Path> pathQueue, final AtomicBoolean end, final AtomicBoolean await) {
        return () -> {
            try (final var byteStream = new ByteArrayOutputStream()) {
                while (!end.get()) {
                    while (await.get()) {
                        final var path = pathQueue.poll();
                        if (nonNull(path)) {
                            this.parseAndProcess(byteStream, path);
                        }
                    }
                    while (!WAITING_QUEUE.isEmpty()) {
                        final var waitingData = WAITING_QUEUE.poll();
                        if (nonNull(waitingData)) {
                            this.processData(waitingData.getKey(), waitingData.getValue());
                        }
                    }
                    // ждем отсальных
                    this.threadFinished(await);
                }
            } catch (Exception e) {
                log.error("Error in slave: ", e);
            } catch (Error e) {
                log.error("error: ", e);
                throw e;
            }
        };
    }

    // выдает задачи
    private Thread getMaster(
            final PriorityQueue<ArchiveData> archives,
            final LinkedBlockingQueue<Path> pathQueue,
            final AtomicBoolean end,
            final AtomicBoolean await
    ) {
        return new Thread(() -> {
                    try {
                        this.doMasterLogic(archives, pathQueue, end, await);
                    } catch (Exception ex) {
                        log.error("Error in master: ", ex);
                    }
                });
    }

    private void doMasterLogic(
            final PriorityQueue<ArchiveData> archives,
            final LinkedBlockingQueue<Path> pathQueue,
            final AtomicBoolean end,
            final AtomicBoolean await
    ) throws InterruptedException, IOException {
        int awaitMillis;
        while (true) {

        final var archiveData = archives.poll();
        if (nonNull(archiveData)) {
            if (LOG_DEBUG) {
                log.debug("Archives count in queue = {}", archives.size());
            }
            this.resetSlaves(await);
            try (final var files = Files.walk(archiveData.archivePath())) {
                files.filter(Files::isRegularFile).forEach(path -> this.putPath(pathQueue, path));

                // ждем пока разберут все файлы
                final var currentPathSize = pathQueue.size();
                if (currentPathSize <= 10) {
                    awaitMillis = 5;
                } else {
                    awaitMillis = currentPathSize > 100 ? 500 : 100;
                }
                while (!pathQueue.isEmpty()) {
                    if (LOG_DEBUG) {
                        log.debug("Current path's size = {}", pathQueue.size());
                    }
                    TimeUnit.MILLISECONDS.sleep(awaitMillis);
                }

                // отпускаем воркеров
                await.set(false);

                // ждем пока очередь с ожидающими архивами распарится
                awaitMillis = WAITING_QUEUE.size() > 100 ? 100 : 10;
                while (!WAITING_QUEUE.isEmpty()) {
                    if (LOG_DEBUG) {
                        log.debug("Current waiting queue's size = {}", WAITING_QUEUE.size());
                    }
                    TimeUnit.MILLISECONDS.sleep(awaitMillis);
                }

                // архив прочитан
                this.dataService.setReadArchive(archiveData.archiveName());

                // ждем рабочие потоки
                this.awaitWorkers(null);
            }
        }
        // если тесты и нет работы - заканчиваем
        if (archives.isEmpty() && !AWAIT_AFTER_PARSE_ALL) {
            end.set(true);
            break;
        }
        // чистим транзакции после парсинга архива
        SUCCESS_TRANSACTIONS.clear();
        }
    }

    private void putPath(final LinkedBlockingQueue<Path> paths, final Path path) {
        try {
            paths.put(path);
        } catch (final InterruptedException ignored) {
        }
    }

    private void parseAndProcess(final ByteArrayOutputStream byteStream, final Path path) {
        try {
            final var data =
                    this.xmlReader.catchRead(this.directoryParser.readBytes(byteStream, path.toFile()));
            final var companyData = NodeUtils.successParse(data, path.getFileName().toString());
            if (nonNull(companyData)) {
                this.processData(companyData, data);
            }
        } catch (final IOException ignored) {
        }
    }

    @SneakyThrows
    private void processData(final CompanyData companyData, final JsonNode node) {
        boolean successProcessing = false;
        String[] lockedChildCompanies = null;
        final var references = companyData.references();
        final var mainCompanyName = companyData.mainCompanyName();
        try {
            if (!LockUtils.tryLockCompany(mainCompanyName)) {
                WAITING_QUEUE.put(Pair.of(companyData, node));
                return;
            }
            if (!references.isEmpty()) {
                lockedChildCompanies = LockUtils.acquireLock(companyData, SUCCESS_TRANSACTIONS, node);
                if (isNull(lockedChildCompanies)) {
                    return;
                }
            }
            this.dataService.processData(companyData, node);
            successProcessing = true;
        } finally {
            if (successProcessing) {
                SUCCESS_TRANSACTIONS.add(mainCompanyName);
                this.successTransaction(lockedChildCompanies);
            }
            LockUtils.unlockCompany(companyData.mainCompanyName());
            LockUtils.nullCheckReleaseLock(lockedChildCompanies);
        }
    }

    private void successTransaction(final String[] lockedChildCompanies) {
        if (nonNull(lockedChildCompanies)) {
            for (final var lockedChildCompany : lockedChildCompanies) {
                if (nonNull(lockedChildCompany)) {
                    SUCCESS_TRANSACTIONS.add(lockedChildCompany);
                }
            }
        }
    }

    private void threadFinished(final AtomicBoolean await) {
        CURRENT_WORKERS.decrementAndGet();
        this.awaitWorkers(await);
    }

    private void resetSlaves(final AtomicBoolean await) {
        await.set(true);
        CURRENT_WORKERS.set(PARALLELISM);
    }

    // await - только для джобов, чтобы не застряли
    @SneakyThrows
    private void awaitWorkers(@Nullable final AtomicBoolean await) {
        if (nonNull(await)) {
            while (CURRENT_WORKERS.get() > 0) {
                if (await.get()) {
                    break;
                }
                TimeUnit.MILLISECONDS.sleep(5);
            }
        } else {
            int mastWaitTime = 10;
            while (CURRENT_WORKERS.get() > 0) {
                // ждем пока воркеры закончат со своей рабоой
                if (LOG_DEBUG) {
                    log.debug("Now workers count = {}", CURRENT_WORKERS.get());
                }
                TimeUnit.MILLISECONDS.sleep(mastWaitTime);
                mastWaitTime = mastWaitTime + 5;
            }
        }
    }
}
