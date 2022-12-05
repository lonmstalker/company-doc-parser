package com.zuzex.algorithm.benchmark;

import com.zuzex.algorithm.ParseAlgorithm;
import com.zuzex.algorithm.impl.ParallelAlgorithm;
import com.zuzex.algorithm.impl.SequenceAlgorithm;
import com.zuzex.config.AppProperties;
import com.zuzex.parse.DirectoryParser;
import com.zuzex.reader.impl.JacksonXmlReader;
import com.zuzex.reader.impl.MetaCsvReaderImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import static com.zuzex.algorithm.utils.BenchUtils.*;
import static com.zuzex.constants.Constants.*;

@Slf4j
@Threads(1)
@State(Scope.Benchmark)
public class AlgoBenchmark {
    private static final String ARCHIVE_FOLDER = "archives";
    public static AppProperties APP_PROPERTIES;
    public static DirectoryParser DIRECTORY_PARSER;
    public static ParseAlgorithm SEQUENCE_ALGORITHM;
    public static ParseAlgorithm PARALLEL_ALGORITHM;

    @Setup(Level.Invocation)
    public void setup() {
        PARALLEL_ALGORITHM = new ParallelAlgorithm(
                new JacksonXmlReader(),
                MOCK_DATA_SERVICE,
                DIRECTORY_PARSER,
                new ExecutorCompletionService<>(
                        Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors())));
        SEQUENCE_ALGORITHM = new SequenceAlgorithm(new JacksonXmlReader(), MOCK_DATA_SERVICE, DIRECTORY_PARSER);
        DIRECTORY_PARSER = new DirectoryParser(APP_PROPERTIES, new MetaCsvReaderImpl());
        APP_PROPERTIES = new AppProperties(ARCHIVE_FOLDER);
    }

    @Setup(Level.Invocation)
    public void deleteFiles() throws IOException {
        FileUtils.deleteDirectory(new File(CURRENT_PATH + PATH_DELIMITER + APP_PROPERTIES.getArchiveFolder() + PATH_DELIMITER + "unzip"));
        log.info(
                "After test given data count: '{}' savedCompanies: '{}'",
                RESULT_SET.size(),
                COUNT_SAVE_COMPANIES.get());
        RESULT_SET.clear();
    }

    @Benchmark
    public void bench_parallel() throws FileNotFoundException {
        PARALLEL_ALGORITHM.parse(DIRECTORY_PARSER.findArchives());
    }

    @Benchmark
    public void bench_sequence() throws FileNotFoundException {
        SEQUENCE_ALGORITHM.parse(DIRECTORY_PARSER.findArchives());
    }
}
