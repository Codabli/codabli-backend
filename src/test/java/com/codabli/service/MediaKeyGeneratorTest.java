package com.codabli.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaKeyGeneratorTest {

    private final MediaKeyGenerator generator = new MediaKeyGenerator();

    @Test
    void generateUsesOnlyMediaIdAndNormalizedExtension() {
        UUID id = UUID.randomUUID();

        assertEquals(id + ".jpg", generator.generate(id, "JPG"));
    }

    @Test
    void generateRejectsPathAndSpecialCharactersInExtension() {
        assertThrows(IllegalArgumentException.class,
                () -> generator.generate(UUID.randomUUID(), "../jpg"));
    }
}
