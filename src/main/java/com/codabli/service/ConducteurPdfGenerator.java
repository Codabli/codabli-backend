package com.codabli.service;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Mise en page du conducteur du spectacle : gabarit HTML/CSS
 * (resources/pdf/conducteur.html) converti en PDF A4 par OpenHTMLtoPDF.
 * <p>
 * PDF balise (PDF/UA) : texte selectionnable et lisible par un lecteur
 * d'ecran. Polices de la charte Codabli embarquees (Lato pour le texte,
 * Baloo 2 pour les titres, licence OFL).
 */
@Component
public class ConducteurPdfGenerator {

    private static final String DOSSIER = "pdf/";

    private final TemplateEngine moteur;

    public ConducteurPdfGenerator() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix(DOSSIER);
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        moteur = new TemplateEngine();
        moteur.setTemplateResolver(resolver);
    }

    public byte[] generer(ConducteurSpectacle conducteur) {
        Context contexte = new Context();
        contexte.setVariable("conducteur", conducteur);
        contexte.setVariable("ligneRideau", ConducteurSpectacleService.LIGNE_RIDEAU);
        String html = moteur.process("conducteur", contexte);

        // OpenHTMLtoPDF attend un document XML bien forme : jsoup le produit a partir du HTML
        org.w3c.dom.Document document = new W3CDom().fromJsoup(Jsoup.parse(html));

        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder()
                    .useFastMode()
                    .usePdfUaAccessibility(true)
                    .withProducer("Codabli")
                    .withW3cDocument(document, null)
                    .toStream(sortie);
            police(builder, "Lato-Regular.ttf", "Lato", 400, FontStyle.NORMAL);
            police(builder, "Lato-Bold.ttf", "Lato", 700, FontStyle.NORMAL);
            police(builder, "Lato-Italic.ttf", "Lato", 400, FontStyle.ITALIC);
            police(builder, "Lato-BoldItalic.ttf", "Lato", 700, FontStyle.ITALIC);
            police(builder, "Baloo2-Variable.ttf", "Baloo 2", 400, FontStyle.NORMAL);
            builder.run();
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Generation du conducteur du spectacle impossible", e);
        }
    }

    private void police(PdfRendererBuilder builder, String fichier, String famille, int graisse, FontStyle style) {
        builder.useFont(() -> getClass().getClassLoader().getResourceAsStream(DOSSIER + "fonts/" + fichier),
                famille, graisse, style, true);
    }
}
