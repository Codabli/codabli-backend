package com.codabli.service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class PromptAnonymizerService {

    private record AnonymizationRule(String name, Pattern pattern, boolean enabled) { }

    private static final List<AnonymizationRule> DEFAULT_RULES = List.of(
            new AnonymizationRule(
                    "URL",
                    Pattern.compile("https?://(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&/=]*)"),
                    true),
            new AnonymizationRule(
                    "EMAIL",
                    Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
                    true),
            new AnonymizationRule(
                    "TELEPHONE",
                    Pattern.compile("(?:(?:\\+|00)33|0)\\s*[1-9](?:[\\s.-]*\\d{2}){4}"),
                    true),
            new AnonymizationRule(
                    "IP_ADDRESS",
                    Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"),
                    true),
            new AnonymizationRule(
                    "PERSONNE",
                    Pattern.compile("\\b(?:M\\.|Mme|Mlle|Dr|Pr|Monsieur|Madame)\\s+[A-ZÀ-ÖØ-ß][a-zà-öø-ÿ]+(?:\\s+[A-ZÀ-ÖØ-ß][a-zà-öø-ÿ]+)?\\b"),
                    false));

    public String anonymize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String anonymizedText = text;
        for (AnonymizationRule rule : DEFAULT_RULES) {
            if (rule.enabled()) {
                anonymizedText = anonymizeMatches(anonymizedText, rule);
            }
        }
        return anonymizedText;
    }

    private String anonymizeMatches(String text, AnonymizationRule rule) {
        Matcher matcher = rule.pattern().matcher(text);
        Map<String, String> placeholdersByValue = new LinkedHashMap<>();
        StringBuffer anonymizedText = new StringBuffer();

        while (matcher.find()) {
            String value = matcher.group();
            String placeholder = placeholdersByValue.computeIfAbsent(
                    value, ignored -> "[" + rule.name() + "_" + (placeholdersByValue.size() + 1) + "]");
            matcher.appendReplacement(anonymizedText, Matcher.quoteReplacement(placeholder));
        }
        matcher.appendTail(anonymizedText);
        return anonymizedText.toString();
    }
}
