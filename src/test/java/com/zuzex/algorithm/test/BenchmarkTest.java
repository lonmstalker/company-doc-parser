package com.zuzex.algorithm.test;

import org.openjdk.jmh.annotations.Benchmark;

import java.io.FileNotFoundException;

import static com.zuzex.algorithm.utils.BenchUtils.DIRECTORY_PARSER;
import static com.zuzex.algorithm.utils.BenchUtils.PARALLEL_ALGORITHM;

public class BenchmarkTest {

    @Benchmark
    public void bench_straight() throws FileNotFoundException {
        PARALLEL_ALGORITHM.parse(DIRECTORY_PARSER.findArchives());
    }
}
