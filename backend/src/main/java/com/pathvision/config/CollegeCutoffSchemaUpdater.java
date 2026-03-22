package com.pathvision.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.List;
import java.util.Map;

@Component
public class CollegeCutoffSchemaUpdater implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CollegeCutoffSchemaUpdater.class);

    private final JdbcTemplate jdbcTemplate;

    public CollegeCutoffSchemaUpdater(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            alignCollegeCutoffSchema();
        } catch (Exception ex) {
            log.warn("College cutoff schema alignment skipped: {}", ex.getMessage());
        }
    }

    private void alignCollegeCutoffSchema() {
        addColumnIfMissing("branch", "varchar(255) null");
        addColumnIfMissing("branch_code", "varchar(255) null");
        addColumnIfMissing("admission_year", "int null");

        int currentYear = Year.now().getValue();
        jdbcTemplate.update("update college_cutoffs set branch = coalesce(nullif(trim(branch), ''), 'General')");
        jdbcTemplate.update("update college_cutoffs set branch_code = coalesce(nullif(trim(branch_code), ''), 'GEN')");
        jdbcTemplate.update("update college_cutoffs set admission_year = coalesce(admission_year, ?)", currentYear);

        dropLegacyUniqueIndexes();
        ensureBranchWiseUniqueIndex();
    }

    private void addColumnIfMissing(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = 'college_cutoffs'
                  and column_name = ?
                """,
                Integer.class,
                columnName
        );

        if (count != null && count == 0) {
            jdbcTemplate.execute("alter table college_cutoffs add column " + columnName + " " + definition);
        }
    }

    private void dropLegacyUniqueIndexes() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                select index_name, seq_in_index, column_name
                from information_schema.statistics
                where table_schema = database()
                  and table_name = 'college_cutoffs'
                  and non_unique = 0
                order by index_name, seq_in_index
                """
        );

        String currentIndex = null;
        StringBuilder columns = new StringBuilder();
        for (Map<String, Object> row : rows) {
            String indexName = String.valueOf(row.get("index_name"));
            String columnName = String.valueOf(row.get("column_name"));
            if (!indexName.equals(currentIndex)) {
                if (isLegacyCollegeCommunityIndex(currentIndex, columns.toString())) {
                    jdbcTemplate.execute("alter table college_cutoffs drop index " + currentIndex);
                    log.info("Dropped legacy college_cutoffs index {}", currentIndex);
                }
                currentIndex = indexName;
                columns = new StringBuilder();
            }
            if (!columns.isEmpty()) {
                columns.append(",");
            }
            columns.append(columnName.toLowerCase());
        }

        if (isLegacyCollegeCommunityIndex(currentIndex, columns.toString())) {
            jdbcTemplate.execute("alter table college_cutoffs drop index " + currentIndex);
            log.info("Dropped legacy college_cutoffs index {}", currentIndex);
        }
    }

    private boolean isLegacyCollegeCommunityIndex(String indexName, String columns) {
        if (indexName == null || "PRIMARY".equalsIgnoreCase(indexName)) {
            return false;
        }
        return "college_id,community".equals(columns);
    }

    private void ensureBranchWiseUniqueIndex() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                select index_name, seq_in_index, column_name
                from information_schema.statistics
                where table_schema = database()
                  and table_name = 'college_cutoffs'
                  and non_unique = 0
                order by index_name, seq_in_index
                """
        );

        String currentIndex = null;
        StringBuilder columns = new StringBuilder();
        boolean exists = false;
        for (Map<String, Object> row : rows) {
            String indexName = String.valueOf(row.get("index_name"));
            String columnName = String.valueOf(row.get("column_name"));
            if (!indexName.equals(currentIndex)) {
                if ("college_id,branch_code,community,admission_year".equals(columns.toString())) {
                    exists = true;
                    break;
                }
                currentIndex = indexName;
                columns = new StringBuilder();
            }
            if (!columns.isEmpty()) {
                columns.append(",");
            }
            columns.append(columnName.toLowerCase());
        }

        if ("college_id,branch_code,community,admission_year".equals(columns.toString())) {
            exists = true;
        }

        if (!exists) {
            jdbcTemplate.execute(
                    "alter table college_cutoffs add unique index uk_college_cutoffs_branch_community_year " +
                            "(college_id, branch_code, community, admission_year)"
            );
        }
    }
}
