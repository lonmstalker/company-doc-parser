package com.zuzex.algorithm.algorithm;

import com.zuzex.parse.DirectoryParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.zuzex.algorithm.utils.BenchUtils.RESULT_FILE;

@Slf4j
@Order(2)
@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class RunnerBenchmarkTest {

    @Test
    public void should_explain_parsers() throws RunnerException, IOException {
        DirectoryParser.recreateFile(RESULT_FILE);
        final var opt =
                new OptionsBuilder()
                        .forks(1)
                        .warmupForks(1)
                        .warmupIterations(3)
                        .warmupTime(TimeValue.seconds(2))
                        .measurementIterations(3)
                        .shouldDoGC(true)
                        .shouldFailOnError(true)
                        .threads(1)
                        .syncIterations(true)
                        .timeUnit(TimeUnit.NANOSECONDS)
                        .resultFormat(ResultFormatType.JSON)
                        .mode(Mode.AverageTime)
                        .result(RESULT_FILE)
                        .jvmArgs("-Xmx512m", "-Xms512m", "-XX:+UseG1GC", "-XX:+UseStringDeduplication") // avg time and before each invocation is very hard program
                        .verbosity(VerboseMode.NORMAL)
                        .build();
        new org.openjdk.jmh.runner.Runner(opt).run();
    }
}
