# UBAX Platform – Tâches Frontend (Jira)

> **Scope :** Team Agence · Team Hôtel · Bailleur · Administrateur  
> **Base URL :** `http://localhost:9999/api`  
> **Auth :** `Authorization: Bearer <access_token>` (sauf routes marquées Public)  
> **Réponse standard :**
> ```json
> { "status": "SUCCESS", "statusCode": 200, "message": "...", "data": { ... } }
> ```
> **Pagination :** `data` contient `{ "content": [...], "totalElements": N, "totalPages": N, "size": N, "number": N }`

---

## MODULE 1 — TEAM AGENCE

**Epic :** `UBAX-FE-TEAM-AGENCE`  
**Rôle requis (JWT) :** `UBAX_PARTNER` · Sous-rôle DB requis pour certaines actions : `DIRECTEUR_AGENCE`

### Objectif

Une **agence partenaire** UBAX peut constituer une équipe interne composée de plusieurs collaborateurs, chacun disposant d'un ou plusieurs sous-rôles fonctionnels. Ce module permet au Directeur d'agence de gérer cette équipe directement depuis l'espace partenaire.

**Flux principal :**
1. Le Directeur d'agence (`DIRECTEUR_AGENCE`) ajoute un collaborateur en renseignant ses informations et en lui assignant d'emblée un ou plusieurs sous-rôles.
2. Le backend crée le compte Keycloak avec le rôle `UBAX_PARTNER`, lie l'utilisateur à l'agence, et persiste les sous-rôles en base.
3. Le Directeur peut à tout moment consulter les sous-rôles d'un membre, en assigner de nouveaux ou en révoquer.
4. Tout membre de l'agence peut consulter la liste de l'équipe, mais seul le `DIRECTEUR_AGENCE` peut modifier la composition ou les rôles.

**Périmètre frontend de ce module :**

| Tâche | Action | Acteur |
|-------|--------|--------|
| UBAX-FE-101 — Lister les membres | Lecture | Tout membre `PARTNER` |
| UBAX-FE-102 — Ajouter un membre | Écriture | `DIRECTEUR_AGENCE` uniquement |
| UBAX-FE-103 — Assigner des sous-rôles | Écriture | `DIRECTEUR_AGENCE` uniquement |
| UBAX-FE-104 — Consulter les sous-rôles | Lecture | Tout membre `PARTNER` |
| UBAX-FE-105 — Révoquer un sous-rôle | Écriture | `DIRECTEUR_AGENCE` uniquement |

**Points d'attention :**
- Les sous-rôles sont en **base de données**, pas dans le JWT. Le frontend doit appeler `GET /sub-roles` pour connaître les droits réels d'un membre avant d'afficher ou masquer les actions sensibles.
- Chaque collaborateur appartient à **une seule agence** — l'`agencyId` est résolu automatiquement depuis le token du Directeur, le frontend n'a pas à le transmettre.
- Le sous-rôle `DIRECTEUR_AGENCE` est le seul habilité à écrire — masquer les boutons d'action pour les autres sous-rôles dès le chargement de la page.

---

### UBAX-FE-101 · Lister les membres de l'équipe agence

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/agency/team` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Request body** | _(aucun)_ |
| **Query params** | _(aucun)_ |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "Team members retrieved successfully",
  "data": [
    {
      "id": "uuid",
      "keycloakId": "string",
      "firstName": "string",
      "lastName": "string",
      "email": "string",
      "phone": "string",
      "dateOfBirth": "2000-01-01",
      "address": "string",
      "city": "string",
      "country": "string",
      "language": "string",
      "avatarUrl": "string",
      "roles": ["PARTNER"],
      "agencyId": "uuid",
      "agencyName": "string",
      "hotelId": null,
      "hotelName": null,
      "emailVerified": true,
      "phoneVerified": false,
      "identityVerified": false,
      "active": true,
      "lastLoginAt": "2026-04-30T10:00:00",
      "createdAt": "2026-01-01T00:00:00",
      "updatedAt": "2026-01-01T00:00:00"
    }
  ]
}
```

**Critères d'acceptation :**
- [ ] Afficher la liste des membres avec nom, email, téléphone, statut actif
- [ ] Afficher les sous-rôles de chaque membre (appel UBAX-FE-104 par membre ou dans un tableau combiné)
- [ ] État vide si aucun membre
- [ ] Loader pendant la requête

---

### UBAX-FE-102 · Ajouter un membre à l'équipe agence

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/agency/team` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` + sous-rôle `DIRECTEUR_AGENCE` |
| **Content-Type** | `application/json` |

**Request body :**
```json
{
  "firstName": "string (2–100 chars, requis)",
  "lastName": "string (2–100 chars, requis)",
  "email": "string (email valide, requis)",
  "phone": "string (format international +XXX, requis)",
  "subRoles": ["COMMERCIAL", "COMPTABLE_AGENCE"]
}
```

> **Valeurs `subRoles` disponibles (scope AGENCE) :**  
> `DIRECTEUR_AGENCE` · `COMMERCIAL` · `COMPTABLE_AGENCE` · `AGENT_SAV`

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "Team member added successfully",
  "data": {
    "id": "uuid",
    "keycloakId": "string",
    "firstName": "string",
    "lastName": "string",
    "email": "string",
    "phone": "string",
    "roles": ["PARTNER"],
    "agencyId": "uuid",
    "agencyName": "string",
    "emailVerified": false,
    "active": true,
    "createdAt": "2026-04-30T10:00:00",
    "updatedAt": "2026-04-30T10:00:00"
  }
}
```

**Erreurs possibles :**
- `409 Conflict` — email déjà utilisé
- `400 Bad Request` — champs invalides

**Critères d'acceptation :**
- [ ] Formulaire modal avec champs firstName, lastName, email, phone, subRoles (multiselect)
- [ ] Validation frontend avant envoi (format email, longueur, format phone)
- [ ] Message de succès + rafraîchissement de la liste
- [ ] Afficher message d'erreur si conflit email

---

### UBAX-FE-103 · Assigner des sous-rôles à un membre agence

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/agency/team/{userId}/sub-roles` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` + sous-rôle `DIRECTEUR_AGENCE` |
| **Path params** | `userId` : UUID du membre |

**Request body :**
```json
["COMMERCIAL", "AGENT_SAV"]
```
_(tableau de strings non vide)_

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "Sub-roles assigned successfully",
  "data": [
    {
      "id": "uuid",
      "userId": "uuid",
      "role": "COMMERCIAL",
      "scope": "AGENCE",
      "createdAt": "2026-04-30T10:00:00"
    }
  ]
}
```

