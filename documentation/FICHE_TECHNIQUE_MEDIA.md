# Gestion technique des Médias

Ce document décrit les cinématiques fonctionnelles sécurisées pour le dépôt et la récupération des médias au sein de l'application (Frontend Angular, Backend Java, Stockage compatible S3).

---

## 1. Processus de dépôt d'un média (Upload)

### 1.1 Objectif
Permettre à un utilisateur authentifié de déposer un média depuis l'interface, tout en garantissant un contrôle d'intégrité, de sécurité sanitaire (anti-malware) et une isolation totale des accès au stockage.

### 1.2 Cinématique fonctionnelle
```
[ Utilisateur ] ──(1. Sélection)──> [ Front-end Angular ]
                                            │
                                    (2. Transfert Flux)
                                            ▼
                                    [ Back-end Java ]
                                            │ (3. Contrôles & Renommage)
                                            │ (4. Chiffrement)
                                            ▼
                                    [ Stockage S3 ]
```

### 1.3 Description détaillée des étapes

1. **Sélection et envoi initial :** L’utilisateur sélectionne un fichier depuis l'interface **Angular**. Le front-end transmet le fichier binaire brut accompagné de ses métadonnées d'origine (dont le nom du fichier) au **Back-end Java** via un flux sécurisé (`MultipartFormData`). Aucun accès direct au stockage n'est accordé au navigateur.
2. **Contrôle de sécurité sanitaire (Anti-Spoofing) :** Le **Back-end Java** intercepte le flux et analyse les premiers octets du fichier (*Magic Numbers*) afin de valider son type MIME réel. Le système rejette le fichier si l'extension déclarée ne correspond pas à sa structure technique interne (ex: un script exécutable renommé en `.jpg`).
3. **Analyse Antivirus :** Le flux binaire est soumis en temps réel à un pré-scanner de logiciels malveillants avant toute écriture définitive.
4. **Anonymisation du stockage et conservation du nom :** 
    * **Côté Stockage (S3) :** Le nom d'origine du fichier est purgé de la clé de stockage physique pour éviter l'injection de caractères malveillants dans l'arborescence ou la fuite de données sensibles. Le **Back-end Java** génère un identifiant unique universel (UUID). Le fichier est exclusivement stocké sous la forme `[UUID].[extension]`.
    * **Côté Base de Données (Java) :** Le **Back-end Java** conserve et associe de manière sécurisée le **nom d'origine du média** à cet UUID, permettant sa restitution ultérieure à l'utilisateur.
5. **Persistance et Chiffrement :** Le **Back-end Java** pousse le fichier vers le stockage S3 en utilisant ses propres identifiants applicatifs privés. Une instruction de chiffrement côté serveur (*Server-Side Encryption*) est jointe à la requête pour s'assurer que le média est crypté dès son écriture sur le disque.

---

## 2. Processus de récupération d'un média (Download / Affichage)

### 2.1 Objectif
Permettre la mise à disposition sécurisée d'un média à un utilisateur autorisé, sans exposer publiquement l'arborescence du stockage et sans consommer les ressources réseau du serveur Java pour le transfert des fichiers lourds.

### 2.2 Cinématique fonctionnelle
```
[ Utilisateur ] <──(4. Affichage avec Nom)── [ Front-end Angular ]
                                                    ▲
                                            (3. URL Signée)
                                                    │
[ Stockage S3 ] <──(3. Flux direct)─────────────────┤
                                                    │
                                            (2. Autorisation)
                                                    ▼
                                            [ Back-end Java ]
```

### 2.3 Description détaillée des étapes

1. **Demande d'accès :** Lors de la navigation, l'application **Angular** requiert l'accès à un média en transmettant l'identifiant unique (UUID) au **Back-end Java**.
2. **Contrôle des habilitations et récupération du nom :** Le **Back-end Java** vérifie dans la base de données les droits d'accès de l'utilisateur connecté sur le média demandé. Si l'accès est validé, Java extrait de la base de données le **nom d'origine du média** associé à cet UUID.
3. **Génération d'un jeton d'accès temporaire personnalisé :** Le **Back-end Java** génère une **URL pré-signée** de téléchargement auprès du service de stockage.
    * L'URL intègre une signature cryptographique éphémère et une durée de validité restreinte (ex: 15 minutes).
    * Pour garantir que le fichier soit téléchargé ou affiché avec son **nom d'origine** (et non avec l'UUID de stockage), Java injecte le paramètre `response-content-disposition` dans la signature S3 (ex: `attachment; filename="nom_origine.jpg"`).
4. **Restitution au Front-end :** Le **Back-end Java** renvoie l'URL sécurisée ainsi que le nom d'origine du média à **Angular**. Le serveur Java est immédiatement libéré de la charge réseau liée au fichier.
5. **Affichage et téléchargement personnalisé :** Le navigateur de l'utilisateur télécharge ou charge le média en interrogeant directement l'URL pré-signée. Grâce à la configuration de la disposition du contenu, le fichier est présenté ou enregistré sur le poste de l'utilisateur avec son **nom d'origine légitime**, maintenant une expérience utilisateur transparente et sécurisée.
