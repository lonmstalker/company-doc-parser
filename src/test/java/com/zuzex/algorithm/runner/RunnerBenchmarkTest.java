package com.zuzex.algorithm.runner;

import com.zuzex.parse.DirectoryParser;
import org.junit.jupiter.api.*;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileNotFoundException;

import static com.zuzex.algorithm.utils.BenchUtils.*;
import static com.zuzex.algorithm.utils.BenchUtils.DIRECTORY_PARSER;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RunnerBenchmarkTest {

    @Test
    @Order(1)
    public void should_validate_data() {
        validArchives();
    }

    @Test
    @Order(2)
    public void should_validate_logic() {
        Assertions.assertDoesNotThrow(() -> PARALLEL_ALGORITHM.parse(DIRECTORY_PARSER.findArchives()));
    }

    @Test
    @Order(3)
    public void should_explain_parsers() throws RunnerException {
        DirectoryParser.recreateFile(RESULT_FILE);
        final var opt = new OptionsBuilder()
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(3)
                .shouldDoGC(true)
                .shouldFailOnError(true)
                .threads(1)
                .resultFormat(ResultFormatType.JSON)
                .mode(Mode.AverageTime)
                .result(RESULT_FILE)
                .build();
        new org.openjdk.jmh.runner.Runner(opt).run();
    }
}