**Critères d'acceptation :**
- [ ] Bouton « Gérer les rôles » sur chaque ligne membre
- [ ] Modal de sélection multiple des sous-rôles disponibles
- [ ] Pré-sélection des sous-rôles déjà assignés (appel GET UBAX-FE-104 d'abord)
- [ ] Confirmation de succès

---

### UBAX-FE-104 · Consulter les sous-rôles d'un membre agence

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/agency/team/{userId}/sub-roles` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Path params** | `userId` : UUID du membre |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "Sub-roles retrieved",
  "data": [
    {
      "id": "uuid",
      "userId": "uuid",
      "role": "DIRECTEUR_AGENCE",
      "scope": "AGENCE",
      "createdAt": "2026-04-30T10:00:00"
    }
  ]
}
```

**Critères d'acceptation :**
- [ ] Afficher les badges de rôle dans le tableau des membres
- [ ] Utilisé en pré-condition pour UBAX-FE-103 et UBAX-FE-105

---

### UBAX-FE-105 · Révoquer un sous-rôle d'un membre agence

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `DELETE /v1/agency/team/{userId}/sub-roles/{role}` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` + sous-rôle `DIRECTEUR_AGENCE` |
| **Path params** | `userId` : UUID · `role` : ex. `COMMERCIAL` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "Sub-role revoked successfully",
  "data": null
}
```

**Critères d'acceptation :**
- [ ] Bouton de suppression par badge de sous-rôle dans la modal de gestion
- [ ] Dialog de confirmation avant suppression
- [ ] Rafraîchissement de l'affichage des sous-rôles après suppression

---

---

## MODULE 2 — TEAM HÔTEL

**Epic :** `UBAX-FE-TEAM-HOTEL`  
**Rôle requis (JWT) :** `UBAX_PARTNER` · Sous-rôle DB : `GERANT_HOTEL` pour actions d'écriture

### Objectif

Un **hôtel partenaire** UBAX fonctionne sur le même modèle que l'agence immobilière mais avec un organigramme adapté au secteur hôtelier. Ce module permet au Gérant de l'hôtel de constituer et de gérer son équipe opérationnelle depuis l'espace partenaire.

**Flux principal :**
1. Le Gérant d'hôtel (`GERANT_HOTEL`) ajoute un collaborateur (réceptionniste, comptable, responsable hébergement) en lui assignant un ou plusieurs sous-rôles hôtel.
2. Le backend crée le compte avec le rôle `UBAX_PARTNER`, lie l'utilisateur à l'hôtel et persiste les sous-rôles avec le scope `HOTEL`.
3. Le Gérant peut ensuite modifier les sous-rôles de chaque membre selon l'évolution de l'organisation.

**Périmètre frontend de ce module :**

| Tâche | Action | Acteur |
|-------|--------|--------|
| UBAX-FE-201 — Lister les membres | Lecture | Tout membre `PARTNER` (hôtel) |
| UBAX-FE-202 — Ajouter un membre | Écriture | `GERANT_HOTEL` uniquement |
| UBAX-FE-203 — Assigner des sous-rôles | Écriture | `GERANT_HOTEL` uniquement |
| UBAX-FE-204 — Consulter les sous-rôles | Lecture | Tout membre `PARTNER` (hôtel) |
| UBAX-FE-205 — Révoquer un sous-rôle | Écriture | `GERANT_HOTEL` uniquement |

**Points d'attention :**
- Ce module est **techniquement identique à Module 1** (Team Agence) — seuls changent le scope (`HOTEL`), les routes (`/v1/hotel/team`), et la liste des sous-rôles disponibles dans les selects.
- Privilégier la **réutilisation des composants** Team Agence en passant un paramètre `scope` ou `partnerType` plutôt que de dupliquer le code.
- Le champ `hotelName` doit être affiché là où Module 1 affiche `agencyName`.
- Un utilisateur ne peut appartenir qu'à **un seul hôtel** — même logique de résolution automatique de l'`hotelId` depuis le token.

> Structure identique à Team Agence, scope `HOTEL`. Réutiliser les composants avec prop `scope="HOTEL"`.

---

### UBAX-FE-201 · Lister les membres de l'équipe hôtel

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/hotel/team` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |

**Response `200` :** _(identique à UBAX-FE-101 — tableau de `UserResponse`)_

**Critères d'acceptation :**
- [ ] Même UI que Team Agence adaptée au contexte hôtel
- [ ] Afficher `hotelName` à la place de `agencyName`

---

### UBAX-FE-202 · Ajouter un membre à l'équipe hôtel

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/hotel/team` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` + sous-rôle `GERANT_HOTEL` |

**Request body :** _(identique à UBAX-FE-102)_
```json
{
  "firstName": "string",
  "lastName": "string",
  "email": "string",
  "phone": "string",
  "subRoles": ["RECEPTIONNISTE"]
}
```

> **Valeurs `subRoles` disponibles (scope HOTEL) :**  
> `GERANT_HOTEL` · `RECEPTIONNISTE` · `COMPTABLE_HOTEL` · `RESPONSABLE_HEBERGEMENT`

**Response `201` :** _(identique à UBAX-FE-102)_

**Critères d'acceptation :**
- [ ] Formulaire identique à UBAX-FE-102 mais avec les sous-rôles hôtel dans le multiselect
- [ ] Validation et gestion d'erreurs identiques

---

### UBAX-FE-203 · Assigner des sous-rôles à un membre hôtel

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/hotel/team/{userId}/sub-roles` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` + sous-rôle `GERANT_HOTEL` |
| **Path params** | `userId` : UUID |

**Request body / Response :** _(identique à UBAX-FE-103, scope HOTEL)_

---

### UBAX-FE-204 · Consulter les sous-rôles d'un membre hôtel

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/hotel/team/{userId}/sub-roles` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |

**Response `200` :** _(identique à UBAX-FE-104)_

---

### UBAX-FE-205 · Révoquer un sous-rôle d'un membre hôtel

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `DELETE /v1/hotel/team/{userId}/sub-roles/{role}` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` + sous-rôle `GERANT_HOTEL` |
| **Path params** | `userId` : UUID · `role` : ex. `RECEPTIONNISTE` |

**Response `200` :** _(identique à UBAX-FE-105)_

---

---

## MODULE 3 — BAILLEUR

**Epic :** `UBAX-FE-BAILLEUR`

### Objectif

Un **bailleur** est un propriétaire immobilier qui souhaite confier la gestion de ses biens à une agence partenaire UBAX.
Ce module couvre l'intégralité du cycle de vie d'une demande d'adhésion bailleur, du formulaire public jusqu'à la décision de l'agence.

**Flux principal :**
1. Le CLIENT connecté choisit une agence (`GET /v1/agencies`) puis remplit un formulaire depuis l'app mobile : pièce d'identité (type + numéro + recto/verso uploadés), et une description obligatoire de son objectif (minimum 10 mots).
2. La demande est reçue par l'agence ciblée avec le statut `PENDING`.
3. Le Directeur d'agence (`DIRECTEUR_AGENCE`) contacte le bailleur en dehors de l'app pour vérifier ses biens, puis **approuve** ou **rejette** depuis l'espace partenaire.
4. En cas d'approbation : si le bailleur est nouveau → compte `UBAX_OWNER` créé automatiquement + SMS envoyé ; si le compte existe déjà → lien agence↔bailleur ajouté. Aucune action frontend supplémentaire requise.
5. L'agence crée ensuite les biens du bailleur via `POST /v1/properties` en passant `ownerId` = UUID du bailleur.
6. L'équipe admin UBAX dispose d'une vue globale en lecture seule sur toutes les demandes de toutes les agences.

**Périmètre frontend de ce module :**

| Tâche | Acteur | Accès |
|-------|--------|-------|
| UBAX-FE-301 — Formulaire de demande | Bailleur (CLIENT connecté) | App mobile |
| UBAX-FE-302 — Liste des demandes reçues | Directeur d'agence | Espace partenaire |
| UBAX-FE-303 — Détail d'une demande | Directeur d'agence | Espace partenaire |
| UBAX-FE-304 — Décision (approuver / rejeter) | Directeur d'agence | Espace partenaire |
| UBAX-FE-305 — Vue globale toutes agences | Admin / Super Admin | Back-office UBAX |
| UBAX-FE-306 — Liste des bailleurs de l'agence | Directeur d'agence | Espace partenaire |

**Points d'attention :**
- Le formulaire **n'est pas public** — le CLIENT doit être connecté. Rediriger vers le login si token absent.
- L'`agencyId` est passé en paramètre d'URL ou sélectionné depuis `GET /v1/agencies` — le frontend doit l'injecter automatiquement dans la requête.
- La `description` est **obligatoire** et doit contenir au moins 10 mots (le backend retourne une `400` si manquant ou trop court).
- Le corps ne contient **aucune liste de biens** — la vérification des biens se fait hors-app entre le Directeur et le bailleur.
- La décision `REJECT` doit obligatoirement inclure un `comment` expliquant le motif.

---

### UBAX-FE-301 · Formulaire de demande d'adhésion bailleur (CLIENT connecté)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/bailleur/apply` |
| **Auth** | Bearer token · Rôle **`UBAX_CLIENT`** (obligatoire) |
| **Content-Type** | `application/json` |

> ⚠️ **Important** : cet endpoint n'est **pas public**. Le CLIENT doit être connecté. Les champs `firstName`, `lastName` et `phone` sont extraits automatiquement du compte. Le champ `email` est extrait du compte **sauf** si le compte n'en possède pas — dans ce cas il doit être fourni dans le body (le backend retourne une `400` explicite si manquant).

**Flux d'upload pièce d'identité (mobile) :**
```
1. POST /v1/storage/upload?bucket=bailleur-documents  (multipart, recto)
   → { fileUrl: "http://minio/.../bailleur-uuid.jpg" }

2. POST /v1/storage/upload?bucket=bailleur-documents  (multipart, verso)
   → { fileUrl: "http://minio/.../bailleur-uuid.jpg" }

3. POST /v1/bailleur/apply  (inclure les deux fileUrl)
```

**Request body :**
```json
{
  "agencyId": "uuid (requis)",
  "idType": "CNI",
  "idNumber": "string (max 100, requis)",
  "email": "string (requis uniquement si le compte n'a pas d'email)",
  "idDocRectoUrl": "string (URL pré-uploadée recto, optionnel mais recommandé)",
  "idDocVersoUrl": "string (URL pré-uploadée verso, optionnel mais recommandé)",
  "description": "string (obligatoire, min 10 mots, max 1000 chars)"
}
```

> **Valeurs `idType` :** `CNI` · `PASSEPORT` · `PERMIS_CONDUIRE` · `TITRE_SEJOUR` · `CARTE_CONSULAIRE`

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "Application submitted successfully",
  "data": {
    "id": "uuid",
    "agencyId": "uuid",
    "agencyName": "string",
    "firstName": "string",
    "lastName": "string",
    "phone": "string",
    "email": "string",
    "idType": "CNI",
    "idNumber": "string",
    "idDocRectoUrl": "string",
    "idDocVersoUrl": "string",
    "description": "string",
    "status": "PENDING",
    "rejectionReason": null,
    "reviewedByName": null,
    "reviewedAt": null,
    "createdAt": "2026-04-30T10:00:00",
    "updatedAt": "2026-04-30T10:00:00"
  }
}
```

**Erreurs possibles :**
- `400 Bad Request` — email absent du compte ET absent du body

**Critères d'acceptation :**
- [ ] Accessible uniquement pour un CLIENT connecté (rediriger vers login si non authentifié)
- [ ] Étape 1 : sélection de l'agence (`agencyId` — liste depuis `GET /v1/agencies` ou passé en param URL)
- [ ] Étape 2 : pièce d'identité
  - `idType` (select) + `idNumber` (texte) — obligatoires
  - Si le compte n'a pas d'email : afficher un champ `email` obligatoire
  - Upload recto/verso (2 appels `POST /v1/storage/upload?bucket=bailleur-documents` avant soumission) — **recommandé mais non bloquant**
- [ ] Étape 3 : `description` **obligatoire** (textarea, min 10 mots, max 1000 chars) — afficher compteur de mots en temps réel
- [ ] Les infos personnelles (nom, prénom, téléphone) affichées en lecture seule depuis le compte
- [ ] Validation : `agencyId`, `idType`, `idNumber`, `description` (min 10 mots) obligatoires · `email` obligatoire si compte sans email
- [ ] Page de confirmation après soumission réussie (`status: PENDING`) avec message : « Votre demande a été transmise à l'agence. Un conseiller vous contactera pour vérifier vos biens. »
- [ ] Gestion d'erreur `400` si email manquant (compte sans email et body sans email) ou description trop courte

---

### UBAX-FE-302 · Liste des demandes bailleur reçues (Agence)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/bailleur/agency/applications` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` + sous-rôle `DIRECTEUR_AGENCE` |
| **Query params** | `page` (défaut 0) · `size` (défaut 20) · `sort=createdAt,desc` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "Applications retrieved",
  "data": {
    "content": [
      {
        "id": "uuid",
        "agencyId": "uuid",
        "agencyName": "string",
        "firstName": "string",
        "lastName": "string",
        "phone": "string",
        "email": "string",
        "idType": "CNI",
        "idNumber": "string",
        "idDocRectoUrl": "string",
        "idDocVersoUrl": "string",
        "description": "string",
        "status": "PENDING",
        "rejectionReason": null,
        "reviewedByName": null,
        "reviewedAt": null,
        "createdAt": "2026-04-30T10:00:00",
        "updatedAt": "2026-04-30T10:00:00"
      }
    ],
    "totalElements": 42,
    "totalPages": 3,
    "size": 20,
    "number": 0
  }
}
```

