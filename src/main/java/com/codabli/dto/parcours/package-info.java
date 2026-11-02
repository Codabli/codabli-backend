/**
 * Contenu des etapes du parcours "Creer mon conte danse", stocke en JSON
 * (une colonne jsonb par etape de ParcoursConte).
 * <p>
 * Les noms de champs sont ceux du JSON echange avec le front (interfaces
 * tale-*.interface.ts), d'ou l'anglais. Un champ inconnu est refuse (voir
 * JacksonConfig). Un brouillon est incomplet par nature : les textes peuvent
 * etre vides, seuls les identifiants sont obligatoires.
 */
package com.codabli.dto.parcours;
