package com.zuzex.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.zuzex.data.algorithm.ArchiveData;
import com.zuzex.data.algorithm.CompanyData;

import java.util.List;

public interface DataService {
    void processData(final CompanyData companyData, final JsonNode node);

    void addArchives(final List<ArchiveData> archiveNameAndPath);

    void setReadArchive(final String archiveName);
}
