package com.zuzex.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.zuzex.data.algorithm.ArchiveData;
import com.zuzex.data.algorithm.CompanyData;
import com.zuzex.service.DataService;
import com.zuzex.tables.records.ArchivesRecord;
import com.zuzex.tables.records.CompaniesRecord;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.nio.serial.Json;
import org.apache.commons.collections4.MapUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.zuzex.tables.Archives.ARCHIVES;
import static com.zuzex.tables.Companies.COMPANIES;

@Service
@RequiredArgsConstructor
public class DataServiceImpl implements DataService {
    private final DSLContext context;

    @Override
    @SneakyThrows
    @Transactional
    public void processData(final CompanyData companyData, final JsonNode node) {
        if (MapUtils.isEmpty(companyData.references())) {
            this.context
                    .insertInto(
                            COMPANIES,
                            COMPANIES.COMPANY_NAME,
                            COMPANIES.DATA
                    )
                    .values(
                            companyData.mainCompanyName(),
                            this.toJsonOrNull(node)
                    )
                    .onDuplicateKeyUpdate()
                    .setAllToExcluded()
                    .execute();
        } else {
            this.context
                    .batchInsert(this.getCompaniesRecord(companyData, node))
                    .execute();
        }
    }

    @Override
    public void addArchives(final List<ArchiveData> archiveNameAndPath) {
        this.context
                .batchInsert(
                        archiveNameAndPath
                                .stream()
                                .map(archiveData ->
                                        new ArchivesRecord()
                                                .value1(archiveData.archiveName())
                                                .value2(archiveData.archivePathStr())
                                                .value3(archiveData.archiveDate())
                                )
                                .collect(Collectors.toList())
                )
                .execute();
    }

    @Override
    public void setReadArchive(final String archiveName) {
        this.context
                .update(ARCHIVES)
                .set(ARCHIVES.IS_PARSED, true)
                .where(ARCHIVES.ARCHIVE_NAME.eq(archiveName))
                .execute();
    }

    private Collection<CompaniesRecord> getCompaniesRecord(final CompanyData companyData, final JsonNode node) throws IOException {
        final var companiesRecords = new ArrayList<CompaniesRecord>();
        companiesRecords.add(
                new CompaniesRecord(
                        companyData.mainCompanyName(),
                        this.toJsonOrNull(node),
                        this.toJsonOrNull(companyData.references())
                )
        );
        for (final var innerCompany : companyData.innerCompanies().entrySet()) {
            final var curReferences = companyData.references().get(innerCompany.getKey());
            companiesRecords.add(
                    new CompaniesRecord()
                            .value1(innerCompany.getKey())
                            .value2(this.toJsonOrNull(innerCompany.getValue()))
                            .value3(this.toJsonOrNull(curReferences))
            );
        }
        return companiesRecords;
    }

    @Nullable
    private JSONB toJsonOrNull(@Nullable Object obj) throws IOException {
        return obj == null ? null : JSONB.jsonb(Json.toJson(obj));
    }
}