**Valeurs `status` :** `PENDING` · `APPROVED` · `REJECTED` · `CANCELLED`

**Critères d'acceptation :**
- [ ] Tableau paginé avec colonnes : nom complet, email, téléphone, statut (badge coloré), date, actions
- [ ] Filtre par statut
- [ ] Pagination avec navigation page suivante/précédente
- [ ] Lien vers le détail (UBAX-FE-303)

---

### UBAX-FE-303 · Détail d'une demande bailleur (Agence)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/bailleur/agency/applications/{id}` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` + sous-rôle `DIRECTEUR_AGENCE` |
| **Path params** | `id` : UUID de la demande |

**Response `200` :** _(objet `BailleurApplicationResponse` complet — voir UBAX-FE-302)_

**Critères d'acceptation :**
- [ ] Page de détail avec les informations du bailleur (nom, email, téléphone, pièce d'identité)
- [ ] Afficher les images recto/verso de la pièce d'identité (`idDocRectoUrl`, `idDocVersoUrl`) si présentes
- [ ] Section « Description » : afficher le texte libre saisi par le bailleur
- [ ] Note informative : « La vérification des biens se fait en dehors de l'application. Contactez le bailleur pour convenir d'un rendez-vous. »
- [ ] Si `status = PENDING` : afficher les boutons Approuver / Rejeter (UBAX-FE-304)
- [ ] Si déjà traité : afficher `reviewedByName`, `reviewedAt`, `rejectionReason`

---

### UBAX-FE-304 · Décision sur une demande bailleur (Agence)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/bailleur/agency/applications/{id}/decision` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` + sous-rôle `DIRECTEUR_AGENCE` |
| **Path params** | `id` : UUID de la demande |
| **Content-Type** | `application/json` |

**Request body :**
```json
{
  "decision": "APPROVE",
  "comment": "string (optionnel)"
}
```

> **Valeurs `decision` :** `APPROVE` · `REJECT`

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "Decision processed successfully",
  "data": {
    "id": "uuid",
    "status": "APPROVED",
    "reviewedByName": "Jean Dupont",
    "reviewedAt": "2026-04-30T10:00:00",
    "rejectionReason": null,
    "...": "autres champs BailleurApplicationResponse"
  }
}
```

**Erreurs possibles :**
- `400 Bad Request` — demande déjà traitée (non `PENDING`)
- `404 Not Found` — demande introuvable

**Critères d'acceptation :**
- [ ] Bouton « Approuver » (vert) → appel avec `decision: APPROVE`
- [ ] Bouton « Rejeter » (rouge) → ouvre modal avec champ `comment` obligatoire pour `REJECT`
- [ ] Si `APPROVE` : création automatique d'un compte OWNER (géré côté backend)
- [ ] Confirmation avant action
- [ ] Mise à jour du statut affiché après succès
- [ ] Désactiver les boutons si `status !== PENDING`

---

### UBAX-FE-305 · Vue globale des demandes bailleur (Admin)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/bailleur/admin/applications` |
| **Auth** | Bearer token · Rôle `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |
| **Query params** | `page` · `size` · `sort=createdAt,desc` |

**Response `200` :** _(identique à UBAX-FE-302 — `Page<BailleurApplicationResponse>`)_

**Critères d'acceptation :**
- [ ] Tableau identique à UBAX-FE-302 mais avec toutes les agences visibles
- [ ] Colonne `agencyName` visible
- [ ] Filtres par agence et par statut
- [ ] Accès en lecture seule (pas de bouton Décision ici)

---

### UBAX-FE-306 · Liste des bailleurs de l'agence (pour créer un bien)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/bailleur/agency/bailleurs` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` + sous-rôle `DIRECTEUR_AGENCE` |
| **Query params** | `page` (défaut 0) · `size` (défaut 20) · `sort=joinedAt,desc` |

> Ce endpoint est utilisé dans le formulaire de création de bien (`POST /v1/properties`) pour permettre au Directeur d'agence de sélectionner le bailleur propriétaire réel. L'`id` retourné devient le champ `ownerId` du bien.

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "BAILLEUR_AGENCY_BAILLEURS_SUCCESS",
  "data": {
    "content": [
      {
        "id": "uuid",
        "firstName": "string",
        "lastName": "string",
        "phone": "string",
        "email": "string",
        "avatarUrl": "string",
        "joinedAt": "2026-04-30T10:00:00"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "size": 20,
    "number": 0
  }
}
```

