package com.codabli.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PromptAnonymizerServiceTest {

    private final PromptAnonymizerService promptAnonymizerService = new PromptAnonymizerService();

    @Test
    void anonymize_shouldReplaceEmailPhoneIpAndUrl() {
        String text = "Email enfant@example.com, téléphone 06 12 34 56 78, "
                + "IP 192.168.1.1 et URL https://example.com/profile";

        assertEquals("Email [EMAIL_1], téléphone [TELEPHONE_1], IP [IP_ADDRESS_1] et URL [URL_1]",
                promptAnonymizerService.anonymize(text));
    }

    @Test
    void anonymize_shouldReusePlaceholderForRepeatedValueAndNumberDistinctValues() {
        String text = "Enfant enfant@example.com, parent parent@example.com, encore enfant@example.com.";

        assertEquals("Enfant [EMAIL_1], parent [EMAIL_2], encore [EMAIL_1].",
                promptAnonymizerService.anonymize(text));
    }

    @Test
    void anonymize_shouldNumberValuesIndependentlyForEachCategory() {
        String text = "06 12 34 56 78 et 07 11 22 33 44, enfant@example.com et parent@example.com";

        assertEquals("[TELEPHONE_1] et [TELEPHONE_2], [EMAIL_1] et [EMAIL_2]",
                promptAnonymizerService.anonymize(text));
    }

    @Test
    void anonymize_shouldLeavePersonRuleDisabled() {
        String text = "Mme Dupont, voici mon adresse enfant@example.com.";

        assertEquals("Mme Dupont, voici mon adresse [EMAIL_1].", promptAnonymizerService.anonymize(text));
    }

    @Test
    void anonymize_shouldReturnEmptyStringForNullOrBlankText() {
        assertEquals("", promptAnonymizerService.anonymize(null));
        assertEquals("", promptAnonymizerService.anonymize("  "));
    }
}
