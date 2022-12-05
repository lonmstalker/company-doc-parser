package com.zuzex.data.parse;

import java.time.LocalDate;

public record ArchiveMetadata(String hash, String inn, String ogrn, LocalDate actualDate) {
}