**Critères d'acceptation :**
- [ ] Select / dropdown « Propriétaire » dans le formulaire de création de bien
- [ ] Appel `GET /v1/bailleur/agency/bailleurs` au chargement du formulaire
- [ ] Afficher nom complet + téléphone dans le select pour identifier le bailleur
- [ ] `ownerId` envoyé dans le body de `POST /v1/properties` si un bailleur est sélectionné
- [ ] Champ optionnel — si aucun bailleur sélectionné, le bien appartient à l'agence (créateur)
- [ ] État vide si aucun bailleur approuvé

---

---

## MODULE 4 — ADMINISTRATEUR

**Epic :** `UBAX-FE-ADMIN`  
**Rôle requis (JWT) :** `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` selon l'action

### Objectif

Le **back-office UBAX** est réservé aux équipes internes d'UBAX. Il permet à un Super Admin de gérer les comptes administrateurs, de leur attribuer des sous-rôles fonctionnels internes, et d'avoir une vision transverse sur les équipes des agences et hôtels partenaires.

**Flux principal :**
1. Le Super Admin crée les comptes des collaborateurs internes UBAX (Directeur général, Finance, Opérations, etc.) via le formulaire de création admin.
2. Il leur assigne un ou plusieurs sous-rôles internes (`UBAX_INTERNAL`) qui déterminent leur tableau de bord et leurs accès fonctionnels dans le back-office.
3. Il peut modifier le niveau de rôle Keycloak (`ADMIN` → `SUPER_ADMIN` ou inverse) et supprimer (archiver) un compte.
4. Un Admin (sans prefix SUPER) peut consulter la liste des admins et les sous-rôles, mais ne peut pas écrire — toutes les actions de modification sont réservées au `SUPER_ADMIN`.
5. En lecture, tout admin peut consulter la composition des équipes agence et hôtel depuis les fiches partenaires.

