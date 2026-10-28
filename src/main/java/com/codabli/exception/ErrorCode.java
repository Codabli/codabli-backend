package com.codabli.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    //errors 400
    ILLEGAL_ARGUMENT("error.illegal.argument", HttpStatus.BAD_REQUEST),
    INVALID_MEDIA("error.invalid.media", HttpStatus.BAD_REQUEST),

    //errors 403
    ACCESS_DENIED("error.access-denied",HttpStatus.FORBIDDEN),

    //errors 404
    UNKNOWN_ABONNEMENT("error.unknown.abonnement", HttpStatus.NOT_FOUND),
    UNKNOWN_ACTUALITE("error.unknown.actualite", HttpStatus.NOT_FOUND),
    UNKNOWN_ADRESSE("error.unknown.adresse", HttpStatus.NOT_FOUND),
    UNKNOWN_CARNET_ELEMENT("error.unknown.carnet-element", HttpStatus.NOT_FOUND),
    UNKNOWN_CARNET_PAGE("error.unknown.carnet-page", HttpStatus.NOT_FOUND),
    UNKNOWN_CARNET_VOYAGE("error.unknown.carnet-voyage", HttpStatus.NOT_FOUND),
    UNKNOWN_CARTE_CONTE("error.unknown.carte-conte", HttpStatus.NOT_FOUND),
    UNKNOWN_CLASSE("error.unknown.classe", HttpStatus.NOT_FOUND),
    UNKNOWN_COMMANDE("error.unknown.commande", HttpStatus.NOT_FOUND),
    UNKNOWN_CONTE_DANSE("error.unknown.conte-danse", HttpStatus.NOT_FOUND),
    UNKNOWN_CONTE_TRADUCTION("error.unknown.conte-traduction", HttpStatus.NOT_FOUND),
    UNKNOWN_DEMANDE_CONTACT("error.unknown.demande-contact", HttpStatus.NOT_FOUND),
    UNKNOWN_ELEVE("error.unknown.eleve", HttpStatus.NOT_FOUND),
    UNKNOWN_EVALUATION_PEDAGOGIQUE("error.unknown.evaluation-pedagogique", HttpStatus.NOT_FOUND),
    UNKNOWN_FICHE_ACTIVITE("error.unknown.fiche-activite", HttpStatus.NOT_FOUND),
    UNKNOWN_FRESQUE("error.unknown.fresque", HttpStatus.NOT_FOUND),
    UNKNOWN_GALERIE_MISE_AVANT("error.unknown.galerie-mise-avant", HttpStatus.NOT_FOUND),
    UNKNOWN_GALERIE_SALLE("error.unknown.galerie-salle", HttpStatus.NOT_FOUND),
    UNKNOWN_HOTSPOT("error.unknown.hotspot", HttpStatus.NOT_FOUND),
    UNKNOWN_KEYCLOAK_USER("error.unknown.keycloak-user", HttpStatus.NOT_FOUND),
    UNKNOWN_MISE_AVANT("error.unknown.mise-avant", HttpStatus.NOT_FOUND),
    UNKNOWN_MEDIA("error.unknown.media", HttpStatus.NOT_FOUND),
    UNKNOWN_OFFRE_COACHING("error.unknown.offre-coaching", HttpStatus.NOT_FOUND),
    UNKNOWN_PANIER_LIGNE("error.unknown.panier-ligne", HttpStatus.NOT_FOUND),
    UNKNOWN_PARTENAIRE("error.unknown.partenaire", HttpStatus.NOT_FOUND),
    UNKNOWN_PRODUIT("error.unknown.produit", HttpStatus.NOT_FOUND),
    UNKNOWN_PROFIL_ENFANT("error.unknown.profil-enfant", HttpStatus.NOT_FOUND),
    UNKNOWN_RESERVATION_COACHING("error.unknown.reservation-coaching", HttpStatus.NOT_FOUND),
    UNKNOWN_RESSOURCE_PEDAGOGIQUE("error.unknown.ressource-pedagogique", HttpStatus.NOT_FOUND),
    UNKNOWN_USER("error.unknown.user", HttpStatus.NOT_FOUND),
    UNKNOWN_WEBINAIRE("error.unknown.webinaire", HttpStatus.NOT_FOUND),
    UNKNOWN_WEBINAIRE_INSCRIPTION("error.unknown.webinaire-inscription", HttpStatus.NOT_FOUND),

    //errors 409
    SOCK_INSUFFISANT("error.stock-insuffisant", HttpStatus.CONFLICT),
    MEDIA_IN_USE("error.media.in-use", HttpStatus.CONFLICT),

    //errors 503
    MEDIA_SCAN_UNAVAILABLE("error.media.scan-unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    MEDIA_STORAGE_UNAVAILABLE("error.media.storage-unavailable", HttpStatus.SERVICE_UNAVAILABLE),

    //errors 413
    MEDIA_TOO_LARGE("error.media.too-large", HttpStatus.CONTENT_TOO_LARGE),

    //errors 422
    MEDIA_INFECTED("error.media.infected", HttpStatus.UNPROCESSABLE_CONTENT),

    //errors 500
    INTERNAL_SERVER_ERROR("error.system.internal", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String key;
    private final HttpStatus status;

    ErrorCode(String key, HttpStatus status) {
        this.key = key;
        this.status = status;
    }

    public String getKey() { return key; }
    public HttpStatus getStatus() { return status; }
}
