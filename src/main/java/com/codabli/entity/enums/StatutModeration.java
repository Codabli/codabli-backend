package com.codabli.entity.enums;

/**
 * Statuts de modération d'une création (Cartes à Conte, etc.), alignés sur le
 * cahier des charges (section 8.24 / GAL-25) : Brouillon, Soumis, En
 * vérification, A corriger, Validé, Refusé, Publié, Retiré, Archivé.
 */
public enum StatutModeration {
    brouillon,
    soumis,
    en_verification,
    a_corriger,
    valide,
    refuse,
    publie,
    retire,
    archive
}