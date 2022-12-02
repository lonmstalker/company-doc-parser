package com.zuzex.algorithm.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.zuzex.algorithm.ParseAlgorithm;
import com.zuzex.parse.DirectoryParser;
import com.zuzex.xml.XmlReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.Objects.nonNull;

/**
 * Перед парсингом кладем в мапу имя дочерних компаний и имя файла, в котором они находятся
 * Если парсинг основного файла неуспешен, собираем все эти файлы дочерних и обновляем по ним
 */
public class ParallelAlgorithm implements ParseAlgorithm {
    private final XmlReader xmlReader;
    private final DirectoryParser directoryParser;
    private static final int PARALLELISM = Runtime.getRuntime().availableProcessors();
    private static final ExecutorCompletionService<Object> EXECUTOR = new ExecutorCompletionService<>(Executors.newWorkStealingPool(PARALLELISM));

    public ParallelAlgorithm(final XmlReader xmlReader, final DirectoryParser directoryParser) {
        this.xmlReader = xmlReader;
        this.directoryParser = directoryParser;
    }

    @Override
    public void parse(final LinkedBlockingQueue<File> archives) {
        try {
            final var archive = archives.peek();
            if (nonNull(archive)) {
                Files.createDirectories(Path.of(archive.getParent() + "\\unzip\\"));
            }
            this.awaitJobs(this.startParseJobs(archives));
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    private void awaitJobs(final List<Future<Object>> jobs) throws ExecutionException, InterruptedException {
        for (final Future<Object> job : jobs) {
            job.get();
        }
    }

    private List<Future<Object>> startParseJobs(final LinkedBlockingQueue<File> archives) {
        final var jobs = new ArrayList<Future<Object>>(PARALLELISM + 1);
        for (int i = 0; i < PARALLELISM; i++) {
            jobs.add(EXECUTOR.submit(this.getParseJob(archives, i), null));
        }
        return jobs;
    }

    private Runnable getParseJob(final LinkedBlockingQueue<File> archives, final int threadNumber) {
        return () -> {
            try (final var byteStream = new ByteArrayOutputStream()) {
                final var unzipArchives = this.unzip(archives, threadNumber);
                while (!unzipArchives.isEmpty()) {
                    try (final var files = Files.walk(unzipArchives.poll())) {
                        files.filter(Files::isRegularFile).forEach(path -> this.doSomething(byteStream, path));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private ArrayDeque<Path> unzip(final LinkedBlockingQueue<File> archives, int threadNumber) throws IOException {
        final var threadArchives = new ArrayDeque<Path>();
        while (!archives.isEmpty()) {
            final var archive = archives.poll();
            if (nonNull(archive)) {
                threadArchives.add(this.directoryParser.unzip(archive, threadNumber));
            }
        }
        return threadArchives;
    }

    private void doSomething(final ByteArrayOutputStream byteStream, final Path path) {
        try {
            final byte[] data = this.directoryParser.readBytes(byteStream, new File(path.toString()));
            System.out.println(this.catchRead(data));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode catchRead(final byte[] bytes) {
        try {
            return xmlReader.readNode(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
