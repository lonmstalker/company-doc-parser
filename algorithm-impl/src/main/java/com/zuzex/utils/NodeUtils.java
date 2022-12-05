package com.zuzex.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.zuzex.constants.Identifier;
import com.zuzex.data.algorithm.CompanyData;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@UtilityClass
public class NodeUtils {
    private static final String COMPANY_NAME = "";
    private static final String COMPANY_INFO = ""; // инфо о компании
    private static final boolean LOG_TRACE = log.isTraceEnabled();

    @Nullable
    public CompanyData successParse(final JsonNode data, final String fileName) {
        final var ogrn = getIdentifier(data);
        final var references = new LinkedHashMap<String, List<String>>(); // key - parentName, value - childName
        final var innerCompanies = new HashMap<String, JsonNode>();

        if (nonNull(ogrn)) {
            // если основная всегда имеет огрн и тп
            final var ogrnAsText = ogrn.getValue().asText();

            if (LOG_TRACE) {
                log.trace("Found '{}' in xml: '{}'", ogrn.getKey(), ogrnAsText);
            }

            processInnerNodes(innerCompanies, references, data, ogrnAsText);

            return new CompanyData(innerCompanies, references, ogrnAsText);
        } else {
            log.error("Found xml without identifier: '{}'", fileName);
        }

        return null;
    }

    @Nullable
    public Pair<String, JsonNode> getIdentifier(final JsonNode data) {
        var ogrn = data.get(Identifier.OGRIP.ruName);
        if (nonNull(ogrn)) {
            return Pair.of(Identifier.OGRIP.name(), ogrn);
        } else {
            ogrn = data.get(Identifier.OGRN.ruName);
        }
        if (nonNull(ogrn)) {
            return Pair.of(Identifier.OGRN.name(), ogrn);
        } else {
            // сюда попадем только во вложенных объектах
            if (nonNull(data.get(COMPANY_NAME)) && nonNull(data.get(Identifier.INN.ruName))) {
                return Pair.of(Identifier.INN.name(), data);
            }
        }
        return null;
    }

    public void processInnerNodes(final Map<String, JsonNode> innerCompanies, @NonNull final Map<String, List<String>> references,
                                  @NonNull final JsonNode mainNode, final String parentIdentifier) {
        JsonNode node;
        Map.Entry<String, JsonNode> entry;
        final var fields = mainNode.fields();
        while (fields.hasNext()) {
            entry = fields.next();
            node = entry.getValue();
            if (node.isObject() && !COMPANY_INFO.equals(entry.getKey()) && processChildNode(innerCompanies, references, node, parentIdentifier)) {
                fields.remove();
            }
        }
    }

    // добавляем все связи нод
    public boolean processChildNode(final Map<String, JsonNode> innerCompanies, @NonNull final Map<String, List<String>> references,
                                    @NonNull final JsonNode childNode, final String parentIdentifier) {
        boolean isCompanyObject = false;
        final var identifier = getIdentifier(childNode);
        if (nonNull(identifier)) {
            final var companyId = Objects.equals(identifier.getKey(), Identifier.INN.name())
                    ? childNode.get(Identifier.INN.ruName).asText().concat(childNode.get(COMPANY_NAME).asText())
                    : identifier.getValue().asText();

            var curReferences = references.get(parentIdentifier);
            if (isNull(curReferences)) {
                curReferences = new ArrayList<>();
                curReferences.add(companyId);
                references.put(parentIdentifier, curReferences);
            } else {
                curReferences.add(companyId);
            }

            isCompanyObject = true;
        }
        processInnerNodes(innerCompanies, references, childNode, parentIdentifier);
        return isCompanyObject;
    }
}
