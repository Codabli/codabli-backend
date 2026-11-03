package com.codabli.service;

import com.codabli.dto.ConteContextData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConteContextFormatterTest {

    private final ConteContextFormatter formatter = new ConteContextFormatter();

    @Test
    void formater_echappeLesValeursEtOmetLesChampsVides() {
        ConteContextData context = new ConteContextData(
                "Conte <secret>",
                null,
                List.of("clé & lanterne", " "),
                "France",
                "",
                4,
                12);

        assertEquals("""
                <conte_context><titre>Conte &lt;secret&gt;</titre><pays>France</pays><tranche_age min="4" max="12">4-12 ans</tranche_age><ingredients_secrets><ingredient_secret>clé &amp; lanterne</ingredient_secret></ingredients_secrets></conte_context>""",
                formatter.formater(context));
    }

    @Test
    void ajouterAuPrompt_placeLeContexteAuDebut() {
        String prompt = formatter.ajouterAuPrompt(
                "Consignes système",
                new ConteContextData(null, null, List.of(), null, null, 4, 12));

        assertEquals("<conte_context><tranche_age min=\"4\" max=\"12\">4-12 ans</tranche_age>"
                + "</conte_context>\n\nConsignes système", prompt);
        assertFalse(prompt.contains("<ingredients_secrets>"));
    }
}