**Deux niveaux de permissions dans ce module :**

| Action | `UBAX_ADMIN` | `UBAX_SUPER_ADMIN` |
|--------|:---:|:---:|
| Voir la liste des admins | ✅ | ✅ |
| Voir les sous-rôles d'un admin | ✅ | ✅ |
| Voir les membres d'une agence / hôtel | ✅ | ✅ |
| Créer un admin | ✗ | ✅ |
| Modifier le rôle d'un admin | ✗ | ✅ |
| Supprimer un admin | ✗ | ✅ |
| Assigner / révoquer des sous-rôles internes | ✗ | ✅ |

**Périmètre frontend de ce module :**

| Tâche | Action | Acteur |
|-------|--------|--------|
| UBAX-FE-401 — Lister les admins | Lecture | `ADMIN` + `SUPER_ADMIN` |
| UBAX-FE-402 — Créer un admin | Écriture | `SUPER_ADMIN` uniquement |
| UBAX-FE-403 — Modifier le rôle d'un admin | Écriture | `SUPER_ADMIN` uniquement |
| UBAX-FE-404 — Supprimer un admin | Écriture | `SUPER_ADMIN` uniquement |
| UBAX-FE-405 — Assigner des sous-rôles internes | Écriture | `SUPER_ADMIN` uniquement |
| UBAX-FE-406 — Consulter les sous-rôles | Lecture | `ADMIN` + `SUPER_ADMIN` |
| UBAX-FE-407 — Révoquer un sous-rôle interne | Écriture | `SUPER_ADMIN` uniquement |
| UBAX-FE-408 — Voir membres d'une agence | Lecture | `ADMIN` + `SUPER_ADMIN` |
| UBAX-FE-409 — Voir membres d'un hôtel | Lecture | `ADMIN` + `SUPER_ADMIN` |

**Points d'attention :**
- Un admin ne peut **pas modifier ou supprimer son propre compte** — désactiver les actions sur la ligne correspondant à l'utilisateur connecté.
- Les sous-rôles internes (`UBAX_INTERNAL`) sont différents des sous-rôles agence/hôtel — utiliser une liste distincte dans les composants de sélection.
- La suppression est un **soft delete** (champ `deletedAt`) — l'entrée disparaît de la liste mais reste en base.
- Les vues membres agence/hôtel (FE-408/409) sont en **lecture seule** depuis le back-office admin — la gestion reste à la charge du Directeur d'agence ou du Gérant hôtel.

---

### UBAX-FE-401 · Lister les administrateurs

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/admin/users` |
| **Auth** | Bearer token · Rôle `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "Admins retrieved",
  "data": [
    {
      "userId": "uuid",
      "keycloakId": "string",
      "email": "string",
      "phone": "string",
      "firstName": "string",
      "lastName": "string",
      "roles": ["ADMIN"]
    }
  ]
}
```

**Critères d'acceptation :**
- [ ] Tableau avec colonnes : nom complet, email, téléphone, rôle (badge), actions
- [ ] Différencier visuellement `ADMIN` et `SUPER_ADMIN`
- [ ] Boutons « Modifier rôle » et « Supprimer » visibles seulement pour `SUPER_ADMIN`

---

### UBAX-FE-402 · Créer un administrateur

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/admin/users` |
| **Auth** | Bearer token · Rôle `UBAX_SUPER_ADMIN` |
| **Content-Type** | `application/json` |

**Request body :**
```json
{
  "firstName": "string (2–100 chars, requis)",
  "lastName": "string (2–100 chars, requis)",
  "email": "string (email valide, requis)",
  "phone": "string (format +XXX, optionnel)",
  "role": "ADMIN"
}
```

> **Valeurs `role` :** `ADMIN` · `SUPER_ADMIN`

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "Admin created successfully",
  "data": {
    "userId": "uuid",
    "keycloakId": "string",
    "email": "string",
    "phone": "string",
    "firstName": "string",
    "lastName": "string",
    "roles": ["ADMIN"]
  }
}
```

**Erreurs possibles :**
- `409 Conflict` — email déjà utilisé
- `400 Bad Request` — champs invalides

**Critères d'acceptation :**
- [ ] Formulaire modal : firstName, lastName, email, phone (optionnel), sélection du rôle
- [ ] Visuel uniquement pour `SUPER_ADMIN`
- [ ] Validation email + longueurs
- [ ] Succès : rafraîchir la liste, afficher toast

---

### UBAX-FE-403 · Modifier le rôle d'un administrateur

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PUT /v1/admin/users/{userId}/role` |
| **Auth** | Bearer token · Rôle `UBAX_SUPER_ADMIN` |
| **Path params** | `userId` : UUID |
| **Content-Type** | `application/json` |

**Request body :**
```json
{
  "role": "SUPER_ADMIN"
}
```

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "Role updated successfully",
  "data": {
    "userId": "uuid",
    "keycloakId": "string",
    "email": "string",
    "phone": "string",
    "firstName": "string",
    "lastName": "string",
    "roles": ["SUPER_ADMIN"]
  }
}
```

**Critères d'acceptation :**
- [ ] Dropdown inline ou modal de confirmation avec sélection `ADMIN` / `SUPER_ADMIN`
- [ ] Empêcher de modifier son propre rôle (désactiver l'action pour `userId == currentUser.id`)
- [ ] Confirmation avant modification
- [ ] Mise à jour du badge rôle en liste après succès

---

### UBAX-FE-404 · Supprimer (archiver) un administrateur

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `DELETE /v1/admin/users/{userId}` |
| **Auth** | Bearer token · Rôle `UBAX_SUPER_ADMIN` |
| **Path params** | `userId` : UUID |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "Admin deleted successfully",
  "data": null
}
```

