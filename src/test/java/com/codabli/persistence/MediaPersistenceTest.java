package com.codabli.persistence;

import com.codabli.entity.Media;
import com.codabli.entity.enums.MediaStatus;
import com.codabli.repository.MediaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MediaPersistenceTest {

    @Autowired
    private MediaRepository mediaRepository;

    @Test
    @Transactional
    void mediaMetadataShouldPersistUsingLiquibaseSchema() {
        UUID id = UUID.randomUUID();
        Media media = Media.builder()
                .id(id)
                .storageKey(id + ".jpg")
                .originalName("dessin.jpg")
                .contentType("image/jpeg")
                .sizeBytes(128)
                .status(MediaStatus.AVAILABLE)
                .build();

        mediaRepository.save(media);
        Media persisted = mediaRepository.findById(id).orElseThrow();

        assertThat(persisted.getStorageKey()).isEqualTo(id + ".jpg");
        assertThat(persisted.getOriginalName()).isEqualTo("dessin.jpg");
        assertThat(persisted.getContentType()).isEqualTo("image/jpeg");
        assertThat(persisted.getSizeBytes()).isEqualTo(128);
        assertThat(persisted.getStatus()).isEqualTo(MediaStatus.AVAILABLE);
        assertThat(persisted.getCreatedAt()).isNotNull();
    }
}
