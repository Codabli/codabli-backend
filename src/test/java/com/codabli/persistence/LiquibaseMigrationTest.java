package com.codabli.persistence;

import com.codabli.entity.Produit;
import com.codabli.entity.enums.TypeProduit;
import com.codabli.repository.ProduitRepository;
import jakarta.persistence.EntityManager;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LiquibaseMigrationTest {

    @Autowired
    private SpringLiquibase liquibase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private EntityManager entityManager;

    @Value("${spring.liquibase.default-schema}")
    private String schema;

    @Test
    void migrations_shouldUseDedicatedSchemaAndNotRunTwice() throws Exception {
        assertThat(schema).isNotEqualToIgnoringCase("public");
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getSchema()).isEqualToIgnoringCase(schema);
        }
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE LOWER(table_schema) = ?
                  AND LOWER(table_name) IN ('produits', 'databasechangelog', 'databasechangeloglock')
                """, Integer.class, schema)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE LOWER(table_schema) = 'public'
                  AND LOWER(table_name) IN ('produits', 'databasechangelog', 'databasechangeloglock')
                """, Integer.class)).isZero();

        var executed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM databasechangelog", Integer.class);
        assertThat(executed).isPositive();
        var history = jdbcTemplate.queryForList("""
                SELECT id, author, filename, md5sum, dateexecuted, orderexecuted, exectype
                FROM databasechangelog ORDER BY orderexecuted
                """);

        liquibase.afterPropertiesSet();

        assertThat(jdbcTemplate.queryForList("""
                SELECT id, author, filename, md5sum, dateexecuted, orderexecuted, exectype
                FROM databasechangelog ORDER BY orderexecuted
                """)).isEqualTo(history);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangeloglock WHERE locked = TRUE", Integer.class)).isZero();
    }

    @Test
    @Transactional
    void hibernate_shouldReadAndWriteLiquibaseTables() {
        var produit = produitRepository.saveAndFlush(Produit.builder()
                .nom("Produit de verification Liquibase")
                .prix(new BigDecimal("12.50"))
                .type(TypeProduit.produit_physique)
                .stock(1)
                .build());
        entityManager.clear();

        var reloaded = produitRepository.findById(produit.getId()).orElseThrow();

        assertThat(reloaded.getNom()).isEqualTo(produit.getNom());
        assertThat(reloaded.getPrix()).isEqualByComparingTo("12.50");
        assertThat(reloaded.getDateCreation()).isNotNull();
    }
}
