package com.codabli.persistence;

import com.codabli.entity.Media;
import com.codabli.entity.RessourcePedagogique;
import com.codabli.entity.enums.MediaStatus;
import com.codabli.entity.enums.TypeRessource;
import com.codabli.repository.MediaRepository;
import com.codabli.repository.RessourcePedagogiqueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MediaRessourcePersistenceTest {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private RessourcePedagogiqueRepository resourceRepository;

    @Test
    @Transactional
    void mediaRelationPersistsWithoutChangingLegacyUrl() {
        UUID mediaId = UUID.randomUUID();
        Media media = mediaRepository.save(Media.builder()
                .id(mediaId)
                .storageKey(mediaId + ".pdf")
                .originalName("guide.pdf")
                .contentType("application/pdf")
                .sizeBytes(512)
                .status(MediaStatus.AVAILABLE)
                .build());
        String legacyUrl = "https://legacy.example/guide.pdf";

        RessourcePedagogique resource = resourceRepository.save(RessourcePedagogique.builder()
                .titre("Guide pédagogique")
                .description("Description")
                .type(TypeRessource.fiche)
                .fichierUrl(legacyUrl)
                .fichierMedia(media)
                .actif(true)
                .build());

        RessourcePedagogique persisted = resourceRepository.findById(resource.getId()).orElseThrow();

        assertThat(persisted.getFichierMedia().getId()).isEqualTo(mediaId);
        assertThat(persisted.getFichierUrl()).isEqualTo(legacyUrl);
    }
}
