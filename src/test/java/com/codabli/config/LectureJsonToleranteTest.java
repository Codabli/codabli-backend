package com.codabli.config;

import com.codabli.dto.parcours.CarteUnivers;
import com.codabli.dto.parcours.UniversParcours;
import org.hibernate.type.descriptor.java.JavaType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LectureJsonToleranteTest {

    private final LectureJsonTolerante mapper = new LectureJsonTolerante();

    @Test
    @SuppressWarnings("unchecked")
    void fromString_champQuiNExistePlus_shouldBeIgnored() {
        JavaType<UniversParcours> type = mock(JavaType.class);
        when(type.getJavaType()).thenReturn(UniversParcours.class);

        UniversParcours univers = mapper.fromString("""
                {"places": [{"id": "p1", "name": "La forêt", "description": "", "image": null}],
                 "characters": [], "periods": [], "objects": []}""", type, null);

        assertEquals(new UniversParcours(List.of(new CarteUnivers("p1", "La forêt", "", null)),
                List.of(), List.of(), List.of()), univers);
    }
}
