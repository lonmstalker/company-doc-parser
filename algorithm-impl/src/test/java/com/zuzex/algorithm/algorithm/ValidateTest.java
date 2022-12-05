package com.zuzex.algorithm.algorithm;

import com.zuzex.algorithm.impl.ParallelAlgorithm;
import com.zuzex.algorithm.impl.SequenceAlgorithm;
import com.zuzex.config.AppProperties;
import com.zuzex.config.ExecutorConfig;
import com.zuzex.parse.DirectoryParser;
import com.zuzex.reader.impl.JacksonXmlReader;
import com.zuzex.reader.impl.MetaCsvReaderImpl;
import com.zuzex.service.impl.DataServiceImpl;
import com.zuzex.tables.records.CompaniesRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jooq.JooqTest;
import org.springframework.context.annotation.Import;

import java.io.File;
import java.io.IOException;

import static com.zuzex.algorithm.utils.BenchUtils.*;
import static com.zuzex.constants.Constants.CURRENT_PATH;
import static com.zuzex.constants.Constants.PATH_DELIMITER;
import static com.zuzex.tables.Companies.COMPANIES;

@Slf4j
@Order(1)
// @Sql(scripts = {"/db/init.sql"})
@Import({
        ParallelAlgorithm.class,
        SequenceAlgorithm.class,
        DirectoryParser.class,
        DataServiceImpl.class,
        JacksonXmlReader.class,
        MetaCsvReaderImpl.class,
        ExecutorConfig.class
})
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@JooqTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class) // reference liquibase error
// @AutoConfigureEmbeddedDatabase(provider = ZONKY, type = POSTGRES, refresh = NEVER)
public class ValidateTest {

    @Autowired
    DSLContext dslContext;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ParallelAlgorithm parallelAlgorithm;

    @Autowired
    private SequenceAlgorithm sequenceAlgorithm;

    @Autowired
    private DirectoryParser directoryParser;

    @BeforeEach
    void prepare() throws IOException {
        FileUtils.deleteDirectory(
                new File(CURRENT_PATH + PATH_DELIMITER + appProperties.getArchiveFolder() + PATH_DELIMITER + "unzip"));
    }

    @AfterEach
    void reset() {
        log.info("Saved into db: '{}'", dslContext.fetchCount(COMPANIES));
//    dslContext.deleteFrom(COMPANIES).execute();
    }

    @Test
    @Order(0)
    public void validate_db() {
        dslContext
                .insertInto(COMPANIES.as("company"))
                .values("name", JSONB.jsonbOrNull("{}"), JSONB.jsonbOrNull("{}"), JSONB.jsonbOrNull("{}"))
                .execute();
        log.info(
                "select: {}",
                dslContext.select().from(COMPANIES.as("company")).fetchInto(CompaniesRecord.class));
        dslContext.deleteFrom(COMPANIES).where(COMPANIES.COMPANY_NAME.equal("name")).execute();
    }

    @Test
    @Order(1)
    public void should_validate_data() {
        validArchives(appProperties.getArchiveFolder());
    }

    /**
     * Тесты ниже запускаются отдельно друг от друга, данные из бд не чистятся для ручной проверки в бд
     */
    @Test
    @Order(2)
    public void should_validate_parallel_logic() {
        Assertions.assertDoesNotThrow(
                () -> this.parallelAlgorithm.parse(directoryParser.findArchives()));
    }

    @Test
    @Order(3)
    public void should_validate_sequence_logic() {
        Assertions.assertDoesNotThrow(
                () -> this.sequenceAlgorithm.parse(directoryParser.findArchives()));
    }
}
