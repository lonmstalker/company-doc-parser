package com.zuzex.algorithm.utils;

import com.zuzex.algorithm.ParseAlgorithm;
import com.zuzex.algorithm.impl.ParallelAlgorithm;
import com.zuzex.parse.DirectoryParser;
import com.zuzex.xml.impl.JacksonXmlReader;
import org.apache.commons.io.FileUtils;

import java.io.File;

public class BenchUtils {
    public static final String ARCHIVE_FOLDER = "archives";
    public static final String RESULT_FILE = "bench\\algo.json";
    public static final String CURRENT_PATH = System.getProperty("user.dir");

    public static final DirectoryParser DIRECTORY_PARSER = new DirectoryParser(CURRENT_PATH, ARCHIVE_FOLDER);
    public static final ParseAlgorithm PARALLEL_ALGORITHM = new ParallelAlgorithm(new JacksonXmlReader(), DIRECTORY_PARSER);

    public static void validArchives() {
        final var archive = new File(ARCHIVE_FOLDER);
        final var isArchive = FileUtils.isDirectory(archive);
        if (!isArchive) {
            throw new RuntimeException("Not found archive directory");
        }
        final var elements = archive.listFiles();
        if (elements == null || elements.length == 0) {
            throw new RuntimeException("Archive's folder is empty");
        }
    }
}
