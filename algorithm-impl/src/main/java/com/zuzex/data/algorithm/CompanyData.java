package com.zuzex.data.algorithm;

import java.util.List;
import java.util.Map;

public record CompanyData(
        Map<String, com.fasterxml.jackson.databind.JsonNode> innerCompanies, Map<String, List<String>> references,
        String mainCompanyName
) {
}
