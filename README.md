# Codabli Backend

Base Spring Boot Maven pour le backend Codabli.

Pré-requis: Java 17+.

## Structure

- `controller` : API HTTP et orchestration des requêtes.
- `service` : logique métier et règles applicatives.
- `repository` : accès aux données via Spring Data JPA.
- `entity` : modèle persistant JPA.
- `dto` : objets de transport pour les échanges API.
- `config` : configuration Spring, sécurité et intégrations.

## Profils

- `dev` : configuration locale PostgreSQL.
- `prod` : configuration injectée par variables d'environnement.

## Lancer

```bash
mvn spring-boot:run
```