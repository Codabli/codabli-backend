package com.codabli.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;

/**
 * Lecture des colonnes JSON par Hibernate (hibernate.type.json_format_mapper) :
 * un champ stocke qui n'existe plus dans le modele est ignore. Sans cela, une
 * evolution du contenu du parcours (dto.parcours) rendrait illisibles les
 * contes deja enregistres. L'ecriture reste stricte (JacksonConfig).
 */
public class LectureJsonTolerante implements FormatMapper {

    private final FormatMapper delegue = new JacksonJsonFormatMapper(new ObjectMapper()
            .findAndRegisterModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

    @Override
    public <T> T fromString(CharSequence chaine, JavaType<T> type, WrapperOptions options) {
        return delegue.fromString(chaine, type, options);
    }

    @Override
    public <T> String toString(T valeur, JavaType<T> type, WrapperOptions options) {
        return delegue.toString(valeur, type, options);
    }
}
