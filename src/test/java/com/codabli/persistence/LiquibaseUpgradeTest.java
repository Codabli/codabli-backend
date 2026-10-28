package com.codabli.persistence;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiquibaseUpgradeTest {

    private static final String MASTER = "classpath:db/changelog/db.changelog-master.xml";

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private String schema;

    @BeforeEach
    void setUp() {
        String url = System.getProperty("migration.test.url",
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        dataSource = new DriverManagerDataSource(url,
                System.getProperty("migration.test.username", "sa"),
                System.getProperty("migration.test.password", ""));
        schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        if (url.startsWith("jdbc:h2:")) {
            schema = schema.toUpperCase(Locale.ROOT);
        }
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE SCHEMA " + schema);
        dataSource.setSchema(schema);
    }

    @AfterEach
    void tearDown() {
        jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
    }

    @Test
    void freshInstall_shouldCreateMessageConstraintsAndIndexAndBeIdempotent() throws Exception {
        migrate(MASTER);

        assertMessageSchema();
        assertMessageLifecycle();
        var history = history();

        migrate(MASTER);

        assertThat(history()).isEqualTo(history);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM databasechangeloglock WHERE locked = TRUE", Integer.class)).isZero();
    }

    @Test
    void upgradeFrom001_shouldPreserveRowsAndOriginalChecksums() throws Exception {
        migrate("classpath:db/changelog/001-schema.xml");
        UUID conteId = insertConte();
        UUID produitId = UUID.randomUUID();
        int changesetSize = 4;
        jdbc.update("""
                INSERT INTO produits (id, nom, prix, type, stock, actif, date_creation, date_mise_a_jour)
                VALUES (?, 'Produit existant', 12.50, 'produit_physique', 3, TRUE,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, produitId);
        var originalHistory = history();

        migrate(MASTER);

        assertThat(history()).hasSize(originalHistory.size() + changesetSize);
        assertThat(history().subList(0, originalHistory.size())).isEqualTo(originalHistory);
        assertThat(jdbc.queryForObject("SELECT titre FROM contes_danses WHERE id = ?",
                String.class, conteId)).isEqualTo("Conte existant");
        assertThat(jdbc.queryForObject("SELECT stock FROM produits WHERE id = ?",
                Integer.class, produitId)).isEqualTo(3);
        UUID messageId = insertMessage(conteId);
        var upgradedHistory = history();

        migrate(MASTER);

        assertThat(history()).isEqualTo(upgradedHistory);
        assertThat(jdbc.queryForObject("SELECT contenu FROM messages_assistant_ia WHERE id = ?",
                String.class, messageId)).isEqualTo("Question conservee");
        assertMessageSchema();
    }

    private void assertMessageSchema() throws Exception {
        assertThat(jdbc.queryForList("""
                SELECT LOWER(column_name) FROM information_schema.columns
                WHERE LOWER(table_schema) = LOWER(?) AND LOWER(table_name) = 'messages_assistant_ia'
                  AND is_nullable = 'NO'
                ORDER BY ordinal_position
                """, String.class, schema))
                .containsExactly("id", "conte_danse_id", "role", "contenu", "date_creation");
        assertThat(jdbc.queryForObject("""
                SELECT character_maximum_length FROM information_schema.columns
                WHERE LOWER(table_schema) = LOWER(?) AND LOWER(table_name) = 'messages_assistant_ia'
                  AND LOWER(column_name) = 'role'
                """, Integer.class, schema)).isEqualTo(16);
        try (var connection = dataSource.getConnection()) {
            var metadata = connection.getMetaData();
            String table = schema.equals(schema.toUpperCase(Locale.ROOT))
                    ? "MESSAGES_ASSISTANT_IA" : "messages_assistant_ia";
            try (var primaryKeys = metadata.getPrimaryKeys(null, schema, table)) {
                assertThat(primaryKeys.next()).isTrue();
                assertThat(primaryKeys.getString("COLUMN_NAME")).isEqualToIgnoringCase("id");
                assertThat(primaryKeys.next()).isFalse();
            }
            List<String> indexedColumns = new ArrayList<>();
            try (var indexes = metadata.getIndexInfo(null, schema, table, false, false)) {
                while (indexes.next()) {
                    if ("idx_messages_assistant_ia_conte_date".equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                        indexedColumns.add(indexes.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                    }
                }
            }
            assertThat(indexedColumns).containsExactly("conte_danse_id", "date_creation");
            try (var keys = metadata.getImportedKeys(null, schema, table)) {
                assertThat(keys.next()).isTrue();
                assertThat(keys.getString("FKCOLUMN_NAME")).isEqualToIgnoringCase("conte_danse_id");
                assertThat(keys.getString("PKTABLE_NAME")).isEqualToIgnoringCase("contes_danses");
                assertThat(keys.getString("PKCOLUMN_NAME")).isEqualToIgnoringCase("id");
                assertThat(keys.getInt("DELETE_RULE")).isEqualTo(DatabaseMetaData.importedKeyCascade);
                assertThat(keys.next()).isFalse();
            }
        }
    }

    private void assertMessageLifecycle() {
        UUID conteId = insertConte();
        UUID autreConteId = insertConte();
        insertMessage(conteId);
        UUID autreMessageId = insertMessage(autreConteId);
        assertThatThrownBy(() -> insertMessage(UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbc.update("DELETE FROM contes_danses WHERE id = ?", conteId);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM messages_assistant_ia WHERE conte_danse_id = ?",
                Integer.class, conteId)).isZero();
        assertThat(jdbc.queryForObject("SELECT contenu FROM messages_assistant_ia WHERE id = ?",
                String.class, autreMessageId)).isEqualTo("Question conservee");
    }

    private UUID insertConte() {
        UUID auteurId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO utilisateurs (id, nom, prenom, role, statut, date_creation, date_mise_a_jour)
                VALUES (?, 'Auteur', 'Existant', 'enseignant', 'actif', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, auteurId);
        UUID conteId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO contes_danses (id, createur_id, titre, langue_originale, statut, acces, date_creation)
                VALUES (?, ?, 'Conte existant', 'fr', 'brouillon', 'gratuit', CURRENT_TIMESTAMP)
                """, conteId, auteurId);
        return conteId;
    }

    private UUID insertMessage(UUID conteId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO messages_assistant_ia (id, conte_danse_id, role, contenu, date_creation)
                VALUES (?, ?, 'user', 'Question conservee', CURRENT_TIMESTAMP)
                """, id, conteId);
        return id;
    }

    private List<Map<String, Object>> history() {
        return jdbc.queryForList("""
                SELECT id, author, filename, md5sum, dateexecuted, orderexecuted, exectype
                FROM databasechangelog ORDER BY orderexecuted
                """);
    }

    private void migrate(String changelog) throws Exception {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setDefaultSchema(schema);
        liquibase.setChangeLog(changelog);
        liquibase.afterPropertiesSet();
    }
}
