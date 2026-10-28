package com.codabli.persistence;

import com.codabli.entity.Produit;
import com.codabli.entity.ConteDanse;
import com.codabli.entity.ConteDanseTraduction;
import com.codabli.entity.Utilisateur;
import com.codabli.entity.enums.AccesConte;
import com.codabli.entity.enums.RoleMessage;
import com.codabli.entity.enums.RoleUtilisateur;
import com.codabli.entity.enums.StatutConte;
import com.codabli.entity.enums.StatutTraduction;
import com.codabli.entity.enums.TypeProduit;
import com.codabli.repository.ProduitRepository;
import com.codabli.repository.MessageAssistantIaRepository;
import com.codabli.service.AssistantIaService;
import com.codabli.service.ConteDanseService;
import com.codabli.service.MessageAssistantIaService;
import com.codabli.dto.AssistantIaRequest;
import com.codabli.exception.BusinessException;
import com.codabli.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Autowired
    private MessageAssistantIaRepository messageRepository;

    @Autowired
    private MessageAssistantIaService messageService;

    @Autowired
    private AssistantIaService assistantService;

    @Autowired
    private ConteDanseService conteService;

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    @Test
    @Transactional
    void hibernate_shouldPersistAnonymizedConversationTurn() {
        ConteFixture fixture = creerConte();

        messageService.ajouterTour(fixture.conteId(), "Contact enfant@example.com", "Quelle est la suite ?",
                fixture.jwt());
        entityManager.flush();
        entityManager.clear();

        var messages = messageRepository.findByConteDanseIdOrderByDateCreationAscIdAsc(fixture.conteId());
        assertThat(messages).hasSize(2);
        assertThat(messages).extracting(message -> message.getRole())
                .containsExactly(RoleMessage.user, RoleMessage.assistant);
        assertThat(messages.getFirst().getContenu()).contains("[EMAIL_1]").doesNotContain("enfant@example.com");
        assertThat(messages).allSatisfy(message -> {
            assertThat(message.getId()).isNotNull();
            assertThat(message.getDateCreation()).isNotNull();
        });
    }

    @Test
    void publication_shouldPurgeHistoryOnCommitButNotOnRollback() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ConteFixture fixture = transaction.execute(status -> {
            ConteFixture created = creerConte();
            messageService.ajouterTour(created.conteId(), "Question", "Reponse", created.jwt());
            return created;
        });
        try {
            transaction.executeWithoutResult(status -> {
                conteService.valider(fixture.conteId(), fixture.jwt());
                status.setRollbackOnly();
            });
            assertThat(messageRepository.findByConteDanseIdOrderByDateCreationAscIdAsc(fixture.conteId()))
                    .hasSize(2);
            assertThat(conteService.getById(fixture.conteId()).statut()).isEqualTo(StatutConte.brouillon);

            conteService.valider(fixture.conteId(), fixture.jwt());

            assertThat(messageRepository.findByConteDanseIdOrderByDateCreationAscIdAsc(fixture.conteId()))
                    .isEmpty();
            assertThat(messageService.listerHistorique(fixture.conteId(), fixture.jwt())).isEmpty();
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> assistantService.preparerConversation(
                            fixture.conteId(), new AssistantIaRequest("Nouvelle question", null), fixture.jwt()));
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONTE_DEJA_PUBLIE);
        } finally {
            supprimerFixture(fixture);
        }
    }

    @Test
    @Transactional
    void catalogue_shouldApplyOptionalFiltersAndReturnOnlyPublishedTranslations() {
        ConteFixture fixture = creerConte();
        ConteDanse conte = entityManager.find(ConteDanse.class, fixture.conteId());
        conte.setStatut(StatutConte.publie);
        conte.setDatePublication(OffsetDateTime.parse("2026-01-02T10:00:00Z"));
        conte.setPays("Liban");
        conte.setThematique("Une grande amitie");
        conte.setAgeMin(4);
        conte.setAgeMax(12);
        entityManager.persist(ConteDanseTraduction.builder().conte(conte).langue("en")
                .statut(StatutTraduction.publiee).texte("Published text").build());
        entityManager.persist(ConteDanseTraduction.builder().conte(conte).langue("ar")
                .statut(StatutTraduction.a_traduire).texte("Brouillon prive").build());
        ConteDanse brouillon = entityManager.find(ConteDanse.class, creerConte().conteId());
        brouillon.setPays("Liban");
        brouillon.setThematique("amitie");
        ConteDanse ancien = entityManager.find(ConteDanse.class, creerConte().conteId());
        ancien.setStatut(StatutConte.publie);
        ancien.setDatePublication(OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        ancien.setPays("France");
        entityManager.flush();
        entityManager.clear();

        var result = conteService.filtrerPublies(" FR ", " liban ", " AMITIE ",
                AccesConte.gratuit, 4, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(fixture.conteId());
        assertThat(result.getContent().getFirst().traductions())
                .singleElement().satisfies(traduction -> {
                    assertThat(traduction.langue()).isEqualTo("en");
                    assertThat(traduction.texte()).isEqualTo("Published text");
                });
        assertThat(conteService.filtrerPublies(null, "Liban", null, null, 12, PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(1);
        assertThat(conteService.filtrerPublies(null, "Liban", null, null, 13, PageRequest.of(0, 10)))
                .isEmpty();
        var sansFiltres = conteService.filtrerPublies("", " ", "", null, null, PageRequest.of(0, 1));
        assertThat(sansFiltres.getTotalElements()).isEqualTo(2);
        assertThat(sansFiltres.getContent().getFirst().id()).isEqualTo(fixture.conteId());
        assertThat(conteService.listerPublies(PageRequest.of(0, 10)).getContent().getFirst().traductions())
                .singleElement().satisfies(traduction -> assertThat(traduction.langue()).isEqualTo("en"));
    }

    @Test
    void concurrentTurns_shouldStayOrderedAndPaired() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ConteFixture fixture = transaction.execute(status -> creerConte());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> turns = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                int numero = i;
                turns.add(executor.submit(() -> {
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Les tours concurrents n'ont pas demarre");
                    }
                    return messageService.ajouterTour(fixture.conteId(),
                            "Question " + numero, "Reponse " + numero, fixture.jwt());
                }));
            }
            start.countDown();
            for (Future<?> turn : turns) {
                turn.get(10, TimeUnit.SECONDS);
            }
            var messages = messageRepository.findByConteDanseIdOrderByDateCreationAscIdAsc(fixture.conteId());
            assertThat(messages).hasSize(16);
            for (int i = 0; i < messages.size(); i += 2) {
                var question = messages.get(i);
                var reponse = messages.get(i + 1);
                assertThat(question.getRole()).isEqualTo(RoleMessage.user);
                assertThat(reponse.getRole()).isEqualTo(RoleMessage.assistant);
                assertThat(reponse.getContenu()).isEqualTo(question.getContenu().replace("Question", "Reponse"));
                assertThat(reponse.getDateCreation()).isAfter(question.getDateCreation());
                if (i > 0) {
                    assertThat(question.getDateCreation()).isAfter(messages.get(i - 1).getDateCreation());
                }
            }
        } finally {
            supprimerFixture(fixture);
        }
    }

    @Test
    void publication_shouldWaitForInFlightAppendAndPurgeIt() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ConteFixture fixture = transaction.execute(status -> creerConte());
        CountDownLatch appendReady = new CountDownLatch(1);
        CountDownLatch releaseAppend = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> append = executor.submit(() -> transaction.executeWithoutResult(status -> {
                messageService.ajouterTour(fixture.conteId(), "Question", "Reponse", fixture.jwt());
                entityManager.flush();
                appendReady.countDown();
                try {
                    if (!releaseAppend.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Le tour n'a pas ete libere");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }));
            assertThat(appendReady.await(5, TimeUnit.SECONDS)).isTrue();
            CountDownLatch publicationStarted = new CountDownLatch(1);
            Future<?> publication = executor.submit(() -> {
                publicationStarted.countDown();
                return conteService.valider(fixture.conteId(), fixture.jwt());
            });
            try {
                assertThat(publicationStarted.await(5, TimeUnit.SECONDS)).isTrue();
                assertThrows(TimeoutException.class, () -> publication.get(200, TimeUnit.MILLISECONDS));
            } finally {
                releaseAppend.countDown();
            }
            append.get(10, TimeUnit.SECONDS);
            publication.get(10, TimeUnit.SECONDS);
            assertThat(messageRepository.findByConteDanseIdOrderByDateCreationAscIdAsc(fixture.conteId()))
                    .isEmpty();
        } finally {
            releaseAppend.countDown();
            supprimerFixture(fixture);
        }
    }

    @Test
    void append_shouldRejectContePublishedWhileWaitingForLock() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ConteFixture fixture = transaction.execute(status -> creerConte());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> append = transaction.execute(status -> {
                conteService.valider(fixture.conteId(), fixture.jwt());
                entityManager.flush();
                Future<?> pending = executor.submit(() -> messageService.ajouterTour(
                        fixture.conteId(), "Question", "Reponse", fixture.jwt()));
                assertThrows(TimeoutException.class, () -> pending.get(200, TimeUnit.MILLISECONDS));
                return pending;
            });

            ExecutionException exception = assertThrows(ExecutionException.class,
                    () -> append.get(10, TimeUnit.SECONDS));
            assertThat(exception.getCause()).isInstanceOfSatisfying(BusinessException.class,
                    cause -> assertThat(cause.getErrorCode()).isEqualTo(ErrorCode.CONTE_DEJA_PUBLIE));
            assertThat(messageRepository.findByConteDanseIdOrderByDateCreationAscIdAsc(fixture.conteId()))
                    .isEmpty();
        } finally {
            supprimerFixture(fixture);
        }
    }

    private void supprimerFixture(ConteFixture fixture) {
        jdbcTemplate.update("DELETE FROM contes_danses WHERE id = ?", fixture.conteId());
        jdbcTemplate.update("DELETE FROM utilisateurs WHERE id = ?", fixture.auteurId());
    }

    private ConteFixture creerConte() {
        String keycloakId = UUID.randomUUID().toString();
        Utilisateur auteur = Utilisateur.builder().nom("Auteur").prenom("Test")
                .role(RoleUtilisateur.enseignant).keycloakId(keycloakId).build();
        entityManager.persist(auteur);
        ConteDanse conte = ConteDanse.builder().titre("Conte de verification").createur(auteur).build();
        entityManager.persist(conte);
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject(keycloakId)
                .claim("realm_access", Map.of("roles", List.of("admin"))).build();
        return new ConteFixture(conte.getId(), auteur.getId(), jwt);
    }

    private record ConteFixture(UUID conteId, UUID auteurId, Jwt jwt) {
    }
}
