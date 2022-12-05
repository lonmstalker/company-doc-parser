package com.zuzex.algorithm.utils;

import com.zuzex.algorithm.ParseAlgorithm;
import com.zuzex.algorithm.impl.ParallelAlgorithm;
import com.zuzex.algorithm.impl.SequenceAlgorithm;
import com.zuzex.config.AppProperties;
import com.zuzex.data.algorithm.CompanyData;
import com.zuzex.parse.DirectoryParser;
import com.zuzex.reader.impl.JacksonXmlReader;
import com.zuzex.reader.impl.MetaCsvReaderImpl;
import com.zuzex.service.DataService;
import org.apache.commons.io.FileUtils;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zuzex.constants.Constants.*;
import static org.mockito.ArgumentMatchers.any;

public class BenchUtils {
    public static final String RESULT_FILE = CURRENT_PATH + PATH_DELIMITER + "bench" + PATH_DELIMITER + "algo.json";
    public static final Set<String> RESULT_SET = new HashSet<>();
    public static final DataService MOCK_DATA_SERVICE = Mockito.mock(DataService.class);
    public static final AtomicInteger COUNT_SAVE_COMPANIES = new AtomicInteger(0);

    static {
        Mockito.doAnswer(
                        invocationOnMock -> {
                            var data = (CompanyData) invocationOnMock.getArguments()[0];
                            RESULT_SET.add(data.mainCompanyName());
                            COUNT_SAVE_COMPANIES.addAndGet(data.references().size());
                            return null;
                        })
                .when(MOCK_DATA_SERVICE)
                .processData(any(), any());
    }

    public static void validArchives(final String archiveFolder) {
        final var archive = new File(CURRENT_PATH + PATH_DELIMITER + archiveFolder);
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
