# 🧪 Tests — Codabli Backend

Guide d'exécution et d'organisation de la suite de tests. Pour l'installation
générale et le lancement de l'application, voir le [README principal](README.md).

---

## ⚠️ Prérequis important : la base de données doit tourner

Une partie des tests (les tests de **contrôleurs**) sont des `@SpringBootTest` :
ils démarrent le **contexte Spring complet**, donc la couche JPA/Hibernate, qui
se connecte à **PostgreSQL sur `localhost:5442`**.

**Sans base démarrée, ces tests échouent tous** avec :

```
Connection to localhost:5442 refused. Check that the hostname and port are correct...
```

Ce n'est pas un bug du code : c'est le prérequis normal. Démarre l'infra avant de
lancer les tests :

```bash
docker compose up -d postgres
```

> 💡 Les tests de **services** (Mockito) n'ont, eux, **pas** besoin de base — voir
> le tableau plus bas.

---

## ▶️ Lancer les tests

### Option A — natif (nécessite un JDK 17+)

```bash
docker compose up -d postgres   # si ce n'est pas déjà fait
./mvnw test
```

### Option B — tout Docker (aucun JDK requis sur l'hôte)

Si la machine n'a qu'un JRE (pas de `javac`), on compile/exécute les tests dans un
conteneur Maven + JDK 17 :

```bash
docker compose up -d postgres
docker run --rm --network host \
  -v "$PWD":/app -w /app \
  maven:3.9-eclipse-temurin-17 mvn -B test
```

*(`--network host` permet au conteneur d'atteindre le PostgreSQL publié sur
`localhost:5442`.)*

### Commandes utiles

```bash
# Une seule classe de test
./mvnw test -Dtest=RessourcePedagogiqueServiceTest

# Une seule méthode
./mvnw test -Dtest=RessourcePedagogiqueServiceTest#creer_shouldSaveRessource

# Continuer malgré les échecs et voir le récapitulatif complet
./mvnw test -Dmaven.test.failure.ignore=true
```

Les rapports détaillés sont générés dans **`target/surefire-reports/`**.

---

## 🗂️ Organisation de la suite

**59 tests** répartis en **2 familles** :

| Famille | Annotation | Base requise ? | Rôle |
| --- | --- | :---: | --- |
| Tests de **contrôleurs** | `@SpringBootTest` + `@AutoConfigureMockMvc` | ✅ Oui | Vérifient les endpoints HTTP de bout en bout (routes, statuts, sécurité par rôle) avec le contexte Spring réel |
| Tests de **services** | `@ExtendWith(MockitoExtension.class)` | ❌ Non | Vérifient la logique métier isolée (dépôt mocké) : règles d'accès, filtres, calculs |

### Inventaire

**Contrôleurs — `@SpringBootTest` (BDD requise) — 27 tests**

| Classe | Nb tests |
| --- | :---: |
| `RessourcePedagogiqueControllerTest` | 8 |
| `GalerieControllerTest` | 6 |
| `ActualiteControllerTest` | 5 |
| `CommandeControllerTest` | 5 |
| `ConteDanseControllerTest` | 3 |

**Services — Mockito (pas de BDD) — 32 tests**

| Classe | Nb tests |
| --- | :---: |
| `RessourcePedagogiqueServiceTest` | 9 |
| `GalerieServiceTest` | 6 |
| `PanierServiceTest` | 6 |
| `ProduitServiceTest` | 6 |
| `CommandeServiceTest` | 3 |
| `ActualiteServiceTest` | 1 |
| `ConteDanseServiceTest` | 1 |

---

## 🧩 Conventions

- **Mockito en mode strict** (par défaut avec `MockitoExtension`) : un *stub*
  déclaré mais jamais utilisé fait échouer le test (`UnnecessaryStubbingException`).
  Pour des *fixtures partagées* dans `@BeforeEach` que seuls certains tests
  utilisent (ex. plusieurs JWT de rôles différents), on déclare le stub en
  `lenient().when(...)` — ainsi la strictness reste active partout ailleurs.
- Les tests de services **ne doivent pas** dépendre d'une base : le dépôt est
  toujours mocké. Si un test de service a besoin de la base, c'est le signe qu'il
  devrait être un test d'intégration/contrôleur.

---

## 🩺 Dépannage

| Symptôme | Cause probable | Solution |
| --- | --- | --- |
| Tous les `*ControllerTest` échouent, `Connection to localhost:5442 refused` | PostgreSQL non démarré | `docker compose up -d postgres` |
| `release version 17 not supported` | Pas de JDK (seulement un JRE) sur l'hôte | Utiliser l'**option B** (tout Docker) ou installer un JDK 17+ |
| `UnnecessaryStubbingException` | Stub partagé non utilisé par tous les tests | Passer le stub en `lenient().when(...)` |
