package com.zuzex.reader.impl;

import com.google.common.base.Splitter;
import com.zuzex.data.parse.ArchiveMetadata;
import com.zuzex.reader.MetaCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MetaCsvReaderImpl implements MetaCsvReader {
    private static final String INN_FIELD = "";
    private static final String HASH_FIELD = "";
    private static final String OGRN_FIELD = "";
    private static final String DATE_FIELD = "";
    private static final boolean LOG_TRACE = log.isTraceEnabled();
    private static final Splitter SPACE_SPLITTER = Splitter.on(" ");

    @Override
    public Map<String, ArchiveMetadata> readDate(final Path filePath) throws IOException {
        try (final var reader = NamedCsvReader.builder().fieldSeparator(';').build(filePath, StandardCharsets.UTF_8)) {
            return reader
                    .stream()
                    .map(row -> new ArchiveMetadata(
                            row.getField(HASH_FIELD),
                            row.getField(INN_FIELD),
                            row.getField(OGRN_FIELD),
                            LocalDate.parse(SPACE_SPLITTER.splitToList(row.getField(DATE_FIELD)).get(0))
                    ))
                    .collect(Collectors.toMap(ArchiveMetadata::hash, e -> e));
        } finally {
            final var deleted = Files.deleteIfExists(filePath);
            if (LOG_TRACE) {
                log.trace("Meta file deleted '{}' \n", deleted);
            }
        }
    }
}