**Erreurs possibles :**
- `404 Not Found` — utilisateur introuvable
- `400 Bad Request` — tentative de suppression de son propre compte

**Critères d'acceptation :**
- [ ] Bouton « Supprimer » avec dialog de confirmation (nom de l'admin affiché dans le message)
- [ ] Visible uniquement pour `SUPER_ADMIN`
- [ ] Empêcher la suppression de son propre compte
- [ ] Retrait de l'entrée de la liste après succès

---

### UBAX-FE-405 · Assigner des sous-rôles internes à un admin

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/admin/users/{userId}/sub-roles` |
| **Auth** | Bearer token · Rôle `UBAX_SUPER_ADMIN` |
| **Path params** | `userId` : UUID |

**Request body :**
```json
["DIRECTEUR_GENERAL", "FINANCE"]
```

> **Valeurs disponibles (scope UBAX_INTERNAL) :**  
> `DIRECTEUR_GENERAL` · `SUPPORT_CLIENT` · `OPERATIONS` · `FINANCE` · `COMMERCIAL`

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "Sub-roles assigned",
  "data": [
    {
      "id": "uuid",
      "userId": "uuid",
      "role": "DIRECTEUR_GENERAL",
      "scope": "UBAX_INTERNAL",
      "createdAt": "2026-04-30T10:00:00"
    }
  ]
}
```

**Critères d'acceptation :**
- [ ] Modal avec multiselect des sous-rôles UBAX_INTERNAL
- [ ] Pré-sélection des sous-rôles existants (appel GET UBAX-FE-406 avant)
- [ ] Visible uniquement pour `SUPER_ADMIN`

---

### UBAX-FE-406 · Consulter les sous-rôles d'un admin

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/admin/users/{userId}/sub-roles` |
| **Auth** | Bearer token · Rôle `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |
| **Path params** | `userId` : UUID |

**Response `200` :** _(tableau de `UserSubRoleResponse` — voir UBAX-FE-405)_

---

### UBAX-FE-407 · Révoquer un sous-rôle interne

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `DELETE /v1/admin/users/{userId}/sub-roles/{role}` |
| **Auth** | Bearer token · Rôle `UBAX_SUPER_ADMIN` |
| **Path params** | `userId` : UUID · `role` : ex. `FINANCE` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "Sub-role revoked",
  "data": null
}
```

**Critères d'acceptation :**
- [ ] Bouton de révocation par badge dans la modal de gestion des sous-rôles
- [ ] Confirmation avant révocation
- [ ] Mise à jour de l'affichage

---

### UBAX-FE-408 · Voir les membres d'une agence (Admin)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/admin/agencies/{agencyId}/members` |
| **Auth** | Bearer token · Rôle `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |
| **Path params** | `agencyId` : UUID |

**Response `200` :** _(tableau de `UserResponse` — voir UBAX-FE-101)_

**Critères d'acceptation :**
- [ ] Accessible depuis la fiche agence dans le back-office admin
- [ ] Tableau en lecture seule (le DIRECTEUR_AGENCE gère son équipe lui-même)
- [ ] Afficher les sous-rôles de chaque membre

---

### UBAX-FE-409 · Voir les membres d'un hôtel (Admin)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/admin/hotels/{hotelId}/members` |
| **Auth** | Bearer token · Rôle `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |
| **Path params** | `hotelId` : UUID |

**Response `200` :** _(tableau de `UserResponse` — voir UBAX-FE-101)_

**Critères d'acceptation :**
- [ ] Accessible depuis la fiche hôtel dans le back-office admin
- [ ] Tableau en lecture seule

---

---

## MODULE 5 — PARTNER (Candidatures partenaires)

**Epic :** `UBAX-FE-PARTNER`  
**Rôle requis (JWT) :** Aucun pour la soumission · `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` pour la gestion

### Objectif

Le module **Partner** gère le processus de candidature des agences immobilières et hôtels qui souhaitent rejoindre la plateforme UBAX en tant que partenaires. C'est la porte d'entrée pour tout acteur professionnel désirant utiliser l'écosystème UBAX.

**Flux principal :**
1. Le futur partenaire (agence ou hôtel) remplit un formulaire public avec ses informations légales et joint ses documents obligatoires (RCCM, DFE, bail, logo).
2. La candidature arrive avec le statut `PENDING` dans le back-office UBAX.
3. Un Admin UBAX passe la demande en `UNDER_REVIEW` pour signaler qu'elle est en cours d'examen.
4. L'Admin peut demander des compléments d'information (`INCOMPLETE`) — le partenaire doit alors resoumettre.
5. L'Admin approuve (`APPROVED`) → le backend crée automatiquement le compte Keycloak `UBAX_PARTNER` et l'entité agence/hôtel, ou rejette définitivement (`REJECTED`).
6. Chaque changement de statut est tracé dans un historique consultable.

**Cycle de vie des statuts :**

```
PENDING → UNDER_REVIEW → APPROVED
                       → REJECTED
                       → INCOMPLETE → (nouvelle soumission) → PENDING
```

**Périmètre frontend de ce module :**

| Tâche | Action | Acteur |
|-------|--------|--------|
| UBAX-FE-501 — Formulaire de candidature | Écriture | Grand public (sans compte) |
| UBAX-FE-502 — Liste des candidatures (Admin) | Lecture | `ADMIN` + `SUPER_ADMIN` |
| UBAX-FE-503 — Détail d'une candidature (Admin) | Lecture | `ADMIN` + `SUPER_ADMIN` |
| UBAX-FE-504 — Décision sur une candidature | Écriture | `ADMIN` + `SUPER_ADMIN` |

**Points d'attention :**
- La soumission est en **multipart/form-data** (pas JSON) — le champ `data` contient le JSON métier, les autres champs sont des fichiers binaires.
- Les fichiers acceptés (`rccm`, `dfe`, `bail`, `logo`) doivent avoir des contraintes de type et taille côté frontend avant upload.
- Le commentaire est **obligatoire** pour les décisions `REJECTED` et `INCOMPLETE`, optionnel pour `UNDER_REVIEW` et `APPROVED`.
- L'historique des statuts (`statusHistory`) n'est retourné que sur l'endpoint de détail — ne pas l'attendre dans la liste paginée.

---

### UBAX-FE-501 · Formulaire de candidature partenaire (Public)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/partner/apply` |
| **Auth** | **Aucune** (public) |
| **Content-Type** | `multipart/form-data` |

