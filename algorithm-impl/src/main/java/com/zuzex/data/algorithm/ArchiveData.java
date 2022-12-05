package com.zuzex.data.algorithm;

import java.nio.file.Path;
import java.time.LocalDate;

public record ArchiveData(Path archivePath, LocalDate archiveDate,
                          String archiveName, String archivePathStr) implements Comparable<ArchiveData> {

    public ArchiveData(Path archivePath, LocalDate archiveDate) {
        this(archivePath, archiveDate, archivePath.getFileName().toString(), archivePath.toString());
    }

    @Override
    public int compareTo(final ArchiveData data) {
        return this.archiveDate.compareTo(data.archiveDate);
    }
}
