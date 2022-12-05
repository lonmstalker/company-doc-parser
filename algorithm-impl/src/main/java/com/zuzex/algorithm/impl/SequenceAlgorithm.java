package com.zuzex.algorithm.impl;

import com.zuzex.algorithm.ParseAlgorithm;
import com.zuzex.data.algorithm.ArchiveData;
import com.zuzex.parse.DirectoryParser;
import com.zuzex.reader.XmlReader;
import com.zuzex.service.DataService;
import com.zuzex.utils.NodeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.zuzex.constants.Constants.UNZIP_PREFIX;
import static java.util.Objects.nonNull;

@Slf4j
@Component
@AllArgsConstructor
public class SequenceAlgorithm implements ParseAlgorithm {
    private final XmlReader xmlReader;
    private final DataService dataService;
    private final DirectoryParser directoryParser;
    private final List<String> dataList = new ArrayList<>();
    private static final boolean LOG_INFO = log.isInfoEnabled();
    private static final boolean LOG_DEBUG = log.isDebugEnabled();

    @Override
    public void parse(LinkedBlockingQueue<File> archives) {
        try (final var byteStream = new ByteArrayOutputStream()) {
            final var archive = archives.peek();
            if (nonNull(archive)) {
                Files.createDirectories(Path.of(archive.getParent() + UNZIP_PREFIX));
            }

            final var archiveList = new ArrayList<ArchiveData>(10 * 3 / 2);
            long startTime = System.nanoTime();
            final var archiveQueue =
                    this.directoryParser.unzipAndReadMeta(archives, archiveData -> this.checkUnzip(archiveData, archiveList));
            long endTime = System.nanoTime() - startTime;
            log.info("Unzip time. ns='{}', ms ='{}'", endTime, TimeUnit.NANOSECONDS.toMillis(endTime));

            startTime = System.nanoTime();
            while (!archiveQueue.isEmpty()) {
                final var archiveData = archiveQueue.poll();
                if (nonNull(archiveData)) {
                    try (final var files = Files.walk(archiveData.archivePath())) {
                        files.filter(Files::isRegularFile).forEach(path -> this.doSomething(byteStream, path));
                    }

                    // архив прочитан
                    this.dataService.setReadArchive(archiveData.archiveName());
                }
            }
            endTime = System.nanoTime() - startTime;
            log.info("Parse time. ns='{}', ms ='{}'", endTime, TimeUnit.NANOSECONDS.toMillis(endTime));

            if (LOG_INFO) {
                log.info("Parsed unique ogrn and ogrip: '{}'", this.dataList.size());
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    // помечаем в базе разархивированный архив
    private void checkUnzip(final ArchiveData archiveData, final List<ArchiveData> archiveList) {
        if (LOG_DEBUG) {
            log.debug("Unpacked '{}'", archiveData.archiveName());
        }
        if (archiveList.size() % 10 == 0) {
            this.dataService.addArchives(archiveList);
            archiveList.clear();
        } else {
            archiveList.add(archiveData);
        }
    }

    private void doSomething(final ByteArrayOutputStream byteStream, final Path path) {
        try {
            final var data =
                    this.xmlReader.catchRead(this.directoryParser.readBytes(byteStream, path.toFile()));
            final var companyData = NodeUtils.successParse(data, path.getFileName().toString());
            if (nonNull(companyData)) {
                dataList.add(companyData.mainCompanyName());
                this.dataService.processData(companyData, data);
            }
        } catch (final IOException ignored) {
        }
    }
}
