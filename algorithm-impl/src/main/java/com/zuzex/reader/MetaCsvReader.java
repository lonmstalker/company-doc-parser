package com.zuzex.reader;

import com.zuzex.data.parse.ArchiveMetadata;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface MetaCsvReader {
    Map<String, ArchiveMetadata> readDate(final Path filePath) throws IOException;
}