**Champs du formulaire :**

| Nom du champ | Type | Obligatoire | Contraintes |
|-------------|------|:-----------:|-------------|
| `data` (part JSON) | String (JSON sérialisé) | ✅ | Voir structure ci-dessous |
| `rccm` | File | ✅ | Document RCCM |
| `dfe` | File | ✅ | Document DFE |
| `bail` | File | ✅ | Bail ou titre d'occupation |
| `logo` | File | ✅ | Logo de la structure |

**Structure du champ `data` (JSON) :**
```json
{
  "partnerType": "AGENCE",
  "companyName": "string (max 200, requis)",
  "legalRepresentative": "string (max 200, requis)",
  "phone": "string (format +XXX, requis)",
  "email": "string (email valide, requis)",
  "country": "string (code ISO 2–5 chars, requis)",
  "city": "string (max 100, requis)",
  "postalAddress": "string (max 500, optionnel)",
  "zone": "string (max 150, optionnel)",
  "description": "string (optionnel)",
  "latitude": 3.8667,
  "longitude": 11.5167,
  "legalStatus": "string (max 100, optionnel)",
  "registrationNumber": "string (max 100, optionnel)"
}
```

> **Valeurs `partnerType` :** `AGENCE` · `HOTEL`

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "Application submitted successfully",
  "data": {
    "id": "uuid",
    "partnerType": "AGENCE",
    "companyName": "string",
    "legalRepresentative": "string",
    "phone": "string",
    "email": "string",
    "country": "CM",
    "city": "Yaoundé",
    "postalAddress": "string",
    "zone": "string",
    "latitude": 3.8667,
    "longitude": 11.5167,
    "description": "string",
    "legalStatus": "string",
    "registrationNumber": "string",
    "rccmUrl": "https://minio/.../rccm.pdf",
    "dfeUrl": "https://minio/.../dfe.pdf",
    "bailUrl": "https://minio/.../bail.pdf",
    "logoUrl": "https://minio/.../logo.png",
    "status": "PENDING",
    "submittedAt": "2026-04-30T10:00:00",
    "reviewedByName": null,
    "reviewedAt": null,
    "rejectionReason": null,
    "createdAt": "2026-04-30T10:00:00",
    "updatedAt": "2026-04-30T10:00:00",
    "statusHistory": null
  }
}
```

**Critères d'acceptation :**
- [ ] Formulaire multi-étapes : Étape 1 (type de partenaire), Étape 2 (infos entreprise), Étape 3 (localisation + coordonnées GPS optionnelles), Étape 4 (upload documents)
- [ ] Sélection du `partnerType` (`AGENCE` / `HOTEL`) en première étape — adapter les libellés selon le type
- [ ] Upload de 4 fichiers distincts avec aperçu du nom et bouton de suppression
- [ ] Sérialiser le champ `data` en JSON avant d'assembler le `FormData`
- [ ] Validation complète avant soumission (email, téléphone, longueurs)
- [ ] Page de confirmation avec numéro de dossier (`id`) après soumission réussie
- [ ] Message clair indiquant que la demande sera traitée sous X jours ouvrés

---

### UBAX-FE-502 · Liste des candidatures partenaires (Admin)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/partner/admin/applications` |
| **Auth** | Bearer token · Rôle `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |
| **Query params** | `status` (optionnel) · `page` (défaut 0) · `size` (défaut 20) · `sort=submittedAt,desc` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "Applications retrieved",
  "data": {
    "content": [
      {
        "id": "uuid",
        "partnerType": "AGENCE",
        "companyName": "Agence Immobilière Alpha",
        "legalRepresentative": "Jean Dupont",
        "phone": "+237612345678",
        "email": "contact@alpha.cm",
        "country": "CM",
        "city": "Douala",
        "postalAddress": "string",
        "zone": "Akwa",
        "latitude": 4.0511,
        "longitude": 9.7679,
        "description": "string",
        "legalStatus": "SARL",
        "registrationNumber": "RC/DLA/2020/B/1234",
        "rccmUrl": "string",
        "dfeUrl": "string",
        "bailUrl": "string",
        "logoUrl": "string",
        "status": "PENDING",
        "submittedAt": "2026-04-30T10:00:00",
        "reviewedByName": null,
        "reviewedAt": null,
        "rejectionReason": null,
        "createdAt": "2026-04-30T10:00:00",
        "updatedAt": "2026-04-30T10:00:00",
        "statusHistory": null
      }
    ],
    "totalElements": 35,
    "totalPages": 2,
    "size": 20,
    "number": 0
  }
}
```

**Critères d'acceptation :**
- [ ] Tableau paginé avec colonnes : nom entreprise, type (`AGENCE`/`HOTEL`), représentant légal, email, ville, statut (badge coloré), date soumission, actions
- [ ] Filtre par statut (`PENDING`, `UNDER_REVIEW`, `INCOMPLETE`, `APPROVED`, `REJECTED`)
- [ ] Badge `partnerType` distinctif (`AGENCE` bleu / `HOTEL` violet)
- [ ] Lien vers le détail (UBAX-FE-503)
- [ ] Compteur par statut en en-tête de page (ex : 12 PENDING, 3 UNDER_REVIEW…)

---

### UBAX-FE-503 · Détail d'une candidature partenaire (Admin)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/partner/admin/applications/{id}` |
| **Auth** | Bearer token · Rôle `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |
| **Path params** | `id` : UUID de la candidature |

**Response `200` :** _(objet `PartnerApplicationResponse` complet avec `statusHistory`)_
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "Application retrieved",
  "data": {
    "...": "tous les champs de UBAX-FE-502",
    "statusHistory": [
      {
        "id": "uuid",
        "previousStatus": null,
        "newStatus": "PENDING",
        "changedByName": null,
        "comment": null,
        "changedAt": "2026-04-30T10:00:00"
      },
      {
        "id": "uuid",
        "previousStatus": "PENDING",
        "newStatus": "UNDER_REVIEW",
        "changedByName": "Admin Martin",
        "comment": "Dossier pris en charge",
        "changedAt": "2026-04-30T11:30:00"
      }
    ]
  }
}
```

