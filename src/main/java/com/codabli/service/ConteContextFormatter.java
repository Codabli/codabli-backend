package com.codabli.service;

import com.codabli.dto.ConteContextData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConteContextFormatter {

    public String ajouterAuPrompt(String systemPrompt, ConteContextData context) {
        return formater(context) + "\n\n" + systemPrompt;
    }

    public String formater(ConteContextData context) {
        StringBuilder xml = new StringBuilder("<conte_context>");
        ajouterBalise(xml, "titre", context.titre());
        ajouterBalise(xml, "thematique", context.thematique());
        ajouterBalise(xml, "pays", context.pays());
        ajouterBalise(xml, "culture", context.culture());
        ajouterTrancheAge(xml, context.ageMin(), context.ageMax());
        ajouterIngredientsSecrets(xml, context.ingredientsSecrets());
        return xml.append("</conte_context>").toString();
    }

    private static void ajouterBalise(StringBuilder xml, String nom, String valeur) {
        if (valeur != null && !valeur.isBlank()) {
            xml.append('<').append(nom).append('>')
                    .append(echapperXml(valeur.strip()))
                    .append("</").append(nom).append('>');
        }
    }

    private static void ajouterTrancheAge(StringBuilder xml, int ageMin, int ageMax) {
        xml.append("<tranche_age min=\"").append(ageMin)
                .append("\" max=\"").append(ageMax).append("\">")
                .append(ageMin).append('-').append(ageMax).append(" ans")
                .append("</tranche_age>");
    }

    private static void ajouterIngredientsSecrets(StringBuilder xml, List<String> ingredients) {
        if (ingredients.isEmpty()) {
            return;
        }
        xml.append("<ingredients_secrets>");
        ingredients.forEach(ingredient ->
                xml.append("<ingredient_secret>")
                        .append(echapperXml(ingredient))
                        .append("</ingredient_secret>"));
        xml.append("</ingredients_secrets>");
    }

    private static String echapperXml(String valeur) {
        return valeur.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
