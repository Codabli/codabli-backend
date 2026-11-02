package com.codabli.config;

import com.codabli.dto.parcours.ContenuEtapeParcours;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Spring ignore par defaut les champs inconnus d'un corps JSON. Pour le
 * contenu des etapes du parcours (dto.parcours), stocke tel quel en JSON, un
 * champ inconnu est refuse : rien n'est enregistre qui ne soit dans le modele.
 * Le reste de l'API garde le comportement par defaut.
 */
@Configuration
public class JacksonConfig {

    private static final String PACKAGE_PARCOURS = ContenuEtapeParcours.class.getPackageName();

    @Bean
    JsonMapperBuilderCustomizer refuserChampsInconnusDuParcours() {
        return builder -> builder.addHandler(new DeserializationProblemHandler() {
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p,
                                                 ValueDeserializer<?> deserializer, Object beanOrClass,
                                                 String propertyName) {
                Class<?> type = beanOrClass instanceof Class<?> classe ? classe : beanOrClass.getClass();
                if (type.getPackageName().equals(PACKAGE_PARCOURS)) {
                    throw UnrecognizedPropertyException.from(p, beanOrClass, propertyName,
                            deserializer.getKnownPropertyNames());
                }
                return false;
            }
        });
    }
}