**Critères d'acceptation :**
- [ ] Section informations entreprise : nom, type, représentant, email, téléphone, ville, pays, zone, adresse postale, coordonnées GPS
- [ ] Section documents : liens cliquables vers RCCM, DFE, bail, logo (ouverture dans un nouvel onglet ou prévisualisation modale)
- [ ] Section statut actuel avec badge + date de soumission
- [ ] Timeline de l'historique des statuts (`statusHistory`) avec auteur, date et commentaire pour chaque transition
- [ ] Si `status` actionnable (`PENDING`, `UNDER_REVIEW`, `INCOMPLETE`) : afficher les boutons de décision (UBAX-FE-504)
- [ ] Si `status = APPROVED` ou `REJECTED` : afficher `reviewedByName`, `reviewedAt`, `rejectionReason`

---

### UBAX-FE-504 · Décision sur une candidature partenaire (Admin)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/partner/admin/applications/{id}/decision` |
| **Auth** | Bearer token · Rôle `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |
| **Path params** | `id` : UUID de la candidature |
| **Content-Type** | `application/json` |

**Request body :**
```json
{
  "newStatus": "APPROVED",
  "comment": "string (obligatoire pour REJECTED et INCOMPLETE, optionnel sinon)"
}
```

> **Valeurs `newStatus` autorisées :** `UNDER_REVIEW` · `APPROVED` · `REJECTED` · `INCOMPLETE`

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "Decision applied successfully",
  "data": {
    "id": "uuid",
    "status": "APPROVED",
    "reviewedByName": "Admin Martin",
    "reviewedAt": "2026-04-30T12:00:00",
    "rejectionReason": null,
    "statusHistory": [ { "...": "historique mis à jour" } ],
    "...": "autres champs PartnerApplicationResponse"
  }
}
```

**Erreurs possibles :**
- `400 Bad Request` — transition de statut invalide ou commentaire manquant pour `REJECTED`/`INCOMPLETE`
- `404 Not Found` — candidature introuvable

**Actions disponibles selon le statut courant :**

| Statut courant | Actions possibles |
|----------------|-------------------|
| `PENDING` | → `UNDER_REVIEW` · → `REJECTED` |
| `UNDER_REVIEW` | → `APPROVED` · → `REJECTED` · → `INCOMPLETE` |
| `INCOMPLETE` | → `UNDER_REVIEW` · → `REJECTED` |
| `APPROVED` | _(aucune action)_ |
| `REJECTED` | _(aucune action)_ |

**Critères d'acceptation :**
- [ ] Bouton « Prendre en charge » → `newStatus: UNDER_REVIEW` (depuis `PENDING`)
- [ ] Bouton « Approuver » (vert) → `newStatus: APPROVED` avec confirmation
- [ ] Bouton « Demander des compléments » (orange) → `newStatus: INCOMPLETE` + modal avec champ `comment` obligatoire
- [ ] Bouton « Rejeter » (rouge) → `newStatus: REJECTED` + modal avec champ `comment` obligatoire
- [ ] Afficher uniquement les actions valides selon le statut courant (tableau ci-dessus)
- [ ] Si `APPROVED` : afficher message informatif « Un compte partenaire a été créé automatiquement »
- [ ] Mise à jour de la timeline `statusHistory` après chaque décision

---

## RÉFÉRENCES TRANSVERSES

### Tableau des rôles Keycloak

| Valeur JWT | Libellé UI | Couleur badge suggérée |
|-----------|------------|------------------------|
| `UBAX_SUPER_ADMIN` | Super Admin | Rouge `#DC2626` |
| `UBAX_ADMIN` | Admin | Orange `#EA580C` |
| `UBAX_PARTNER` | Partenaire | Bleu `#2563EB` |
| `UBAX_OWNER` | Propriétaire | Violet `#7C3AED` |
| `UBAX_CLIENT` | Client | Vert `#16A34A` |

### Tableau des sous-rôles

| Scope | Sous-rôle | Libellé UI |
|-------|-----------|------------|
| `AGENCE` | `DIRECTEUR_AGENCE` | Directeur d'agence |
| `AGENCE` | `COMMERCIAL` | Commercial |
| `AGENCE` | `COMPTABLE_AGENCE` | Comptable |
| `AGENCE` | `AGENT_SAV` | Agent SAV |
| `HOTEL` | `GERANT_HOTEL` | Gérant hôtel |
| `HOTEL` | `RECEPTIONNISTE` | Réceptionniste |
| `HOTEL` | `COMPTABLE_HOTEL` | Comptable hôtel |
| `HOTEL` | `RESPONSABLE_HEBERGEMENT` | Resp. hébergement |
| `UBAX_INTERNAL` | `DIRECTEUR_GENERAL` | Directeur général |
| `UBAX_INTERNAL` | `SUPPORT_CLIENT` | Support client |
| `UBAX_INTERNAL` | `OPERATIONS` | Opérations |
| `UBAX_INTERNAL` | `FINANCE` | Finance |
| `UBAX_INTERNAL` | `COMMERCIAL` | Commercial |

### Statuts candidature partenaire (`ApplicationStatus`)

| Valeur | Libellé | Couleur |
|--------|---------|---------|
| `PENDING` | En attente | Jaune `#EAB308` |
| `UNDER_REVIEW` | En cours d'examen | Bleu `#3B82F6` |
| `INCOMPLETE` | Dossier incomplet | Orange `#F97316` |
| `APPROVED` | Approuvé | Vert `#16A34A` |
| `REJECTED` | Rejeté | Rouge `#DC2626` |

### Statuts bailleur (`BailleurApplicationStatus`)

| Valeur | Libellé | Couleur |
|--------|---------|---------|
| `PENDING` | En attente | Jaune `#EAB308` |
| `APPROVED` | Approuvé | Vert `#16A34A` |
| `REJECTED` | Rejeté | Rouge `#DC2626` |
| `CANCELLED` | Annulé | Gris `#6B7280` |

### Gestion des erreurs HTTP (standard)

| Code | Signification | Message UI |
|------|---------------|------------|
| `400` | Données invalides | Afficher les erreurs de champ |
| `401` | Non authentifié | Rediriger vers login |
| `403` | Accès refusé | Toast « Accès non autorisé » |
| `404` | Ressource introuvable | Message vide ou page 404 |
| `409` | Conflit (ex: email existant) | Toast avec message backend |
| `500` | Erreur serveur | Toast « Erreur serveur, réessayez » |
