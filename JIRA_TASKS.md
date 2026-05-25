SUR # UBAX Platform – Tâches Frontend (Jira) · Biens & Storage

> **Scope :** Portail Agence · Portail Hôtel · Storage  
> **Base URL :** `http://localhost:9999/api`  
> **Auth :** `Authorization: Bearer <access_token>` (sauf routes marquées Public)  
> **Réponse standard :**
> ```json
> { "status": "SUCCESS", "statusCode": 200, "message": "...", "data": { ... } }
> ```
> **Pagination :** `data` contient `{ "content": [...], "totalElements": N, "totalPages": N, "size": N, "number": N }`

---

## MODULE 6 — BIENS IMMOBILIERS · PORTAIL AGENCE

**Epic :** `UBAX-FE-BIENS-AGENCE`  
**Rôle requis (JWT) :** `UBAX_PARTNER` · Sous-rôles DB : `DIRECTEUR_AGENCE` ou `COMMERCIAL` pour les actions d'écriture

### Objectif

Une **agence partenaire** UBAX peut publier, gérer et modérer ses annonces immobilières depuis son espace partenaire. Ce module couvre l'intégralité du cycle de vie d'un bien : création en brouillon, enrichissement (médias, documents), soumission pour modération, puis publication.

**Flux principal :**
1. Un membre de l'agence (`COMMERCIAL` ou `DIRECTEUR_AGENCE`) crée un bien en statut `DRAFT` et renseigne ses caractéristiques.
2. Il joint des photos via upload direct (mobile) ou presigned URL (web).
3. Il attache les documents légaux nécessaires à la modération.
4. Il soumet le bien (`DRAFT` → `PENDING`) pour qu'un admin UBAX l'examine.
5. L'admin publie (`PUBLISHED`) ou rejette (`REJECTED`) avec un motif.
6. En cas de rejet, le membre corrige et re-soumet.

**Périmètre frontend de ce module :**

| Tâche | Action | Acteur |
|-------|--------|--------|
| UBAX-FE-601 — Catalogue public | Lecture | Grand public (sans compte) |
| UBAX-FE-602 — Détail d'un bien | Lecture | Grand public (sans compte) |
| UBAX-FE-603 — Mes biens | Lecture | Tout membre `PARTNER` |
| UBAX-FE-604 — Créer un bien | Écriture | `COMMERCIAL` · `DIRECTEUR_AGENCE` |
| UBAX-FE-605 — Modifier un bien | Écriture | `COMMERCIAL` · `DIRECTEUR_AGENCE` |
| UBAX-FE-606 — Soumettre à la modération | Écriture | `COMMERCIAL` · `DIRECTEUR_AGENCE` |
| UBAX-FE-607 — Uploader des médias | Écriture | `COMMERCIAL` · `DIRECTEUR_AGENCE` |
| UBAX-FE-608 — Définir la photo de couverture | Écriture | `COMMERCIAL` · `DIRECTEUR_AGENCE` |
| UBAX-FE-609 — Supprimer un média | Écriture | `COMMERCIAL` · `DIRECTEUR_AGENCE` |
| UBAX-FE-610 — Gérer les documents légaux | Écriture | `DIRECTEUR_AGENCE` |
| UBAX-FE-611 — Archiver un bien | Écriture | `DIRECTEUR_AGENCE` |
| UBAX-FE-612 — Détail d'un bien (espace partenaire) | Lecture + actions contextuelles | Tout membre `PARTNER` |

**Points d'attention :**
- Le statut `PUBLISHED` est **exclusivement** accordé par un admin UBAX via le back-office — le bouton "Publier" n'existe pas côté partenaire, seulement "Soumettre".
- Les médias du bucket `properties-media` sont **publics** — utiliser directement `fileUrl` dans `<img src>` sans token.
- Les documents du bucket `property-documents` sont **privés** — appeler `GET /v1/storage/presign/read?fileUrl=...` avant d'afficher ou télécharger.
- La soumission (`/submit`) n'est possible que depuis le statut `DRAFT` — désactiver le bouton pour tout autre statut.
- Un bien rejeté (`REJECTED`) peut être modifié et re-soumis — garder les champs éditables.

---

### UBAX-FE-601 · Catalogue public des biens

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/properties` |
| **Auth** | **Aucune** (public) |
| **Query params** | Voir tableau ci-dessous |

**Query params (tous optionnels) :**

| Paramètre | Type | Description | Exemple |
|-----------|------|-------------|---------|
| `status` | `string` | Défaut `PUBLISHED` | `PUBLISHED` |
| `city` | `string` | Code ville (code list `CITY`) | `DAKAR` |
| `propertyType` | `string` | Type de bien | `VILLA` |
| `transactionType` | `string` | Type de transaction | `RENT` |
| `minPrice` | `number` | Prix minimum | `100000` |
| `maxPrice` | `number` | Prix maximum | `5000000` |
| `agencyId` | `UUID` | Filtrer par agence | `uuid` |
| `ownerId` | `UUID` | Filtrer par propriétaire | `uuid` |
| `page` | `int` | Numéro de page (défaut `0`) | `0` |
| `size` | `int` | Taille de page (défaut `20`) | `20` |
| `sort` | `string` | Tri | `publishedAt,desc` |

**Tri par défaut :** annonces boostées en tête (`boosted DESC`), puis par `publishedAt DESC`.

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "PROPERTY_GET_LIST_SUCCESS",
  "data": {
    "content": [
      {
        "id": "uuid",
        "ownerId": "uuid",
        "ownerName": "Mamadou Diallo",
        "agencyId": "uuid",
        "agencyName": "Immo Dakar",
        "title": "Villa F5 Almadies",
        "description": "Belle villa avec vue mer...",
        "propertyType": "VILLA",
        "transactionType": "SALE",
        "price": 85000000,
        "condition": "GOOD",
        "yearBuilt": 2018,
        "surfaceTotal": 450.0,
        "surfaceLiving": 320.0,
        "rooms": 7,
        "bedrooms": 5,
        "bathrooms": 3,
        "balconies": 2,
        "floor": null,
        "totalFloors": 2,
        "address": "Rue des Almadies, Dakar",
        "city": "DAKAR",
        "district": "Almadies",
        "street": "Rue 10",
        "latitude": 14.7495,
        "longitude": -17.4942,
        "amenities": [
          { "id": "uuid", "code": "POOL", "customValue": null, "customDescription": null },
          { "id": "uuid", "code": "GENERATOR", "customValue": null, "customDescription": null },
          { "id": "uuid", "code": "WATER_TANK", "customValue": null, "customDescription": null },
          { "id": "uuid", "code": "AC", "customValue": null, "customDescription": null },
          { "id": "uuid", "code": "SECURITY", "customValue": null, "customDescription": null },
          { "id": "uuid", "code": "PARKING", "customValue": null, "customDescription": null },
          { "id": "uuid", "code": "GARDEN", "customValue": null, "customDescription": null }
        ],
        "boosted": true,
        "boostExpiresAt": "2026-06-01T00:00:00",
        "status": "PUBLISHED",
        "rejectionReason": null,
        "publishedAt": "2026-04-10T09:00:00",
        "createdAt": "2026-04-05T14:30:00",
        "updatedAt": "2026-04-10T09:00:00"
      }
    ],
    "totalElements": 128,
    "totalPages": 7,
    "size": 20,
    "number": 0
  }
}
```

**Critères d'acceptation :**
- [ ] Grille de cartes paginée avec photo de couverture, titre, prix, ville, type
- [ ] Filtres : ville (select depuis code list `CITY`), type de bien, type de transaction, fourchette de prix
- [ ] Badge ⚡ visible si `boosted = true`
- [ ] Badge type de transaction (`SALE` / `RENT` / `RENT_FURNISHED`)
- [ ] Loader squelette pendant la requête
- [ ] État vide si aucun résultat
- [ ] La `coverUrl` du média couverture est directement utilisable en `<img src>` (bucket public)

---

### UBAX-FE-602 · Détail d'un bien (public)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/properties/{id}` |
| **Auth** | **Aucune** (public) |
| **Path params** | `id` : UUID du bien |

**Response `200` :** _(objet `PropertyDetailResponse` = `PropertyResponse` + `medias` + `documents`)_
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "PROPERTY_GET_SUCCESS",
  "data": {
    "...": "tous les champs PropertyResponse (voir UBAX-FE-601)",
    "medias": [
      {
        "id": "uuid",
        "propertyId": "uuid",
        "fileUrl": "http://localhost:9000/properties-media/uuid/photo1.jpg",
        "mediaType": "PHOTO",
        "cover": true,
        "createdAt": "2026-04-06T10:00:00"
      }
    ],
    "documents": [
      {
        "id": "uuid",
        "propertyId": "uuid",
        "fileUrl": "http://localhost:9000/property-documents/uuid/titre.pdf",
        "documentType": "TITRE_FONCIER",
        "label": "Titre foncier N°12345",
        "verified": true,
        "verifiedAt": "2026-04-08T14:00:00",
        "createdAt": "2026-04-06T11:00:00"
      }
    ]
  }
}
```

**Erreurs possibles :**
- `404 Not Found` — bien introuvable

**Critères d'acceptation :**
- [ ] Galerie photo/vidéo (slider) avec la couverture en premier
- [ ] Fiche complète : titre, prix, ville, quartier, surface, équipements (chips booléens)
- [ ] Carte interactive si `latitude` / `longitude` présents
- [ ] Section documents : les `fileUrl` des documents sont **privés** — appeler `GET /v1/storage/presign/read` avant affichage (uniquement pour utilisateurs authentifiés)
- [ ] Badge « Agence certifiée » si `agencyId` présent
- [ ] Bouton « Contacter » visible pour les visiteurs

---

### UBAX-FE-603 · Mes biens (espace partenaire)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/properties/mine` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Query params** | `status` (optionnel) · `page` (défaut `0`) · `size` (défaut `20`) · `sort=createdAt,desc` |

**Response `200` :** _(identique à UBAX-FE-601, tous statuts inclus)_

**Critères d'acceptation :**
- [ ] Tableau ou grille avec colonnes : titre, ville, type, prix, statut (badge coloré), date création, actions
- [ ] Filtre par statut (`DRAFT` / `PENDING` / `PUBLISHED` / `REJECTED` / `ARCHIVED`)
- [ ] Bouton « Modifier » pour statuts `DRAFT` et `REJECTED`
- [ ] Bouton « Soumettre » visible uniquement si `status = DRAFT`
- [ ] Bouton « Archiver » pour les biens `PUBLISHED` ou `DRAFT`
- [ ] Motif de rejet (`rejectionReason`) affiché en tooltip ou bandeau si `status = REJECTED`
- [ ] Badge ⚡ si `boosted = true`

---

### UBAX-FE-604 · Créer un bien (brouillon)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/properties` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `DIRECTEUR_AGENCE` · `COMMERCIAL` |
| **Content-Type** | `application/json` |

**Champs obligatoires :** `title`, `propertyType`, `transactionType`, `price` (≥ 0), `city`

**`propertyType` — Biens agence :**

| Valeur | Libellé |
|--------|---------|
| `APARTMENT` | Appartement |
| `VILLA` | Villa |
| `HOUSE` | Maison |
| `LAND` | Terrain |
| `OFFICE` | Bureau |
| `WAREHOUSE` | Entrepôt |
| `STORE` | Boutique |

**`propertyType` — Espaces hôteliers (`transactionType` = `SHORT_STAY` obligatoire) :**

| Valeur | Libellé |
|--------|---------|
| `HOTEL_ROOM` | Chambre d'hôtel |
| `HOTEL_SUITE` | Suite hôtelière |
| `HOTEL_STUDIO` | Studio / chambre avec kitchenette |
| `EVENT_SPACE` | Salle événementielle |
| `CONFERENCE_ROOM` | Salle de conférence |
| `RESTAURANT_SPACE` | Espace restaurant / bar |

> ⚠️ **Cohérence** : types hôteliers → `SHORT_STAY` **uniquement** ; types agence → `SHORT_STAY` **interdit**. Le backend retourne `400` sinon.  
> Valeurs récupérables dynamiquement via `GET /v1/code-list/type/PROPERTY_TYPE`

**`transactionType` :**

| Valeur | Contexte | `price` représente |
|--------|----------|--------------------|
| `SALE` | Vente (agence) | Prix de cession |
| `RENT` | Location vide mensuelle (agence) | Loyer mensuel |
| `RENT_FURNISHED` | Location meublée mensuelle (agence) | Loyer mensuel |
| `SHORT_STAY` | Courte durée / nuitée (hôtel) | Tarif par nuit |

**Request body — bien agence (villa à vendre) :**
```json
{
  "title": "Villa F5 aux Almadies",
  "description": "Belle villa avec piscine et jardin",
  "propertyType": "VILLA",
  "transactionType": "SALE",
  "price": 85000000,
  "condition": "GOOD",
  "yearBuilt": 2018,
  "surfaceTotal": 450.0,
  "surfaceLiving": 320.0,
  "rooms": 7,
  "bedrooms": 5,
  "bathrooms": 3,
  "balconies": 2,
  "totalFloors": 2,
  "city": "Dakar",
  "district": "Almadies",
  "street": "Rue des Almadies",
  "latitude": 14.7495,
  "longitude": -17.4942,
  "amenities": [
    { "code": "POOL",     "description": "Piscine (privée ou commune)" },
    { "code": "PARKING",  "description": "Parking privatif ou en commun" },
    { "code": "SECURITY", "description": "Gardiennage / sécurité sur site" },
    { "code": "GARDEN",   "description": "Jardin ou espace vert privatif" }
  ],
  "ownerId": null
}
```

**Request body — bien agence (appartement à louer) :**
```json
{
  "title": "Appartement F3 Plateau",
  "propertyType": "APARTMENT",
  "transactionType": "RENT",
  "price": 350000,
  "rooms": 3,
  "bedrooms": 2,
  "bathrooms": 1,
  "city": "Dakar",
  "district": "Plateau",
  "amenities": [
    { "code": "AC",       "description": "Climatisation" },
    { "code": "FURNISHED","description": "Bien livré meublé" }
  ]
}
```

**Request body — espace hôtelier (chambre SHORT_STAY) :**
```json
{
  "title": "Chambre Deluxe Vue Mer",
  "propertyType": "HOTEL_ROOM",
  "transactionType": "SHORT_STAY",
  "price": 45000,
  "city": "DAKAR",
  "district": "Almadies",
  "bedrooms": 1,
  "bathrooms": 1,
  "surfaceLiving": 28.0,
  "paymentFrequency": "NIGHTLY",
  "maxOccupancy": 2,
  "bedType": "KING",
  "mealPlan": "BREAKFAST"
}
```

> **`amenities`** : charger via `GET /v1/code-list/type/PROPERTY_AMENITY` (retourne `value` + `description`). Envoyer **les deux champs** dans le body pour éviter un appel DB supplémentaire côté backend — commodité standard : `{ "code": "POOL", "description": "Piscine (privée ou commune)" }`. Commodité personnalisée : `{ "customValue": "Jacuzzi", "customDescription": "Jacuzzi extérieur", "description": "Jacuzzi extérieur" }`  
> **`ownerId`** : UUID du bailleur si l'agence gère pour un tiers — `null` si l'appelant est lui-même propriétaire  
> **Champs hôteliers (requis pour `SHORT_STAY`) :** `paymentFrequency` (`NIGHTLY`·`WEEKLY`·`MONTHLY`) · `maxOccupancy` (≥ 1 personne). `bedType` (`SINGLE`·`DOUBLE`·`TWIN`·`KING`·`QUEEN`·`BUNK`) et `mealPlan` (`ROOM_ONLY`·`BREAKFAST`·`HALF_BOARD`·`FULL_BOARD`·`ALL_INCLUSIVE`) sont recommandés.  
> **Champs hôtel ignorés pour biens agence** — envoyer `null` ou omettre.

**Response `201` :** objet `PropertyResponse` avec `status: "DRAFT"`

**Erreurs possibles :**
- `400 Bad Request` — champ obligatoire manquant (`title`, `propertyType`, `transactionType`, `price`, `city`) ou `price` < 0
- `401 Unauthorized` — token absent
- `403 Forbidden` — rôle insuffisant

**Critères d'acceptation :**
- [ ] Formulaire multi-étapes : Étape 1 (infos générales + type), Étape 2 (surfaces & pièces), Étape 3 (localisation), Étape 4 (équipements)
- [ ] `propertyType` chargé depuis `GET /v1/code-list/type/PROPERTY_TYPE` — filtrer les types agence vs hôtel selon le contexte
- [ ] `transactionType` pré-sélectionné à `SHORT_STAY` et non modifiable pour les espaces hôteliers
- [ ] `city` chargée depuis `GET /v1/code-list/type/CITY`
- [ ] Carte interactive optionnelle pour saisir `latitude` / `longitude`
- [ ] Validation frontend avant envoi (champs requis, `price` ≥ 0)
- [ ] Après création : rediriger vers la fiche du bien en mode édition (ou vers l'upload de médias)
- [ ] Conserver les données saisies en cas d'erreur API

---

### UBAX-FE-605 · Modifier un bien

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PUT /v1/properties/{id}` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `DIRECTEUR_AGENCE` · `COMMERCIAL` |
| **Path params** | `id` : UUID du bien |
| **Content-Type** | `application/json` |

**Request body :** _(identique à UBAX-FE-604 — seuls les champs non-null sont modifiés)_

> ⚠️ Modification autorisée uniquement en statut `DRAFT` ou `REJECTED`. Désactiver le formulaire pour tout autre statut.

**Response `200` :** _(objet `PropertyResponse` mis à jour)_

**Erreurs possibles :**
- `400 Bad Request` — bien dans un statut non modifiable
- `403 Forbidden` — bien appartenant à une autre agence
- `404 Not Found` — bien introuvable

**Critères d'acceptation :**
- [ ] Formulaire pré-rempli avec les valeurs actuelles du bien
- [ ] Même structure multi-étapes que la création
- [ ] Bandeau d'information si `status = REJECTED` avec le motif de rejet visible
- [ ] Toast de succès après sauvegarde
- [ ] Désactiver et griser le formulaire si `status = PUBLISHED` ou `ARCHIVED`

---

### UBAX-FE-606 · Soumettre un bien à la modération

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/properties/{id}/submit` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `DIRECTEUR_AGENCE` · `COMMERCIAL` |
| **Path params** | `id` : UUID du bien |
| **Request body** | _(aucun)_ |

> ⚠️ Le bien doit être en statut `DRAFT`. La soumission passe le statut à `PENDING`.

**Response `200` :** _(objet `PropertyResponse` avec `status: "PENDING"`)_

**Erreurs possibles :**
- `400 Bad Request` — bien non en statut `DRAFT`
- `403 Forbidden` — bien appartenant à une autre agence
- `404 Not Found` — bien introuvable

**Critères d'acceptation :**
- [ ] Bouton « Soumettre pour modération » visible uniquement si `status = DRAFT`
- [ ] Dialog de confirmation avant soumission (« Une fois soumis, le bien ne pourra plus être modifié jusqu'à la décision de l'administrateur »)
- [ ] Après succès : afficher badge `PENDING` et désactiver les boutons d'édition
- [ ] Message informatif : « Votre annonce est en cours d'examen. Vous serez notifié de la décision. »

---

### UBAX-FE-607 · Uploader des médias (photos / vidéos)

| Champ | Valeur |
|-------|--------|
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `DIRECTEUR_AGENCE` · `COMMERCIAL` |
| **Bucket** | `properties-media` (public — `fileUrl` utilisable directement en `<img src>`) |

#### Stratégie A — Upload direct multipart (recommandé mobile / fichiers ≤ 50 Mo)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/properties/{id}/media/upload` |
| **Content-Type** | `multipart/form-data` |
| **Path params** | `id` : UUID du bien |

**Champs du formulaire :**

| Nom | Type | Requis | Description |
|-----|------|:------:|-------------|
| `file` | `File` | ✅ | Image ou vidéo |
| `mediaType` | `string` | ✅ | `PHOTO` · `VIDEO` · `PLAN` |
| `cover` | `boolean` | — | `true` pour définir comme couverture (défaut `false`) |

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "PROPERTY_MEDIA_ADD_SUCCESS",
  "data": {
    "id": "uuid",
    "propertyId": "uuid",
    "fileUrl": "http://localhost:9000/properties-media/propertyId/uuid.jpg",
    "mediaType": "PHOTO",
    "cover": false,
    "createdAt": "2026-05-06T10:00:00"
  }
}
```

#### Stratégie B — Presigned URL PUT (recommandé web / fichiers volumineux)

**Étape 1 — Obtenir l'URL d'upload :**

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/storage/presign/property-media` |
| **Query params** | `propertyId` (UUID, requis) · `contentType` (MIME, requis) · `expires` (défaut `900`) |

**Response `200` :**
```json
{
  "data": {
    "uploadUrl": "http://localhost:9000/properties-media/...?X-Amz-Signature=...",
    "publicUrl": "http://localhost:9000/properties-media/propertyId/uuid.jpg",
    "objectName": "propertyId/uuid.jpg",
    "bucket": "properties-media",
    "expiresInSeconds": 900
  }
}
```

**Étape 2 — Upload direct vers MinIO (côté navigateur) :**
```
PUT {uploadUrl}
Content-Type: image/jpeg
Body: <binaire>
```

**Étape 3 — Lier le média au bien :**

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/properties/{id}/media` |
| **Content-Type** | `application/json` |

```json
{
  "fileUrl": "http://localhost:9000/properties-media/propertyId/uuid.jpg",
  "mediaType": "PHOTO",
  "cover": false
}
```

**Formats acceptés :**

| Type | MIME | Taille max |
|------|------|-----------|
| Image | `image/jpeg` · `image/png` · `image/webp` | 10 Mo |
| Vidéo | `video/mp4` · `video/quicktime` · `video/mpeg` | 100 Mo |

**Erreurs possibles :**
- `400 Bad Request` — type MIME non autorisé ou taille dépassée
- `404 Not Found` — bien introuvable

**Critères d'acceptation :**
- [ ] Zone de drop (drag & drop) avec aperçu miniature immédiat
- [ ] Barre de progression pendant l'upload
- [ ] Sélection multiple possible
- [ ] Indicateur du type de média (`PHOTO` / `VIDEO` / `PLAN`)
- [ ] Les `fileUrl` retournés sont directement utilisables en `<img src>` (bucket public)
- [ ] Afficher le nombre de médias ajoutés / maximum recommandé

---

### UBAX-FE-608 · Définir la photo de couverture

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/properties/{propertyId}/media/{mediaId}/cover` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `DIRECTEUR_AGENCE` · `COMMERCIAL` |
| **Path params** | `propertyId` : UUID · `mediaId` : UUID du média |
| **Request body** | _(aucun)_ |

> L'ancienne couverture est automatiquement désactivée côté backend.

**Response `200` :** _(objet `PropertyMediaResponse` avec `cover: true`)_

**Critères d'acceptation :**
- [ ] Bouton « Définir comme couverture » sur chaque photo dans la galerie de gestion
- [ ] Indicateur visuel (icône étoile ou badge) sur le média actuellement sélectionné comme couverture
- [ ] Mise à jour instantanée de l'affichage après succès

---

### UBAX-FE-609 · Supprimer un média

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `DELETE /v1/properties/{propertyId}/media/{mediaId}` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `DIRECTEUR_AGENCE` · `COMMERCIAL` |
| **Path params** | `propertyId` : UUID · `mediaId` : UUID du média |

> Supprime le fichier dans MinIO **et** l'entrée en base de données.

**Response `200` :**
```json
{ "status": "SUCCESS", "statusCode": 200, "message": "PROPERTY_MEDIA_DELETE_SUCCESS", "data": null }
```

**Critères d'acceptation :**
- [ ] Bouton « Supprimer » sur chaque miniature dans la galerie de gestion
- [ ] Dialog de confirmation avant suppression
- [ ] Retrait immédiat de la miniature après succès
- [ ] Alerte si l'on supprime la photo de couverture (inviter à en définir une nouvelle)

---

### UBAX-FE-610 · Gérer les documents légaux d'un bien

| Champ | Valeur |
|-------|--------|
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `DIRECTEUR_AGENCE` |
| **Bucket** | `property-documents` (**privé** — presigned GET requis pour lecture) |

#### Étape 1 — Obtenir l'URL d'upload

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/storage/presign/property-document` |
| **Query params** | `propertyId` (UUID, requis) · `contentType` (MIME, requis) |

**Response `200` :**
```json
{
  "data": {
    "uploadUrl": "http://localhost:9000/property-documents/...?X-Amz-Signature=...",
    "publicUrl": "http://localhost:9000/property-documents/propertyId/doc-uuid.pdf",
    "objectName": "propertyId/doc-uuid.pdf",
    "bucket": "property-documents",
    "expiresInSeconds": 900
  }
}
```

#### Étape 2 — Upload direct vers MinIO
```
PUT {uploadUrl}
Content-Type: application/pdf
Body: <binaire>
```

#### Étape 3 — Lier le document au bien

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/properties/{id}/documents` |
| **Content-Type** | `application/json` |

```json
{
  "fileUrl": "http://localhost:9000/property-documents/propertyId/doc-uuid.pdf",
  "documentType": "TITRE_FONCIER",
  "label": "Titre foncier N°12345"
}
```

**Response `201` :**
```json
{
  "data": {
    "id": "uuid",
    "propertyId": "uuid",
    "fileUrl": "http://localhost:9000/property-documents/uuid/titre.pdf",
    "documentType": "TITRE_FONCIER",
    "label": "Titre foncier N°12345",
    "verified": false,
    "verifiedAt": null,
    "createdAt": "2026-05-06T10:00:00"
  }
}
```

#### Étape 4 — Lire un document (presigned GET)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/storage/presign/read` |
| **Query params** | `fileUrl` : URL complète du document (telle que stockée) |

**Response `200` :**
```json
{
  "data": {
    "readUrl": "http://localhost:9000/property-documents/...?X-Amz-Signature=...",
    "objectName": "propertyId/doc-uuid.pdf",
    "bucket": "property-documents",
    "expiresInSeconds": 300
  }
}
```

> Utiliser `readUrl` dans `window.open(readUrl)` ou `<a href={readUrl}>` — valide **300 secondes**.

**Formats acceptés pour les documents :** `application/pdf` · `image/jpeg` · `image/png` · `image/webp` — max 20 Mo

**Critères d'acceptation :**
- [ ] Liste des documents avec type, libellé, statut de vérification (badge `Vérifié` / `En attente`)
- [ ] Bouton « Ajouter un document » avec sélection du type depuis une liste prédéfinie
- [ ] Upload du fichier avec barre de progression
- [ ] Bouton « Consulter » → appeler presigned read puis `window.open(readUrl)`
- [ ] Bouton « Supprimer » avec confirmation (`DELETE /v1/properties/{id}/documents/{docId}`)
- [ ] Ne pas afficher les boutons si le bien est en statut `PUBLISHED` (documents figés)

---

### UBAX-FE-611 · Archiver un bien

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `DELETE /v1/properties/{id}` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `DIRECTEUR_AGENCE` |
| **Path params** | `id` : UUID du bien |
| **Request body** | _(aucun)_ |

> Soft delete : passe `status` à `ARCHIVED`. Le bien disparaît du catalogue public mais reste en base.

**Response `200` :**
```json
{ "status": "SUCCESS", "statusCode": 200, "message": "PROPERTY_DELETE_SUCCESS", "data": null }
```

**Critères d'acceptation :**
- [ ] Bouton « Archiver » accessible depuis la liste « Mes biens » et la fiche bien
- [ ] Dialog de confirmation avec le titre du bien affiché
- [ ] Retrait du bien de la liste après succès (ou mise à jour du badge statut → `ARCHIVED`)
- [ ] Désactiver l'action si le bien est déjà archivé

---

### UBAX-FE-612 · Détail d'un bien (espace partenaire)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/properties/{id}` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Path params** | `id` : UUID du bien |

> Même endpoint que UBAX-FE-602 (public), mais affiché dans le contexte authentifié du partenaire avec les actions contextuelles selon le statut du bien.

**Response `200` :** _(objet `PropertyDetailResponse` — voir UBAX-FE-602)_

**Actions disponibles selon le statut :**

| Statut | Modifier | Soumettre | Archiver | Uploader médias | Gérer documents |
|--------|----------|-----------|----------|-----------------|-----------------|
| `DRAFT` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `PENDING` | ❌ | ❌ | ❌ | ❌ | ❌ |
| `PUBLISHED` | ❌ | ❌ | ✅ | ❌ | ❌ |
| `REJECTED` | ✅ | ❌ | ✅ | ✅ | ✅ |
| `ARCHIVED` | ❌ | ❌ | ❌ | ❌ | ❌ |

> ⚠️ Le bouton « Soumettre » n'apparaît que pour `DRAFT`. Le bouton « Modifier » n'apparaît que pour `DRAFT` et `REJECTED`.

**Endpoints utilisés sur cette page :**

| Action | Endpoint | Sous-rôle |
|--------|----------|-----------|
| Charger le bien | `GET /v1/properties/{id}` | Tout `PARTNER` |
| Modifier | `PUT /v1/properties/{id}` | `COMMERCIAL` · `DIRECTEUR_AGENCE` |
| Soumettre | `PATCH /v1/properties/{id}/submit` | `COMMERCIAL` · `DIRECTEUR_AGENCE` |
| Archiver | `DELETE /v1/properties/{id}` | `DIRECTEUR_AGENCE` |
| Lire un document privé | `GET /v1/storage/presign/read?bucket=property-documents&key=...` | Tout `PARTNER` |

**Critères d'acceptation :**
- [ ] Galerie photo/vidéo (slider) avec la couverture en premier — `fileUrl` directement (bucket public)
- [ ] Fiche complète : titre, prix, ville, quartier, surface, nb pièces, équipements, description
- [ ] Badge statut coloré (`DRAFT` gris · `PENDING` orange · `PUBLISHED` vert · `REJECTED` rouge · `ARCHIVED` gris foncé)
- [ ] Bandeau de rejet visible si `status = REJECTED` avec `rejectionReason` affiché
- [ ] Bouton **« Modifier »** visible si `status = DRAFT` ou `REJECTED` → redirige vers UBAX-FE-605
- [ ] Bouton **« Soumettre »** visible si `status = DRAFT` → dialog de confirmation avant appel UBAX-FE-606
- [ ] Bouton **« Archiver »** visible si `status ∈ {DRAFT, PUBLISHED, REJECTED}` → dialog de confirmation
- [ ] Section médias avec bouton « Ajouter des photos » si `status ∈ {DRAFT, REJECTED}`
- [ ] Section documents légaux : bouton « Consulter » → presigned read URL → `window.open(readUrl)` ; bouton « Ajouter » si `status ∈ {DRAFT, REJECTED}` · sous-rôle `DIRECTEUR_AGENCE` uniquement
- [ ] Badge ⚡ si `boosted = true`
- [ ] Carte interactive si `latitude` / `longitude` présents

---

---

## MODULE 7 — BIENS · PORTAIL HÔTEL

**Epic :** `UBAX-FE-BIENS-HOTEL`  
**Rôle requis (JWT) :** `UBAX_PARTNER` · Sous-rôles DB : `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT`

### Objectif

Un **hôtel partenaire** UBAX publie et gère ses espaces (chambres, suites, salles de conférence) depuis son espace dédié. Le cycle de vie est identique à celui des biens agence (création → médias → soumission → publication), mais le contexte hôtelier impose des libellés, des `propertyType` et un mode de tarification spécifiques.

**Flux principal :**
1. Un membre hôtel (`GERANT_HOTEL` ou `RESPONSABLE_HEBERGEMENT`) crée un espace en statut `DRAFT`.
2. Il ajoute des photos via upload direct ou presigned URL.
3. Il soumet l'espace (`DRAFT` → `PENDING`) pour examen par un admin UBAX.
4. L'admin publie (`PUBLISHED`) ou rejette (`REJECTED`) avec motif.
5. En cas de rejet, le membre corrige et re-soumet.

**Périmètre frontend de ce module :**

| Tâche | Action | Acteur |
|-------|--------|--------|
| UBAX-FE-701 — Catalogue public hôtel | Lecture | Grand public (sans compte) |
| UBAX-FE-702 — Détail d'un espace (public) | Lecture | Grand public (sans compte) |
| UBAX-FE-703 — Mes espaces | Lecture | Tout membre `PARTNER` (hôtel) |
| UBAX-FE-704 — Créer un espace | Écriture | `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| UBAX-FE-705 — Modifier un espace | Écriture | `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| UBAX-FE-706 — Soumettre à la modération | Écriture | `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| UBAX-FE-707 — Uploader des photos | Écriture | `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| UBAX-FE-708 — Définir la photo de couverture | Écriture | `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| UBAX-FE-709 — Supprimer un média | Écriture | `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| UBAX-FE-710 — Archiver un espace | Écriture | `GERANT_HOTEL` |
| UBAX-FE-711 — Détail d'un espace (espace hôtel) | Lecture + actions contextuelles | Tout membre `PARTNER` (hôtel) |

**Points d'attention :**
- Le `price` représente le **tarif unitaire en XOF** — afficher « /nuit » (NIGHTLY), « /semaine » (WEEKLY) ou « /mois » (MONTHLY) selon `paymentFrequency`. Le backend calcule le `totalAmount` de la réservation en conséquence.
- `transactionType` est toujours `SHORT_STAY` pour les espaces hôteliers — le champ doit être pré-sélectionné et non modifiable dans le formulaire.
- Les médias du bucket `properties-media` sont **publics** — utiliser directement `fileUrl` dans `<img src>`.
- Le statut `PUBLISHED` est **exclusivement** accordé par un admin UBAX — le bouton côté hôtel est « Soumettre », pas « Publier ».
- Les `propertyType` hôteliers sont : `HOTEL_ROOM` · `HOTEL_SUITE` · `HOTEL_STUDIO` · `EVENT_SPACE` · `CONFERENCE_ROOM` · `RESTAURANT_SPACE` — ne pas afficher les types agence dans ce contexte.

---

### UBAX-FE-701 · Catalogue public des espaces hôteliers

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/properties` |
| **Auth** | **Aucune** (public) |
| **Query params** | Voir tableau ci-dessous |

**Query params (tous optionnels) :**

| Paramètre | Type | Description | Exemple |
|-----------|------|-------------|---------|
| `status` | `string` | Défaut `PUBLISHED` | `PUBLISHED` |
| `propertyType` | `string` | Filtrer par type d'espace | `ROOM` |
| `city` | `string` | Code ville (code list `CITY`) | `ABIDJAN` |
| `transactionType` | `string` | Toujours `SHORT_STAY` pour hôtels | `SHORT_STAY` |
| `minPrice` | `number` | Tarif min (nuit/semaine/mois selon `paymentFrequency`) | `30000` |
| `maxPrice` | `number` | Tarif max | `500000` |
| `page` | `int` | Numéro de page (défaut `0`) | `0` |
| `size` | `int` | Taille de page (défaut `20`) | `20` |
| `sort` | `string` | Tri | `publishedAt,desc` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "PROPERTY_GET_LIST_SUCCESS",
  "data": {
    "content": [
      {
        "id": "uuid",
        "ownerId": null,
        "ownerName": null,
        "agencyId": "uuid",
        "agencyName": "Hôtel La Teranga",
        "title": "Suite Présidentielle vue mer",
        "description": "Suite luxueuse avec jacuzzi privatif et terrasse panoramique",
        "propertyType": "HOTEL_SUITE",
        "transactionType": "SHORT_STAY",
        "price": 150000,
        "condition": "NEW",
        "surfaceLiving": 85.0,
        "rooms": 1,
        "bedrooms": 1,
        "bathrooms": 2,
        "address": "Avenue du Président Léopold Sédar Senghor",
        "city": "DAKAR",
        "district": "Plateau",
        "latitude": 14.6937,
        "longitude": -17.4441,
        "amenities": [
          { "id": "uuid", "code": "AC", "customValue": null, "customDescription": null },
          { "id": "uuid", "code": "FURNISHED", "customValue": null, "customDescription": null }
        ],
        "boosted": false,
        "boostExpiresAt": null,
        "status": "PUBLISHED",
        "rejectionReason": null,
        "publishedAt": "2026-04-15T10:00:00",
        "createdAt": "2026-04-10T09:00:00",
        "updatedAt": "2026-04-15T10:00:00"
      }
    ],
    "totalElements": 45,
    "totalPages": 3,
    "size": 20,
    "number": 0
  }
}
```

**Tri par défaut :** espaces boostés en tête (`boosted DESC`), puis `publishedAt DESC`.

**Critères d'acceptation :**
- [ ] Grille de cartes paginée avec photo de couverture, titre, tarif/nuit, ville, type d'espace
- [ ] Filtres : ville (code list `CITY`), type d'espace (`ROOM` / `SUITE` / `CONFERENCE_ROOM`), fourchette de tarif
- [ ] Prix affiché avec l'unité « / nuit » (ex. « 150 000 XOF / nuit »)
- [ ] Badge du type d'espace avec libellé adapté (ex. « Suite », « Chambre »)
- [ ] Badge ⚡ si `boosted = true`
- [ ] Loader squelette pendant la requête · État vide si aucun résultat
- [ ] La `coverUrl` est directement utilisable en `<img src>` (bucket public)

---

### UBAX-FE-702 · Détail d'un espace (public)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/properties/{id}` |
| **Auth** | **Aucune** (public) |
| **Path params** | `id` : UUID de l'espace |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "PROPERTY_GET_SUCCESS",
  "data": {
    "id": "uuid",
    "agencyName": "Hôtel La Teranga",
    "title": "Suite Présidentielle vue mer",
    "propertyType": "HOTEL_SUITE",
    "transactionType": "SHORT_STAY",
    "price": 150000,
    "surfaceLiving": 85.0,
    "bedrooms": 1,
    "bathrooms": 2,
    "paymentFrequency": "NIGHTLY",
    "maxOccupancy": 2,
    "bedType": "KING",
    "mealPlan": "BREAKFAST",
    "city": "DAKAR",
    "district": "Plateau",
    "latitude": 14.6937,
    "longitude": -17.4441,
    "status": "PUBLISHED",
    "publishedAt": "2026-04-15T10:00:00",
    "medias": [
      {
        "id": "uuid",
        "propertyId": "uuid",
        "fileUrl": "http://localhost:9000/properties-media/uuid/photo1.jpg",
        "mediaType": "PHOTO",
        "cover": true,
        "createdAt": "2026-04-11T10:00:00"
      }
    ],
    "documents": []
  }
}
```

**Erreurs possibles :**
- `404 Not Found` — espace introuvable

**Critères d'acceptation :**
- [ ] Galerie photo (slider) avec la couverture en premier
- [ ] Fiche complète : titre, tarif/nuit, ville, quartier, surface, équipements
- [ ] Carte interactive si `latitude` / `longitude` présents
- [ ] Nom de l'hôtel (`agencyName`) affiché en en-tête
- [ ] Bouton « Réserver / Contacter » visible pour les visiteurs
- [ ] `fileUrl` des médias directement utilisable en `<img src>` (bucket public)

---

### UBAX-FE-703 · Mes espaces (portail hôtel)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/properties/mine` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Query params** | `status` (optionnel) · `page` (défaut `0`) · `size` (défaut `20`) · `sort=createdAt,desc` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "PROPERTY_GET_LIST_SUCCESS",
  "data": {
    "content": [
      {
        "id": "uuid",
        "title": "Suite Présidentielle vue mer",
        "propertyType": "HOTEL_SUITE",
        "transactionType": "SHORT_STAY",
        "price": 150000,
        "paymentFrequency": "NIGHTLY",
        "maxOccupancy": 2,
        "bedType": "KING",
        "mealPlan": "BREAKFAST",
        "city": "DAKAR",
        "status": "PUBLISHED",
        "boosted": false,
        "rejectionReason": null,
        "publishedAt": "2026-04-15T10:00:00",
        "createdAt": "2026-04-10T09:00:00"
      }
    ],
    "totalElements": 12,
    "totalPages": 1,
    "size": 20,
    "number": 0
  }
}
```

**Critères d'acceptation :**
- [ ] Tableau ou grille avec colonnes : titre, type d'espace, tarif/nuit, ville, statut (badge coloré), date création, actions
- [ ] Filtre par statut (`DRAFT` / `PENDING` / `PUBLISHED` / `REJECTED` / `ARCHIVED`)
- [ ] Badge `propertyType` adapté : « Chambre », « Suite », « Salle de conférence »
- [ ] Bouton « Modifier » pour statuts `DRAFT` et `REJECTED`
- [ ] Bouton « Soumettre » visible uniquement si `status = DRAFT`
- [ ] Bouton « Archiver » pour les espaces `PUBLISHED` ou `DRAFT`
- [ ] Motif de rejet (`rejectionReason`) affiché en tooltip ou bandeau si `status = REJECTED`
- [ ] Badge ⚡ si `boosted = true`

---

### UBAX-FE-704 · Créer un espace / chambre

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/properties` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| **Content-Type** | `application/json` |

**Request body :**
```json
{
  "title": "Suite Présidentielle avec vue mer",
  "description": "Suite luxueuse avec jacuzzi privatif, salon et terrasse panoramique",
  "propertyType": "HOTEL_SUITE",
  "transactionType": "SHORT_STAY",
  "price": 150000,
  "condition": "NEW",
  "surfaceLiving": 85.0,
  "rooms": 1,
  "bedrooms": 1,
  "bathrooms": 2,
  "balconies": 1,
  "floor": 5,
  "totalFloors": 10,
  "address": "Avenue du Président Léopold Sédar Senghor",
  "city": "DAKAR",
  "district": "Plateau",
  "latitude": 14.6937,
  "longitude": -17.4441,
  "paymentFrequency": "NIGHTLY",
  "maxOccupancy": 2,
  "bedType": "KING",
  "mealPlan": "BREAKFAST",
  "amenities": [
    { "code": "AC",       "description": "Climatisation" },
    { "code": "SECURITY", "description": "Gardiennage / sécurité sur site" },
    { "code": "PARKING",  "description": "Parking privatif ou en commun" },
    { "code": "ELEVATOR", "description": "Ascenseur dans l'immeuble" }
  ]
}
```

> **Champs obligatoires :** `title`, `propertyType`, `transactionType`, `price` (≥ 0), `city`  
> **`propertyType` disponibles pour hôtels :** `HOTEL_ROOM` · `HOTEL_SUITE` · `HOTEL_STUDIO` · `EVENT_SPACE` · `CONFERENCE_ROOM` · `RESTAURANT_SPACE`  
> **`transactionType` :** toujours `SHORT_STAY` pour les espaces hôteliers  
> **Champs hôteliers requis pour `SHORT_STAY` :** `paymentFrequency` (`NIGHTLY`/`WEEKLY`/`MONTHLY`) · `maxOccupancy` (≥ 1)  
> **`amenities`** : charger via `GET /v1/code-list/type/PROPERTY_AMENITY` (retourne `value` + `description`). Envoyer **les deux champs** dans le body — `{ "code": "AC", "description": "Climatisation" }` — pour éviter un appel DB supplémentaire côté backend.

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "PROPERTY_CREATE_SUCCESS",
  "data": {
    "id": "uuid",
    "title": "Suite Présidentielle avec vue mer",
    "propertyType": "SUITE",
    "transactionType": "SHORT_STAY",
    "price": 150000,
    "paymentFrequency": "NIGHTLY",
    "maxOccupancy": 2,
    "bedType": "KING",
    "mealPlan": "BREAKFAST",
    "city": "DAKAR",
    "status": "DRAFT",
    "createdAt": "2026-05-06T10:00:00"
  }
}
```

**Erreurs possibles :**
- `400 Bad Request` — champ obligatoire manquant ou valeur invalide (`paymentFrequency` ou `maxOccupancy` absent)
- `401 Unauthorized` — token absent
- `403 Forbidden` — rôle insuffisant

**Critères d'acceptation :**
- [ ] Formulaire multi-étapes : Étape 1 (informations générales), Étape 2 (capacité & surfaces), Étape 3 (localisation), Étape 4 (équipements & services)
- [ ] `propertyType` limité aux valeurs hôtelières (`HOTEL_ROOM` · `HOTEL_SUITE` · `HOTEL_STUDIO` · `EVENT_SPACE` · `CONFERENCE_ROOM` · `RESTAURANT_SPACE`)
- [ ] `transactionType` pré-sélectionné à `SHORT_STAY` et non modifiable
- [ ] Champ `price` libellé « Tarif unitaire (XOF) » + sélecteur `paymentFrequency` (NIGHTLY · WEEKLY · MONTHLY)
- [ ] Champ `maxOccupancy` (capacité max, entier ≥ 1) affiché et requis
- [ ] Champ `bedType` code list `BED_TYPE` : `SINGLE`·`DOUBLE`·`TWIN`·`KING`·`QUEEN`·`BUNK`
- [ ] Champ `mealPlan` code list `MEAL_PLAN` : `ROOM_ONLY`·`BREAKFAST`·`HALF_BOARD`·`FULL_BOARD`·`ALL_INCLUSIVE`
- [ ] Ville chargée depuis `GET /v1/code-list/type/CITY`
- [ ] Carte interactive optionnelle pour saisir `latitude` / `longitude`
- [ ] Validation frontend avant envoi (champs requis, `price` ≥ 0)
- [ ] Après création : rediriger vers la fiche de l'espace en mode édition
- [ ] Conserver les données saisies en cas d'erreur API

---

### UBAX-FE-705 · Modifier un espace

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PUT /v1/properties/{id}` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| **Path params** | `id` : UUID de l'espace |
| **Content-Type** | `application/json` |

**Request body :** _(identique à UBAX-FE-704 — seuls les champs non-null sont modifiés)_

> ⚠️ Modification autorisée uniquement en statut `DRAFT` ou `REJECTED`. Désactiver le formulaire pour tout autre statut.

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "PROPERTY_UPDATE_SUCCESS",
  "data": {
    "id": "uuid",
    "title": "Suite Présidentielle avec vue mer – Rénovée",
    "propertyType": "HOTEL_SUITE",
    "transactionType": "SHORT_STAY",
    "price": 160000,
    "paymentFrequency": "NIGHTLY",
    "status": "DRAFT",
    "updatedAt": "2026-05-06T11:00:00"
  }
}
```

**Erreurs possibles :**
- `400 Bad Request` — espace dans un statut non modifiable
- `403 Forbidden` — espace appartenant à un autre hôtel
- `404 Not Found` — espace introuvable

**Critères d'acceptation :**
- [ ] Formulaire pré-rempli avec les valeurs actuelles de l'espace
- [ ] Même structure multi-étapes que la création
- [ ] Bandeau d'information si `status = REJECTED` avec le motif de rejet visible
- [ ] Toast de succès après sauvegarde
- [ ] Désactiver et griser le formulaire si `status = PUBLISHED`, `PENDING` ou `ARCHIVED`

---

### UBAX-FE-706 · Soumettre un espace à la modération

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/properties/{id}/submit` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| **Path params** | `id` : UUID de l'espace |
| **Request body** | _(aucun)_ |

> ⚠️ L'espace doit être en statut `DRAFT`. La soumission passe le statut à `PENDING`.

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "PROPERTY_SUBMIT_SUCCESS",
  "data": {
    "id": "uuid",
    "title": "Suite Présidentielle avec vue mer",
    "status": "PENDING",
    "updatedAt": "2026-05-06T12:00:00"
  }
}
```

**Erreurs possibles :**
- `400 Bad Request` — espace non en statut `DRAFT`
- `403 Forbidden` — espace appartenant à un autre hôtel
- `404 Not Found` — espace introuvable

**Critères d'acceptation :**
- [ ] Bouton « Soumettre pour modération » visible uniquement si `status = DRAFT`
- [ ] Dialog de confirmation avant soumission
- [ ] Après succès : afficher badge `PENDING` et désactiver les boutons d'édition
- [ ] Message informatif : « Votre espace est en cours d'examen. Vous serez notifié de la décision. »

---

### UBAX-FE-707 · Uploader des photos / médias

| Champ | Valeur |
|-------|--------|
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| **Bucket** | `properties-media` (public — `fileUrl` utilisable directement en `<img src>`) |

#### Stratégie A — Upload direct multipart (recommandé mobile / fichiers ≤ 50 Mo)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/properties/{id}/media/upload` |
| **Content-Type** | `multipart/form-data` |
| **Path params** | `id` : UUID de l'espace |

**Champs du formulaire :**

| Nom | Type | Requis | Description |
|-----|------|:------:|-------------|
| `file` | `File` | ✅ | Image ou vidéo |
| `mediaType` | `string` | ✅ | `PHOTO` · `VIDEO` · `PLAN` |
| `cover` | `boolean` | — | `true` pour définir comme couverture (défaut `false`) |

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "PROPERTY_MEDIA_ADD_SUCCESS",
  "data": {
    "id": "uuid",
    "propertyId": "uuid",
    "fileUrl": "http://localhost:9000/properties-media/propertyId/uuid.jpg",
    "mediaType": "PHOTO",
    "cover": false,
    "createdAt": "2026-05-06T10:00:00"
  }
}
```

#### Stratégie B — Presigned URL PUT (recommandé web / fichiers volumineux)

**Étape 1 — Obtenir l'URL d'upload :**

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/storage/presign/property-media` |
| **Query params** | `propertyId` (UUID, requis) · `contentType` (MIME, requis) |

**Response `200` :**
```json
{
  "data": {
    "uploadUrl": "http://localhost:9000/properties-media/...?X-Amz-Signature=...",
    "publicUrl": "http://localhost:9000/properties-media/propertyId/uuid.jpg",
    "objectName": "propertyId/uuid.jpg",
    "bucket": "properties-media",
    "expiresInSeconds": 900
  }
}
```

**Étape 2 — Upload direct vers MinIO (côté navigateur) :**
```
PUT {uploadUrl}
Content-Type: image/jpeg
Body: <binaire>
```

**Étape 3 — Lier le média à l'espace :**

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/properties/{id}/media` |
| **Content-Type** | `application/json` |

```json
{
  "fileUrl": "http://localhost:9000/properties-media/propertyId/uuid.jpg",
  "mediaType": "PHOTO",
  "cover": false
}
```

**Formats acceptés :**

| Type | MIME | Taille max |
|------|------|-----------|
| Image | `image/jpeg` · `image/png` · `image/webp` | 10 Mo |
| Vidéo | `video/mp4` · `video/quicktime` · `video/mpeg` | 100 Mo |

**Erreurs possibles :**
- `400 Bad Request` — type MIME non autorisé ou taille dépassée
- `404 Not Found` — espace introuvable

**Critères d'acceptation :**
- [ ] Zone de drop (drag & drop) avec aperçu miniature immédiat
- [ ] Barre de progression pendant l'upload
- [ ] Sélection multiple possible
- [ ] Indicateur du type de média (`PHOTO` / `VIDEO` / `PLAN`)
- [ ] Recommandation affichée : au minimum 3 photos par espace
- [ ] Utiliser `mediaType = PLAN` pour les plans de salle de conférence
- [ ] Les `fileUrl` retournés sont directement utilisables en `<img src>` (bucket public)

---

### UBAX-FE-708 · Définir la photo de couverture

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/properties/{propertyId}/media/{mediaId}/cover` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| **Path params** | `propertyId` : UUID · `mediaId` : UUID du média |
| **Request body** | _(aucun)_ |

> L'ancienne couverture est automatiquement désactivée côté backend.

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "PROPERTY_MEDIA_COVER_SUCCESS",
  "data": {
    "id": "uuid",
    "propertyId": "uuid",
    "fileUrl": "http://localhost:9000/properties-media/propertyId/uuid.jpg",
    "mediaType": "PHOTO",
    "cover": true,
    "createdAt": "2026-05-06T10:00:00"
  }
}
```

**Erreurs possibles :**
- `404 Not Found` — espace ou média introuvable
- `403 Forbidden` — espace appartenant à un autre hôtel

**Critères d'acceptation :**
- [ ] Bouton « Définir comme couverture » sur chaque photo dans la galerie de gestion
- [ ] Indicateur visuel (icône étoile ou badge) sur le média actuellement sélectionné comme couverture
- [ ] Mise à jour instantanée de l'affichage après succès
- [ ] La couverture est la première image affichée dans le catalogue public

---

### UBAX-FE-709 · Supprimer un média

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `DELETE /v1/properties/{propertyId}/media/{mediaId}` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| **Path params** | `propertyId` : UUID · `mediaId` : UUID du média |

> Supprime le fichier dans MinIO **et** l'entrée en base de données.

**Response `200` :**
```json
{ "status": "SUCCESS", "statusCode": 200, "message": "PROPERTY_MEDIA_DELETE_SUCCESS", "data": null }
```

**Erreurs possibles :**
- `404 Not Found` — espace ou média introuvable
- `403 Forbidden` — espace appartenant à un autre hôtel

**Critères d'acceptation :**
- [ ] Bouton « Supprimer » sur chaque miniature dans la galerie de gestion
- [ ] Dialog de confirmation avant suppression
- [ ] Retrait immédiat de la miniature après succès
- [ ] Alerte si l'on supprime la photo de couverture (inviter à en définir une nouvelle)

---

### UBAX-FE-710 · Archiver un espace (fermeture / rénovation)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `DELETE /v1/properties/{id}` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Sous-rôle DB** | `GERANT_HOTEL` |
| **Path params** | `id` : UUID de l'espace |
| **Request body** | _(aucun)_ |

> Soft delete : passe `status` à `ARCHIVED`. L'espace disparaît du catalogue public mais reste en base.

**Response `200` :**
```json
{ "status": "SUCCESS", "statusCode": 200, "message": "PROPERTY_DELETE_SUCCESS", "data": null }
```

**Erreurs possibles :**
- `403 Forbidden` — espace appartenant à un autre hôtel
- `404 Not Found` — espace introuvable

**Critères d'acceptation :**
- [ ] Bouton « Archiver » accessible depuis la liste « Mes espaces » et la fiche de l'espace
- [ ] Libellé contextuel selon le type : « Fermer cette chambre » / « Mettre en rénovation »
- [ ] Dialog de confirmation avec le titre de l'espace affiché
- [ ] Retrait de l'espace de la liste après succès (ou mise à jour du badge statut → `ARCHIVED`)
- [ ] Désactiver l'action si l'espace est déjà archivé

---

### UBAX-FE-711 · Détail d'un espace (espace hôtel)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/properties/{id}` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` |
| **Path params** | `id` : UUID de l'espace |

> Même endpoint que UBAX-FE-702 (public), affiché dans le contexte authentifié du partenaire hôtel avec les actions contextuelles selon le statut. Identique à UBAX-FE-612 (agence) mais avec les sous-rôles hôteliers et les libellés adaptés.

**Response `200` :** _(objet `PropertyDetailResponse` — voir UBAX-FE-702)_

**Actions disponibles selon le statut :**

| Statut | Modifier | Soumettre | Archiver | Uploader photos |
|--------|----------|-----------|----------|-----------------|
| `DRAFT` | ✅ | ✅ | ✅ | ✅ |
| `PENDING` | ❌ | ❌ | ❌ | ❌ |
| `PUBLISHED` | ❌ | ❌ | ✅ | ❌ |
| `REJECTED` | ✅ | ❌ | ✅ | ✅ |
| `ARCHIVED` | ❌ | ❌ | ❌ | ❌ |

**Endpoints utilisés sur cette page :**

| Action | Endpoint | Sous-rôle |
|--------|----------|-----------|
| Charger l'espace | `GET /v1/properties/{id}` | Tout `PARTNER` (hôtel) |
| Modifier | `PUT /v1/properties/{id}` | `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| Soumettre | `PATCH /v1/properties/{id}/submit` | `GERANT_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| Archiver | `DELETE /v1/properties/{id}` | `GERANT_HOTEL` |

**Critères d'acceptation :**
- [ ] Galerie photo/vidéo (slider) avec la couverture en premier — `fileUrl` directement (bucket public)
- [ ] Fiche complète : titre, tarif unitaire en XOF (libellé selon `paymentFrequency`), ville, surface, type d'espace (`HOTEL_ROOM` · `HOTEL_SUITE` · `HOTEL_STUDIO` · `EVENT_SPACE` · `CONFERENCE_ROOM` · `RESTAURANT_SPACE`), description
- [ ] Badge statut coloré (`DRAFT` gris · `PENDING` orange · `PUBLISHED` vert · `REJECTED` rouge · `ARCHIVED` gris foncé)
- [ ] Bandeau de rejet visible si `status = REJECTED` avec `rejectionReason` affiché
- [ ] Bouton **« Modifier »** visible si `status = DRAFT` ou `REJECTED` → redirige vers UBAX-FE-705
- [ ] Bouton **« Soumettre »** visible si `status = DRAFT` → dialog de confirmation avant appel UBAX-FE-706
- [ ] Bouton **« Archiver / Fermer »** visible si `status ∈ {DRAFT, PUBLISHED, REJECTED}` → dialog de confirmation
- [ ] Section photos avec bouton « Ajouter des photos » si `status ∈ {DRAFT, REJECTED}`
- [ ] Badge ⚡ si `boosted = true`
- [ ] Carte interactive si `latitude` / `longitude` présents
- [ ] Pas de section documents légaux (non applicable au contexte hôtelier)

---

---

## MODULE 8 — STORAGE · GESTION DES FICHIERS

**Epic :** `UBAX-FE-STORAGE`  
**Rôle requis (JWT) :** Authentifié (toutes actions sauf lecture des buckets publics)

### Objectif

Le module Storage gère l'ensemble des fichiers de la plateforme : avatars, logos, médias de biens et documents confidentiels. Il expose deux comportements selon la visibilité du bucket cible : accès direct pour les buckets publics, presigned GET temporaire pour les buckets privés.

**Deux types de buckets :**

| Bucket | Accès | Contenu | Comment lire |
|--------|-------|---------|-------------|
| `users-avatars` | **Public** | Avatars utilisateurs | `<img src={fileUrl}>` directement |
| `agencies-logos` | **Public** | Logos agences | `<img src={fileUrl}>` directement |
| `properties-media` | **Public** | Photos/vidéos des biens | `<img src={fileUrl}>` directement |
| `property-documents` | **Privé** | Docs légaux des biens | Presigned GET (TTL 300 s) |
| `tenant-documents` | **Privé** | Pièces KYC locataires | Presigned GET (TTL 180 s) |
| `partner-documents` | **Privé** | Docs légaux partenaires | Presigned GET (TTL 300 s) |
| `ticket-attachments` | **Privé** | Pièces jointes tickets | Presigned GET (TTL 600 s) |
| `documents-generated` | **Privé** | Contrats / factures PDF | Presigned GET (TTL 300 s) |

**Points d'attention :**
- Ne **jamais** appeler `GET /v1/storage/presign/read` pour un bucket public — retourne une erreur `400`.
- Le TTL est **défini automatiquement côté backend** selon le bucket — le frontend ne le contrôle pas.
- Une presigned URL expire — ne pas la stocker durablement en cache. La régénérer à chaque affichage.

---

### UBAX-FE-801 · Uploader / remplacer son avatar

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/users/me/avatar` |
| **Auth** | Bearer token · Authentifié |
| **Content-Type** | `multipart/form-data` |

**Champs du formulaire :**

| Nom | Type | Requis | Contraintes |
|-----|------|:------:|-------------|
| `file` | `File` | ✅ | JPEG · PNG · WEBP — max 5 Mo |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "Photo de profil mise à jour",
  "data": {
    "avatarUrl": "http://localhost:9000/users-avatars/keycloakId.jpg"
  }
}
```

> ✅ `avatarUrl` retourné est une URL directe MinIO **publique** — utiliser dans `<img src>` sans token.  
> Uploader une nouvelle image **remplace automatiquement** l'ancienne (même objectName `{keycloakId}.{ext}`).

**Erreurs possibles :**
- `400 Bad Request` — fichier vide, format non supporté, taille > 5 Mo
- `401 Unauthorized` — token absent

**Critères d'acceptation :**
- [ ] Bouton « Modifier la photo » sur la page profil
- [ ] Aperçu immédiat de la nouvelle photo avant confirmation
- [ ] Validation locale du format et de la taille avant envoi
- [ ] Barre de progression pendant l'upload
- [ ] Mise à jour instantanée de l'avatar affiché dans le header / sidebar après succès
- [ ] `avatarUrl` persisté dans le store global de l'utilisateur connecté

---

### UBAX-FE-802 · Uploader le logo de l'agence

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/storage/upload/agency-logo` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` + sous-rôle `DIRECTEUR_AGENCE` |
| **Content-Type** | `multipart/form-data` |

**Champs du formulaire :**

| Nom | Type | Requis | Contraintes |
|-----|------|:------:|-------------|
| `file` | `File` | ✅ | JPEG · PNG · WEBP — max 10 Mo |

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "Logo agence uploadé avec succès",
  "data": {
    "fileUrl": "http://localhost:9000/agencies-logos/logo-uuid.png",
    "objectName": "logo-uuid.png",
    "bucket": "agencies-logos"
  }
}
```

> ✅ `fileUrl` est public — utilisable directement dans `<img src>` sans token.

**Erreurs possibles :**
- `400 Bad Request` — format non supporté ou taille > 10 Mo
- `403 Forbidden` — sous-rôle `DIRECTEUR_AGENCE` requis

**Critères d'acceptation :**
- [ ] Zone d'upload avec aperçu du logo actuel (si existant) et bouton de remplacement
- [ ] Restriction visuelle aux formats image (JPEG/PNG/WEBP)
- [ ] Validation locale de la taille avant envoi
- [ ] Mise à jour du logo affiché dans le header de l'espace partenaire après succès

---

### UBAX-FE-803 · Lire un document privé (presigned GET)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/storage/presign/read` |
| **Auth** | Bearer token · Authentifié |
| **Query params** | `fileUrl` (requis) : URL complète du fichier telle que stockée |

> Utiliser cet endpoint **uniquement** pour les buckets privés. Pour les buckets publics, utiliser directement `fileUrl`.

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "URL de lecture générée – valide 300 secondes",
  "data": {
    "readUrl": "http://localhost:9000/property-documents/...?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=...",
    "objectName": "propertyId/doc-uuid.pdf",
    "bucket": "property-documents",
    "expiresInSeconds": 300
  }
}
```

**TTL automatiques par bucket (définis par le backend — non modifiables) :**

| Bucket | TTL |
|--------|-----|
| `tenant-documents` | **180 s** |
| `property-documents` | **300 s** |
| `documents-generated` | **300 s** |
| `partner-documents` | **300 s** |
| `ticket-attachments` | **600 s** |

**Erreurs possibles :**
- `400 Bad Request` — `fileUrl` invalide ou bucket public
- `401 Unauthorized` — token absent

**Utilisation côté front :**
```javascript
// Ouvrir un PDF dans un nouvel onglet
const res = await fetch(
  `/api/v1/storage/presign/read?fileUrl=${encodeURIComponent(fileUrl)}`,
  { headers: { Authorization: `Bearer ${token}` } }
);
const { data } = await res.json();
window.open(data.readUrl, '_blank');
```

**Critères d'acceptation :**
- [ ] Bouton « Consulter » ou « Télécharger » sur chaque document privé
- [ ] Appeler cet endpoint **au clic**, pas au chargement de page (URL expire rapidement)
- [ ] Ne pas mettre en cache `readUrl` — régénérer à chaque clic
- [ ] Loader visible pendant la génération de l'URL
- [ ] Ouvrir `readUrl` dans un nouvel onglet (`window.open`) ou déclencher un téléchargement selon le type

---

---

## MODULE 9 — DOSSIER LOCATAIRE · KYC CLIENT

**Epic :** `UBAX-FE-DOSSIER-LOCATAIRE`  
**Rôle requis (JWT) :** `UBAX_CLIENT` pour créer/consulter son propre dossier · `UBAX_PARTNER` ou `UBAX_ADMIN` pour consulter et qualifier/rejeter

### Objectif

Un **client** UBAX peut constituer son dossier de solvabilité (KYC) en ligne pour décrocher un bien en location. Ce module couvre la création du dossier, l'upload de documents d'identité, la soumission, et la revue par l'agence.

**Flux principal :**
1. Le client crée son dossier (`POST /v1/tenants/profile`) en précisant **obligatoirement `propertyId`** — le bien qu'il cible.
2. Si les documents d'identité obligatoires sont fournis dès la création, le dossier passe directement en `PENDING_REVIEW`.
3. Sinon, le client complète via `PATCH /profile` après avoir uploadé ses documents (presigned URL → multipart → URL retournée).
4. Quand `idDocumentUrl` + `idDocumentNumber` + `idDocumentExpiry` sont tous renseignés → statut auto-passe à `PENDING_REVIEW`.
5. L'agence/admin qualifie (`QUALIFIED`) ou rejette (`REJECTED`) le dossier.
6. En cas de rejet, le client corrige et re-soumet (`REJECTED` → `PENDING_REVIEW` automatique si dossier complet).
7. **Pour un autre bien :** le client n'a **pas besoin de recréer son dossier** — il fait un `PATCH /profile` avec `{ "propertyId": "uuid-nouveau-bien" }`. Le dossier KYC existant (pièces, revenus) est réutilisé.

**Périmètre frontend de ce module :**

| Tâche | Action | Acteur |
|-------|--------|--------|
| UBAX-FE-901 — Upload document KYC | Écriture | `CLIENT` |
| UBAX-FE-902 — Créer son dossier locataire | Écriture | `CLIENT` |
| UBAX-FE-903 — Voir son dossier locataire | Lecture | `CLIENT` |
| UBAX-FE-904 — Mettre à jour / Changer de bien ciblé | Écriture | `CLIENT` |
| UBAX-FE-905 — Liste des dossiers (agence / admin) | Lecture | `PARTNER` · `ADMIN` |
| UBAX-FE-906 — Qualifier un dossier | Écriture | `PARTNER` · `ADMIN` |
| UBAX-FE-907 — Rejeter un dossier | Écriture | `PARTNER` · `ADMIN` |

**Points d'attention :**
- **Un seul dossier par client** (unicité `userId`). Pour changer de bien → `PATCH /profile` avec `propertyId`, pas un nouveau `POST`.
- `propertyId` est **obligatoire** à la création — le dossier est rattaché au bien visé dès le départ.
- `isDossierComplete()` ne requiert que 3 champs : `idDocumentUrl`, `idDocumentNumber`, `idDocumentExpiry` — `incomeProofUrl` est **optionnel** (travailleurs informels).
- Le bucket `tenant-documents` est **privé** — URLs presignées temporaires. Ne pas les stocker en clair.
- Le statut `PENDING_REVIEW` est déclenché **automatiquement** par le backend — pas de bouton "Soumettre" explicite.
- Un dossier `REJECTED` peut être corrigé et re-soumis — afficher le motif de rejet clairement.
- `hasGuarantor = true` débloque les champs `guarantorName`, `guarantorPhone`, `guarantorEmail`.

---

### UBAX-FE-901 · Upload d'un document KYC (upload direct mobile)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/storage/upload` |
| **Auth** | Bearer token |
| **Content-Type** | `multipart/form-data` |

**Request (multipart/form-data) :**
```
POST /v1/storage/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data

file=<binary>          ← fichier sélectionné par l'utilisateur
bucket=tenant-documents
```

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "STORAGE_UPLOAD_SUCCESS",
  "data": {
    "fileUrl": "http://localhost:9000/tenant-documents/uuid/id-card.pdf"
  }
}
```

> **Important :** stocker `fileUrl` retourné — c'est cette URL qui sera passée dans `idDocumentUrl`, `incomeProofUrl` ou `addressProofUrl` lors de la création / mise à jour du dossier.

**Erreurs possibles :**
- `400 Bad Request` — fichier absent ou bucket invalide
- `401 Unauthorized` — token absent ou expiré

**Critères d'acceptation :**
- [ ] Sélecteur de fichier natif mobile (PDF, JPG, PNG acceptés)
- [ ] Afficher la progression de l'upload (barre ou spinner)
- [ ] Stocker `fileUrl` en état local pour l'étape suivante (création / mise à jour dossier)
- [ ] Afficher un aperçu miniature ou le nom du fichier uploadé après succès
- [ ] Gérer le cas d'erreur réseau avec message d'erreur clair

---

### UBAX-FE-902 · Créer son dossier locataire

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/tenants/profile` |
| **Auth** | Bearer token · Rôle `UBAX_CLIENT` |
| **Content-Type** | `application/json` |

> **Règle :** un seul dossier par client. Si le client a déjà un dossier → `409 Conflict` → rediriger vers `PATCH /profile` pour mettre à jour ou changer de bien.

**Request body :**
```json
{
  "propertyId": "394e1b94-6d87-41b2-8b31-031a9f45944d",
  "employmentStatus": "SELF_EMPLOYED",
  "employerName": null,
  "monthlyIncome": 300000,
  "hasGuarantor": false,
  "guarantorName": null,
  "guarantorPhone": null,
  "guarantorEmail": null,
  "idDocumentUrl": "http://localhost:9000/tenant-documents/uuid/id-card.pdf",
  "idDocumentType": "CNI",
  "idDocumentNumber": "XZZZTBPZZZFW4",
  "idDocumentExpiry": "2030-12-31",
  "incomeProofUrl": null,
  "addressProofUrl": null
}
```

> **Champs obligatoires :** `propertyId`, `employmentStatus`, `monthlyIncome`  
> **`propertyId` :** UUID du bien ciblé — doit exister en base (404 sinon)  
> **`employmentStatus` disponibles :** via `GET /v1/code-list/type/EMPLOYMENT_STATUS`  
> **`idDocumentType` disponibles :** via `GET /v1/code-list/type/ID_TYPE` (`CNI`, `PASSEPORT`, `PERMIS_CONDUIRE`, `TITRE_SEJOUR`, `CARTE_CONSULAIRE`)  
> **Auto-transition :** si `idDocumentUrl` + `idDocumentNumber` + `idDocumentExpiry` sont fournis → statut = `PENDING_REVIEW` (sinon `INCOMPLETE`)

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "TENANT_CREATE_SUCCESS",
  "data": {
    "id": "uuid",
    "propertyId": "394e1b94-6d87-41b2-8b31-031a9f45944d",
    "userId": "uuid",
    "employmentStatus": "SELF_EMPLOYED",
    "employerName": null,
    "monthlyIncome": 300000,
    "hasGuarantor": false,
    "idDocumentType": "CNI",
    "idDocumentNumber": "XZZZTBPZZZFW4",
    "idDocumentExpiry": "2030-12-31",
    "idDocumentUrl": "http://localhost:9000/tenant-documents/uuid/id-card.pdf",
    "incomeProofUrl": null,
    "addressProofUrl": null,
    "status": "PENDING_REVIEW",
    "qualified": false,
    "createdAt": "2026-05-17T10:00:00"
  }
}
```

**Erreurs possibles :**
- `400 Bad Request` — `propertyId`, `employmentStatus` ou `monthlyIncome` manquant
- `404 Not Found` — bien (`propertyId`) introuvable
- `409 Conflict` — un dossier existe déjà → utiliser `PATCH /profile`

**Critères d'acceptation :**
- [ ] Le bien est pré-sélectionné depuis la fiche bien (`propertyId` transmis automatiquement)
- [ ] Formulaire en 2 étapes : Étape 1 (situation professionnelle + revenu + garant), Étape 2 (documents d'identité via upload)
- [ ] `hasGuarantor` toggle — affiche/masque les champs garant
- [ ] Upload intégré pour `idDocumentUrl`, `incomeProofUrl`, `addressProofUrl` (appel UBAX-FE-901)
- [ ] Si `409 Conflict` → ne pas afficher une erreur brute — proposer au client de mettre à jour son dossier existant
- [ ] Indicateur visuel du statut résultant (`INCOMPLETE` ou `PENDING_REVIEW`) après création
- [ ] Redirection vers la vue "Mon dossier" après succès

---

### UBAX-FE-903 · Voir son dossier locataire

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/tenants/profile` |
| **Auth** | Bearer token · `UBAX_CLIENT` ou tout rôle authentifié |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "TENANT_GET_SUCCESS",
  "data": {
    "id": "uuid",
    "propertyId": "394e1b94-6d87-41b2-8b31-031a9f45944d",
    "userId": "uuid",
    "fullName": "Amara Koné",
    "email": "amara.kone@example.ci",
    "employmentStatus": "SELF_EMPLOYED",
    "employerName": null,
    "monthlyIncome": 300000,
    "hasGuarantor": false,
    "idDocumentType": "CNI",
    "idDocumentNumber": "XZZZTBPZZZFW4",
    "idDocumentExpiry": "2030-12-31",
    "idDocumentUrl": "http://localhost:9000/tenant-documents/uuid/id-card.pdf",
    "incomeProofUrl": null,
    "addressProofUrl": null,
    "status": "PENDING_REVIEW",
    "qualified": false,
    "qualifiedAt": null,
    "rejectionReason": null,
    "createdAt": "2026-05-17T10:00:00",
    "updatedAt": "2026-05-17T10:00:00"
  }
}
```

**Erreurs possibles :**
- `404 Not Found` — aucun dossier créé pour ce client

**Critères d'acceptation :**
- [ ] Afficher le bien ciblé (`propertyId`) avec un lien vers la fiche bien
- [ ] Afficher le badge de statut coloré : `INCOMPLETE` (gris) · `PENDING_REVIEW` (orange) · `QUALIFIED` (vert) · `REJECTED` (rouge)
- [ ] Motif de rejet (`rejectionReason`) affiché en bandeau si `status = REJECTED`
- [ ] Bouton « Mettre à jour » visible si `status ∈ {INCOMPLETE, REJECTED}`
- [ ] Bouton « Changer de bien » visible pour modifier `propertyId` sans recréer le dossier
- [ ] Documents affichés avec lien/aperçu (URL privée — voir UBAX-FE-803 pour presign de lecture)
- [ ] Indicateur de complétude du dossier (ex. check list : identité ✓, revenus ○)

---

### UBAX-FE-904 · Mettre à jour son dossier / Changer de bien ciblé

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/tenants/profile` |
| **Auth** | Bearer token · `UBAX_CLIENT` ou tout rôle authentifié |
| **Content-Type** | `application/json` |

> **Dossier réutilisable :** le client n'a qu'un seul dossier KYC. Pour candidater sur un nouveau bien, il suffit de passer `propertyId` dans ce PATCH — les documents et informations professionnelles sont conservés, seul le bien ciblé change.

**Request body (tous les champs sont optionnels — seuls les non-null sont mis à jour) :**

_Cas 1 — Compléter les documents KYC :_
```json
{
  "idDocumentUrl": "http://localhost:9000/tenant-documents/uuid/id-card.pdf",
  "idDocumentType": "CNI",
  "idDocumentNumber": "XZZZTBPZZZFW4",
  "idDocumentExpiry": "2030-12-31",
  "incomeProofUrl": "http://localhost:9000/tenant-documents/uuid/income.pdf",
  "addressProofUrl": "http://localhost:9000/tenant-documents/uuid/address.pdf"
}
```

_Cas 2 — Changer de bien ciblé (sans retoucher les documents) :_
```json
{
  "propertyId": "nouveau-uuid-bien"
}
```

_Cas 3 — Changer de bien ET mettre à jour les infos pro :_
```json
{
  "propertyId": "nouveau-uuid-bien",
  "employmentStatus": "EMPLOYEE",
  "employerName": "Nouvelle Société CI",
  "monthlyIncome": 450000
}
```

> **Auto-transition :** après mise à jour, si `idDocumentUrl` + `idDocumentNumber` + `idDocumentExpiry` sont tous renseignés et que le statut est `INCOMPLETE` ou `REJECTED` → statut passe automatiquement à `PENDING_REVIEW`.  
> Si `propertyId` est fourni → le bien est résolu (404 si introuvable) et le dossier est réassigné.

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "TENANT_UPDATE_SUCCESS",
  "data": {
    "id": "uuid",
    "propertyId": "nouveau-uuid-bien",
    "status": "PENDING_REVIEW",
    "idDocumentUrl": "http://localhost:9000/tenant-documents/uuid/id-card.pdf",
    "incomeProofUrl": "http://localhost:9000/tenant-documents/uuid/income.pdf",
    "addressProofUrl": "http://localhost:9000/tenant-documents/uuid/address.pdf",
    "updatedAt": "2026-05-17T11:00:00"
  }
}
```

**Erreurs possibles :**
- `400 Bad Request` — format de date invalide pour `idDocumentExpiry`
- `404 Not Found` — dossier introuvable ou bien (`propertyId`) introuvable

**Critères d'acceptation :**
- [ ] Formulaire pré-rempli avec les valeurs actuelles du dossier
- [ ] Champ `propertyId` (sélecteur de bien) disponible pour changer le bien ciblé
- [ ] Seuls les champs modifiés sont envoyés (patch partiel)
- [ ] Upload intégré pour les nouveaux documents (réutilise UBAX-FE-901)
- [ ] Toast de succès indiquant le nouveau statut (ex. « Dossier soumis pour révision »)
- [ ] Bouton « Changer de bien » depuis la vue "Mon dossier" → ouvre ce formulaire avec seul `propertyId` modifiable
- [ ] Bouton désactivé si `status ∉ {INCOMPLETE, REJECTED}` (sauf pour le changement de bien qui reste possible en tout statut)

---

### UBAX-FE-905 · Liste des dossiers locataires (agence / admin)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/tenants` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` (agence) ou `UBAX_ADMIN` / `UBAX_SUPER_ADMIN` |
| **Query params** | `status` (optionnel) · `propertyId` (optionnel) · `withoutContract` (optionnel) · `page` (défaut `0`) · `size` (défaut `20`) · `sort=createdAt,desc` |

**Query params :**

| Paramètre | Type | Description |
|-----------|------|-------------|
| `status` | `string` | Filtre par statut : `INCOMPLETE` · `PENDING_REVIEW` · `QUALIFIED` · `REJECTED` · `BLACKLISTED` |
| `propertyId` | `UUID` | Filtre les dossiers rattachés à un bien précis |
| `withoutContract` | `boolean` | Si `true` **et** `propertyId` fourni : n'affiche que les candidats sans contrat actif pour ce bien — candidatures en attente de décision. Ignoré si `propertyId` est absent. |

> **Comportement PARTNER :** retourne uniquement les dossiers des clients liés aux biens de son agence.  
> **Comportement ADMIN :** retourne tous les dossiers toutes agences confondues.  
> **PARTNER hôtel :** accès refusé — `403 Forbidden`.

**Cas d'usage principal agence — "Candidatures en attente pour un bien" :**
```
GET /v1/tenants?propertyId=<uuid-bien>&withoutContract=true&status=PENDING_REVIEW
```
→ Retourne tous les dossiers locataires soumis pour ce bien, qualifiés ou en attente, qui n'ont pas encore de contrat généré. L'agence peut alors statuer sur chacun (qualifier ou rejeter).

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "TENANT_GET_LIST_SUCCESS",
  "data": {
    "content": [
      {
        "id": "uuid",
        "propertyId": "uuid-bien-cible",
        "userId": "uuid",
        "fullName": "Amara Koné",
        "email": "amara.kone@example.ci",
        "employmentStatus": "EMPLOYEE",
        "monthlyIncome": 500000,
        "qualified": false,
        "status": "PENDING_REVIEW",
        "createdAt": "2026-05-16T10:00:00"
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
- [ ] Tableau paginé avec colonnes : nom du client, bien ciblé (`propertyId`), situation professionnelle, revenu mensuel, statut (badge), date soumission, actions
- [ ] Filtre par statut (`INCOMPLETE` / `PENDING_REVIEW` / `QUALIFIED` / `REJECTED`)
- [ ] Filtre par bien (`propertyId`) — sélecteur ou champ de recherche par UUID/titre du bien
- [ ] Vue "Candidatures en attente" : appel avec `?propertyId=xxx&withoutContract=true` — affiche uniquement les candidats à statuer pour le bien sélectionné
- [ ] PARTNER : ne voit que les dossiers rattachés aux biens de son agence
- [ ] Bouton « Qualifier » visible si `status = PENDING_REVIEW`
- [ ] Bouton « Rejeter » visible si `status = PENDING_REVIEW`
- [ ] Lien vers le détail du dossier
- [ ] Indicateur visuel distinct pour la vue "sans contrat" (ex. badge « En attente de décision »)

---

### UBAX-FE-906 · Qualifier un dossier

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/tenants/{id}/qualify` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` ou `UBAX_ADMIN` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "TENANT_QUALIFY_SUCCESS",
  "data": { "id": "uuid", "status": "QUALIFIED" }
}
```

**Erreurs possibles :**
- `400 Bad Request` — dossier non en `PENDING_REVIEW`
- `404 Not Found` — dossier introuvable

**Critères d'acceptation :**
- [ ] Confirmation avant qualification (modal ou bouton avec confirmation)
- [ ] Badge statut mis à jour en `QUALIFIED` (vert) sans rechargement de page
- [ ] Toast de succès
- [ ] Boutons « Qualifier » / « Rejeter » désactivés après action

---

### UBAX-FE-907 · Rejeter un dossier

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/tenants/{id}/reject` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER` ou `UBAX_ADMIN` |
| **Content-Type** | `application/json` |

**Request body :**
```json
{
  "reason": "Revenus insuffisants pour couvrir le loyer demandé"
}
```

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "TENANT_REJECT_SUCCESS",
  "data": { "id": "uuid", "status": "REJECTED", "rejectionReason": "Revenus insuffisants..." }
}
```

**Erreurs possibles :**
- `400 Bad Request` — dossier non en `PENDING_REVIEW` ou motif vide
- `404 Not Found` — dossier introuvable

**Critères d'acceptation :**
- [ ] Modal de rejet avec champ texte pour le motif (obligatoire)
- [ ] Badge statut mis à jour en `REJECTED` (rouge) sans rechargement de page
- [ ] Motif de rejet visible dans le tableau et le détail
- [ ] Toast de succès avec le motif affiché

---

---

## MODULE 10 — CONTRATS · PORTAIL AGENCE / PROPRIÉTAIRE

**Epic :** `UBAX-FE-CONTRATS`  
**Rôle requis (JWT) :** `UBAX_PARTNER` · `UBAX_OWNER` · `UBAX_ADMIN` · `UBAX_SUPER_ADMIN` (selon l'action)

### Objectif

Ce module couvre la gestion complète du cycle de vie des contrats (bail, vente, location-vente, réservation) depuis leur création jusqu'à leur résiliation ou annulation. L'activation d'un contrat déclenche automatiquement la génération du premier paiement de loyer.

> ⚠️ **Le type `MANDATE` (mandat de gestion agence↔bailleur) n'est PAS géré ici.** Utiliser exclusivement `POST /v1/mandates` — voir MODULE 3 UBAX-FE-307.

**Flux principal :**
1. Un partenaire/propriétaire crée un contrat en brouillon (`DRAFT`) et sélectionne le bien + le locataire.
2. Il soumet le contrat (`DRAFT` → `PENDING_SIGNATURE`) — le PDF est généré automatiquement.
3. Une fois signé physiquement, il (ou un admin) active le contrat (`PENDING_SIGNATURE` → `ACTIVE`) — le premier loyer est créé.
4. En cours de bail, le contrat peut être résilié (`ACTIVE` → `TERMINATED`) ou annulé avant signature (`DRAFT` / `PENDING_SIGNATURE` → `CANCELLED`).

**Périmètre frontend de ce module :**

| Tâche | Action | Acteur |
|-------|--------|--------|
| UBAX-FE-1001 — Liste des contrats | Lecture | `PARTNER` · `OWNER` · `ADMIN` |
| UBAX-FE-1002 — Détail d'un contrat | Lecture | `PARTNER` · `OWNER` · `ADMIN` · `CLIENT` (lien seul) |
| UBAX-FE-1003 — Créer un contrat | Écriture | `PARTNER` · `OWNER` · `ADMIN` |
| UBAX-FE-1004 — Modifier un contrat | Écriture | `PARTNER` · `OWNER` · `ADMIN` |
| UBAX-FE-1005 — Soumettre un contrat | Écriture | `PARTNER` · `OWNER` · `ADMIN` |
| UBAX-FE-1006 — Activer un contrat | Écriture | `PARTNER` · `OWNER` · `ADMIN` |
| UBAX-FE-1007 — Résilier un contrat | Écriture | `PARTNER` · `OWNER` · `ADMIN` |
| UBAX-FE-1008 — Annuler un contrat | Écriture | `PARTNER` · `OWNER` · `ADMIN` |
| UBAX-FE-1009 — KPIs contrats | Lecture | `PARTNER` · `OWNER` · `ADMIN` |

**Types de contrat supportés :**

| `contractType` | Description | Champs obligatoires (validés backend) | Notes |
|----------------|-------------|---------------------------------------|-------|
| `LEASE` | Bail de location | `tenantId`, `monthlyRent` | `depositAmount` recommandé (`monthlyRent × 2`) · `endDate` optionnel (reconduction tacite) · activate → 1er loyer créé |
| `SALE` | Acte de vente | `tenantId` (acheteur), `salePrice` | L'acheteur est obligatoire pour générer l'acte — contrat one-shot, pas de loyer récurrent |
| `RENT_TO_OWN` | Location-vente | `tenantId`, `salePrice`, `monthlyInstallment`, `endDate` OU `durationYears` | `endDate` **ou** `durationYears` (1-30 ans) **obligatoire** — le backend calcule `endDate = startDate + durationYears` si seul `durationYears` fourni · `depositAmount` recommandé (`monthlyInstallment × 6`) · afficher récap *"X XOF × N mois = Y XOF"* |
| `RESERVATION` | Réservation | `reservationDeposit` | `reservationDurationDays` recommandé (défaut 30 j) · bloque le bien le temps de confirmer |
| ~~`MANDATE`~~ | ~~Mandat de gestion~~ | — | **Bloqué sur cet endpoint** — utiliser `POST /v1/mandates` (MODULE 3 UBAX-FE-307) |

**Points d'attention :**
- Le PDF de contrat est généré automatiquement à la soumission (`/submit`) — afficher un lien de téléchargement depuis la réponse.
- L'activation (`/activate`) crée le **premier loyer** (paiement) automatiquement — ne pas l'appeler deux fois.
- Seul le statut `DRAFT` est modifiable — désactiver le formulaire d'édition pour tout autre statut.
- Un `CLIENT` peut consulter le détail d'un contrat (`GET /v1/contracts/{id}`) mais n'accède pas à la liste.
- Les KPIs (`GET /v1/contracts/stats`) doivent être appelés **avant** la liste (route `/stats` déclarée avant `/{id}` dans Spring).
- **`RENT_TO_OWN` :** le formulaire doit afficher `salePrice` + `monthlyInstallment` + `endDate` (obligatoire) à la place des champs `LEASE`. Masquer `monthlyRent`.
- **`SALE` :** `tenantId` (acheteur) est **obligatoire** — sans acheteur identifié, l'acte de vente ne peut pas être généré. Le sélecteur de locataire doit rester visible pour le type SALE.
- **`MANDATE` est bloqué** sur `POST /v1/contracts` — le backend retourne `400`. Utiliser `POST /v1/mandates` pour les mandats agence↔bailleur. Ne pas exposer `MANDATE` dans le select `contractType` de ce formulaire.
- **Conflits (409)** : le backend bloque si un contrat `ACTIVE` ou `PENDING_SIGNATURE` existe déjà sur le même bien (conflit propriété), ou si le même locataire a déjà un contrat `DRAFT`/`PENDING_SIGNATURE`/`ACTIVE` sur ce bien (conflit locataire+bien).

---

### UBAX-FE-1001 · Liste des contrats

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/contracts` |
| **Auth** | Bearer token · Rôle `UBAX_PARTNER`, `UBAX_OWNER`, `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |
| **Query params** | `status` (optionnel) · `search` (optionnel) · `page` (défaut `0`) · `size` (défaut `20`) · `sort=createdAt,desc` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "CONTRACT_GET_LIST_SUCCESS",
  "data": {
    "content": [
      {
        "id": "uuid",
        "referenceNumber": "CTR-20260516-0001",
        "propertyId": "uuid",
        "propertyTitle": "Villa F5 Almadies",
        "ownerId": "uuid",
        "ownerName": "Mamadou Diallo",
        "tenantId": "uuid",
        "tenantName": "Amara Koné",
        "contractType": "LEASE",
        "status": "ACTIVE",
        "startDate": "2026-06-01",
        "endDate": "2027-05-31",
        "rentAmount": 450000,
        "createdAt": "2026-05-16T10:00:00"
      }
    ],
    "totalElements": 12,
    "totalPages": 1,
    "size": 20,
    "number": 0
  }
}
```

**Critères d'acceptation :**
- [ ] Tableau paginé : référence, bien, locataire, type, statut (badge coloré), date début, loyer, actions
- [ ] Barre de recherche (filtre `search` : titre bien, référence, nom locataire)
- [ ] Filtre par statut (`DRAFT` / `PENDING_SIGNATURE` / `ACTIVE` / `TERMINATED` / `CANCELLED`)
- [ ] Bouton « Nouveau contrat » → formulaire UBAX-FE-1003
- [ ] Actions contextuelles par statut (voir tableau ci-dessous)
- [ ] Loader squelette pendant la requête

**Actions disponibles par statut :**

| Statut | Actions |
|--------|---------|
| `DRAFT` | Modifier · Soumettre · Annuler |
| `PENDING_SIGNATURE` | Activer · Annuler |
| `ACTIVE` | Voir détail · Résilier |
| `TERMINATED` / `CANCELLED` | Voir détail uniquement |

---

### UBAX-FE-1002 · Détail d'un contrat

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/contracts/{id}` |
| **Auth** | Bearer token · `UBAX_PARTNER`, `UBAX_OWNER`, `UBAX_ADMIN`, `UBAX_SUPER_ADMIN` ou `UBAX_CLIENT` (reporter) |
| **Path params** | `id` : UUID du contrat |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "CONTRACT_GET_SUCCESS",
  "data": {
    "id": "uuid",
    "referenceNumber": "CTR-20260516-0001",
    "propertyId": "uuid",
    "propertyTitle": "Villa F5 Almadies",
    "ownerId": "uuid",
    "ownerName": "Mamadou Diallo",
    "tenantId": "uuid",
    "tenantName": "Amara Koné",
    "contractType": "RENT_TO_OWN",
    "status": "ACTIVE",
    "startDate": "2026-06-01",
    "endDate": "2031-06-01",
    "monthlyRent": null,
    "salePrice": 12000000,
    "monthlyInstallment": 200000,
    "depositAmount": 1200000,
    "depositReturned": false,
    "paymentDay": 5,
    "terminationReason": null,
    "fileUrl": "http://localhost:9000/documents-generated/uuid/contract.pdf",
    "signedFileUrl": null,
    "createdAt": "2026-05-16T10:00:00",
    "updatedAt": "2026-05-16T10:00:00"
  }
}
```

**Erreurs possibles :**
- `403 Forbidden` — CLIENT qui n'est pas le locataire du contrat
- `404 Not Found` — contrat introuvable

**Critères d'acceptation :**
- [ ] Fiche complète : référence, bien, propriétaire, locataire, type, statut, dates, montants
- [ ] **Affichage conditionnel selon `contractType` :**
  - `LEASE` → afficher `monthlyRent`, `depositAmount`
  - `SALE` → afficher `salePrice`, `tenantName` (acheteur)
  - `RENT_TO_OWN` → afficher `salePrice` (prix total), `monthlyInstallment` (mensualité), `endDate` (fin du programme) + indicateur de progression (mois écoulés / durée totale)
  - `RESERVATION` → afficher `reservationDeposit`, `reservationDurationDays`
- [ ] Bouton « Télécharger le contrat PDF » si `fileUrl` non null (URL privée — presign de lecture)
- [ ] Badge statut coloré
- [ ] Boutons d'action contextuels selon statut et rôle (identiques à UBAX-FE-1001)
- [ ] Historique des paiements lié (lien vers le module Payment si disponible)

---

### UBAX-FE-1003 · Créer un contrat

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/contracts` |
| **Auth** | Bearer token · `UBAX_PARTNER`, `UBAX_OWNER`, `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |
| **Content-Type** | `application/json` |

**Request body — LEASE :**
```json
{
  "propertyId": "394e1b94-6d87-41b2-8b31-031a9f45944d",
  "tenantId": "uuid-du-dossier-locataire",
  "contractType": "LEASE",
  "startDate": "2026-06-01",
  "endDate": "2027-05-31",
  "monthlyRent": 450000,
  "depositAmount": 900000,
  "paymentDay": 5
}
```

**Request body — RENT_TO_OWN (avec `endDate` explicite) :**
```json
{
  "propertyId": "394e1b94-6d87-41b2-8b31-031a9f45944d",
  "tenantId": "uuid-du-dossier-locataire",
  "contractType": "RENT_TO_OWN",
  "startDate": "2026-06-01",
  "endDate": "2031-06-01",
  "salePrice": 12000000,
  "monthlyInstallment": 200000,
  "depositAmount": 1200000,
  "paymentDay": 5
}
```

**Request body — RENT_TO_OWN (avec `durationYears` — alternative à `endDate`) :**
```json
{
  "propertyId": "394e1b94-6d87-41b2-8b31-031a9f45944d",
  "tenantId": "uuid-du-dossier-locataire",
  "contractType": "RENT_TO_OWN",
  "startDate": "2026-06-01",
  "durationYears": 5,
  "salePrice": 12000000,
  "monthlyInstallment": 200000,
  "depositAmount": 1200000,
  "paymentDay": 5
}
```

> **`durationYears`** : entier entre 1 et 30 — le backend calcule `endDate = startDate + durationYears` et stocke la date calculée. Passer `endDate` **ou** `durationYears`, pas les deux (si les deux sont fournis, `endDate` est prioritaire).

**Request body — SALE :**
```json
{
  "propertyId": "394e1b94-6d87-41b2-8b31-031a9f45944d",
  "tenantId": "uuid-du-dossier-acheteur",
  "contractType": "SALE",
  "startDate": "2026-06-01",
  "salePrice": 25000000
}
```

> ⚠️ **`MANDATE` est bloqué sur cet endpoint.** Le backend retourne `400 CONTRACT_CREATE_FAILURE` si `contractType = "MANDATE"`. Utiliser `POST /v1/mandates` — voir UBAX-FE-307.

**Request body — RESERVATION :**
```json
{
  "propertyId": "394e1b94-6d87-41b2-8b31-031a9f45944d",
  "contractType": "RESERVATION",
  "startDate": "2026-06-01",
  "reservationDeposit": 500000,
  "reservationDurationDays": 30
}
```

> **`contractType` disponibles :** `LEASE` · `SALE` · `RENT_TO_OWN` · `RESERVATION` (MANDATE bloqué — utiliser `/v1/mandates`)  
> **`ownerId` :** non envoyé dans le body — le backend le dérive automatiquement depuis `property.owner`. Le propriétaire est toujours celui enregistré sur le bien au moment de sa création.  
> **`tenantId` :** UUID du dossier `Tenant` (KYC), pas l'`userId` — requis pour `LEASE`, `RENT_TO_OWN` et **`SALE`** (acheteur obligatoire pour l'acte de vente)  
> **`endDate` :** optionnel pour `LEASE` (reconduction tacite si absent) ; pour `RENT_TO_OWN`, **`endDate` ou `durationYears` est obligatoire** (pas les deux — si les deux sont fournis, `endDate` est prioritaire)

**Champs par type de contrat :**

| Champ | `LEASE` | `SALE` | `RENT_TO_OWN` | `RESERVATION` |
|-------|---------|--------|---------------|---------------|
| `tenantId` | ✅ requis | ✅ requis (acheteur) | ✅ requis | — |
| `monthlyRent` | ✅ | — | — | — |
| `salePrice` | — | ✅ | ✅ prix total | — |
| `monthlyInstallment` | — | — | ✅ mensualité | — |
| `depositAmount` | ✅ caution | — | ✅ apport | — |
| `endDate` | optionnel | — | `endDate` OU `durationYears` | — |
| `durationYears` | — | — | `endDate` OU `durationYears` (1-30) | — |
| `reservationDeposit` | — | — | — | ✅ |
| `reservationDurationDays` | — | — | — | ✅ |
| `specialClauses` | optionnel | optionnel | optionnel | optionnel |
| `terminationConditions` | optionnel | — | optionnel | — |

> La colonne `MANDATE` a été supprimée — ce type est bloqué sur cet endpoint. Voir `POST /v1/mandates`.

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "CONTRACT_CREATE_SUCCESS",
  "data": {
    "id": "uuid",
    "referenceNumber": "CTR-20260516-0001",
    "propertyTitle": "Villa F5 Almadies",
    "tenantName": "Amara Koné",
    "contractType": "RENT_TO_OWN",
    "status": "DRAFT",
    "startDate": "2026-06-01",
    "endDate": "2031-06-01",
    "salePrice": 12000000,
    "monthlyInstallment": 200000,
    "depositAmount": 1200000,
    "createdAt": "2026-05-16T10:00:00"
  }
}
```

**Validations backend par type (`400` si champ manquant) :**

| Type | Champs obligatoires vérifiés par le backend |
|------|---------------------------------------------|
| `LEASE` | `tenantId`, `monthlyRent` |
| `SALE` | `tenantId` (acheteur), `salePrice` |
| `RENT_TO_OWN` | `tenantId`, `salePrice`, `monthlyInstallment`, (`endDate` OU `durationYears`) |
| `RESERVATION` | `reservationDeposit` |
| `MANDATE` | **Bloqué** — retourne `400 CONTRACT_CREATE_FAILURE` → utiliser `POST /v1/mandates` |

**Erreurs possibles :**

| Code HTTP | Message | Cause |
|-----------|---------|-------|
| `400` | `CONTRACT_CREATE_FAILURE` | Champ obligatoire manquant, type invalide, dates invalides, ou `contractType = MANDATE` |
| `404` | `CONTRACT_NOT_FOUND` | Bien ou locataire introuvable |
| `409` | `CONTRACT_CREATE_FAILURE_PROPERTY_CONFLICT` | Un contrat `ACTIVE` ou `PENDING_SIGNATURE` existe déjà sur ce bien |
| `409` | `CONTRACT_CREATE_FAILURE_TENANT_CONFLICT` | Ce locataire a déjà un contrat `DRAFT`/`PENDING_SIGNATURE`/`ACTIVE` sur ce bien |

---

#### Auto-remplissage du formulaire depuis l'API

**Étape 1 — Sélection du bien** → `GET /v1/properties/{id}`

Les champs suivants de `PropertyResponse` permettent de pré-remplir le formulaire :

| Champ `PropertyResponse` | Utilisation dans le formulaire |
|--------------------------|-------------------------------|
| `ownerId` / `ownerName` | Affichage informatif uniquement — **non envoyé dans le body** (le backend le dérive depuis le bien) |
| `price` | → `salePrice` si `transactionType = SALE`<br>→ `monthlyRent` si `transactionType = RENT` ou `RENT_FURNISHED`<br>→ valeur de base pour le calcul `reservationDeposit` (`price × 5%`) |
| `transactionType` | → aide à pré-sélectionner le `contractType` recommandé |

> ⚠️ `PropertyResponse` n'a pas de champs `monthly_rent_estimate` ni `deposit_amount_estimate` séparés — `price` est le seul champ prix. Appliquer les calculs dérivés décrits ci-dessous.

**Étape 2 — Valeurs pré-remplies par type de contrat**

| Type | Champ | Source / Calcul |
|------|-------|-----------------|
| **LEASE** | `monthlyRent` | `property.price` (si `transactionType = RENT`) |
| | `depositAmount` | `monthlyRent × 2` (calcul frontend) |
| | `startDate` | Date du jour |
| | `endDate` | `startDate + 1 an` (optionnel, modifiable) |
| | `paymentDay` | `5` (défaut métier) |
| **SALE** | `salePrice` | `property.price` (si `transactionType = SALE`) |
| | `startDate` | Date du jour |
| **RENT_TO_OWN** | `salePrice` | `property.price` (prix total du bien) |
| | `monthlyInstallment` | `salePrice ÷ 60` (5 ans, modifiable) |
| | `depositAmount` | `monthlyInstallment × 6` (modifiable) |
| | `startDate` | Date du jour |
| | `endDate` | `startDate + 5 ans` (modifiable — alternatif à `durationYears`) |
| | `durationYears` | `5` (défaut — alternatif à `endDate`, 1-30 ans, modifiable) |
| | `paymentDay` | `5` (défaut) |
| **RESERVATION** | `reservationDeposit` | `property.price × 5%` (modifiable) |
| | `reservationDurationDays` | `30` (défaut) |
> **Pour `RENT_TO_OWN`** — afficher un récapitulatif indicatif recalculé à la volée :  
> *« 200 000 XOF/mois × 60 mois = 12 000 000 XOF »* → recalculer si `monthlyInstallment` ou `endDate` changent.

> **Règle de non-écrasement** : une fois qu'un champ a été modifié manuellement par l'utilisateur, ne plus l'écraser lors d'un changement de type de contrat. Stocker un flag `userEdited` par champ dans l'état local du formulaire.

**Critères d'acceptation :**
- [ ] Formulaire multi-étapes : bien → locataire (pré-rempli depuis dossier KYC) → type → conditions
- [ ] Sélecteur de locataire : appel `GET /v1/tenants?status=QUALIFIED&withoutContract=true` — locataires qualifiés sans contrat actif
- [ ] Sélecteur de type de contrat (`contractType`) — 4 valeurs : LEASE, SALE, RENT_TO_OWN, RESERVATION (**ne pas inclure MANDATE** — utiliser `/v1/mandates`)
- [ ] **Formulaire dynamique selon `contractType` :**
  - `LEASE` → afficher `tenantId`, `monthlyRent`, `depositAmount`, `endDate` (optionnel), `paymentDay`
  - `SALE` → afficher `tenantId` (acheteur, obligatoire), `salePrice`
  - `RENT_TO_OWN` → afficher `tenantId`, `salePrice` (prix total du bien), `monthlyInstallment` (mensualité), `endDate` **ou** `durationYears` (1-30 ans, **l'un des deux est obligatoire**), `depositAmount`, `paymentDay`
  - `RESERVATION` → afficher `reservationDeposit`, `reservationDurationDays`
- [ ] Pour `RENT_TO_OWN` : afficher un récapitulatif indicatif recalculé en temps réel — ex. « 200 000 XOF/mois × 60 mois = 12 000 000 XOF » ; recalculer si `monthlyInstallment`, `endDate` ou `durationYears` changent
- [ ] Pour `RENT_TO_OWN` : proposer deux modes de saisie de la durée — sélecteur « N années » (`durationYears`) ou date-picker explicite (`endDate`)
- [ ] Champs de dates avec date-picker (start, end)
- [ ] Champ `paymentDay` (1–28) pour le jour d'échéance mensuel
- [ ] Redirection vers le détail du contrat créé après succès

**Autoremplissage depuis le bien sélectionné :**
- [ ] À la sélection du bien : afficher le nom du propriétaire (`property.ownerName`) à titre informatif — **ne pas inclure `ownerId` dans le body envoyé** (le backend le dérive automatiquement depuis le bien)
- [ ] À la sélection du bien : suggérer automatiquement le `contractType` recommandé selon `property.transactionType` :
  - `RENT` / `RENT_FURNISHED` → suggérer `LEASE`
  - `SALE` → suggérer `SALE`
  - `SHORT_STAY` → suggérer `RESERVATION`
- [ ] Pré-remplissage automatique des montants et dates dès la sélection du type :
  - `LEASE` → `monthlyRent = property.price` · `depositAmount = monthlyRent × 2` · `startDate = today` · `endDate = startDate + 1 an` · `paymentDay = 5`
  - `SALE` → `salePrice = property.price` · `startDate = today`
  - `RENT_TO_OWN` → `salePrice = property.price` · `monthlyInstallment = salePrice ÷ 60` · `depositAmount = monthlyInstallment × 6` · `startDate = today` · `durationYears = 5` (ou `endDate = startDate + 5 ans`) · `paymentDay = 5`
  - `RESERVATION` → `reservationDeposit = property.price × 5%` · `reservationDurationDays = 30` · `startDate = today`
- [ ] Flag `userEdited` par champ : un champ modifié manuellement par l'utilisateur n'est plus écrasé lors d'un changement de `contractType` — stocker le flag dans l'état local du formulaire

---

### UBAX-FE-1004 · Modifier un contrat (DRAFT uniquement)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PUT /v1/contracts/{id}` |
| **Auth** | Bearer token · `UBAX_PARTNER`, `UBAX_OWNER`, `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |
| **Content-Type** | `application/json` |

**Request body :** identique à UBAX-FE-1003 (même schéma `CreateContractRequest`).

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "CONTRACT_UPDATE_SUCCESS",
  "data": { "id": "uuid", "status": "DRAFT", "rentAmount": 500000 }
}
```

**Erreurs possibles :**
- `400 Bad Request` — contrat non en statut `DRAFT`
- `403 Forbidden` — accès refusé (contrat d'une autre agence)
- `404 Not Found` — contrat introuvable

**Critères d'acceptation :**
- [ ] Formulaire pré-rempli avec les valeurs actuelles
- [ ] Bouton « Modifier » affiché uniquement si `status = DRAFT`
- [ ] Désactiver / masquer le formulaire si le contrat n'est plus en `DRAFT`
- [ ] Toast de succès après modification

---

### UBAX-FE-1005 · Soumettre un contrat pour signature

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/contracts/{id}/submit` |
| **Auth** | Bearer token · `UBAX_PARTNER`, `UBAX_OWNER`, `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "CONTRACT_UPDATE_SUCCESS",
  "data": {
    "id": "uuid",
    "status": "PENDING_SIGNATURE",
    "pdfUrl": "http://localhost:9000/documents-generated/uuid/contract.pdf"
  }
}
```

> **PDF généré :** le backend génère automatiquement le PDF du contrat via Thymeleaf → MinIO. `pdfUrl` est disponible dans la réponse.

**Erreurs possibles :**
- `400 Bad Request` — contrat non en statut `DRAFT`
- `404 Not Found` — contrat introuvable

**Critères d'acceptation :**
- [ ] Bouton « Soumettre pour signature » visible uniquement si `status = DRAFT`
- [ ] Modal de confirmation avant soumission
- [ ] Après succès : badge passe à `PENDING_SIGNATURE` (jaune/orange)
- [ ] Lien « Télécharger le PDF » affiché si `pdfUrl` présent dans la réponse

---

### UBAX-FE-1006 · Activer un contrat

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/contracts/{id}/activate` |
| **Auth** | Bearer token · `UBAX_PARTNER`, `UBAX_OWNER`, `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "CONTRACT_UPDATE_SUCCESS",
  "data": {
    "id": "uuid",
    "status": "ACTIVE"
  }
}
```

> **Premier loyer créé :** l'activation génère automatiquement le premier paiement de loyer dans le module Payment.

**Erreurs possibles :**
- `400 Bad Request` — contrat non en statut `PENDING_SIGNATURE`
- `404 Not Found` — contrat introuvable

**Critères d'acceptation :**
- [ ] Bouton « Activer le contrat » visible uniquement si `status = PENDING_SIGNATURE`
- [ ] Modal de confirmation : « Activer le contrat créera automatiquement le premier paiement de loyer »
- [ ] Badge passe à `ACTIVE` (vert) après succès
- [ ] Toast de succès
- [ ] Bouton désactivé après activation (idempotence côté UI)

---

### UBAX-FE-1007 · Résilier un contrat

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/contracts/{id}/terminate` |
| **Auth** | Bearer token · `UBAX_PARTNER`, `UBAX_OWNER`, `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |
| **Content-Type** | `application/json` |

**Request body :**
```json
{
  "reason": "Fin de bail arrivée à son terme — locataire ne renouvelle pas"
}
```

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "CONTRACT_UPDATE_SUCCESS",
  "data": {
    "id": "uuid",
    "status": "TERMINATED",
    "terminationReason": "Fin de bail arrivée à son terme — locataire ne renouvelle pas"
  }
}
```

**Erreurs possibles :**
- `400 Bad Request` — contrat non en statut `ACTIVE`
- `404 Not Found` — contrat introuvable

**Critères d'acceptation :**
- [ ] Bouton « Résilier » visible uniquement si `status = ACTIVE`
- [ ] Modal de résiliation avec champ texte pour le motif (obligatoire)
- [ ] Badge passe à `TERMINATED` (noir/gris foncé) après succès
- [ ] Toast de confirmation de résiliation

---

### UBAX-FE-1008 · Annuler un contrat

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/contracts/{id}/cancel` |
| **Auth** | Bearer token · `UBAX_PARTNER`, `UBAX_OWNER`, `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "CONTRACT_UPDATE_SUCCESS",
  "data": { "id": "uuid", "status": "CANCELLED" }
}
```

**Erreurs possibles :**
- `400 Bad Request` — contrat en statut `ACTIVE`, `TERMINATED` ou `CANCELLED` (annulation impossible)
- `404 Not Found` — contrat introuvable

**Critères d'acceptation :**
- [ ] Bouton « Annuler » visible si `status ∈ {DRAFT, PENDING_SIGNATURE}`
- [ ] Modal de confirmation avant annulation
- [ ] Badge passe à `CANCELLED` (rouge pâle) après succès
- [ ] Toast de succès

---

### UBAX-FE-1009 · KPIs contrats (statistiques)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/contracts/stats` |
| **Auth** | Bearer token · `UBAX_PARTNER`, `UBAX_OWNER`, `UBAX_ADMIN` ou `UBAX_SUPER_ADMIN` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "CONTRACT_GET_STATS_SUCCESS",
  "data": {
    "total": 45,
    "active": 32,
    "pendingSignature": 5,
    "terminated": 6,
    "cancelled": 2,
    "draft": 0
  }
}
```

**Critères d'acceptation :**
- [ ] Cartes KPI en haut du tableau de bord contrats : Total · Actifs · En attente · Résiliés · Annulés
- [ ] Cartes cliquables pour filtrer la liste par statut correspondant
- [ ] Couleurs cohérentes avec les badges de statut
- [ ] Appel effectué au montage du composant, données rafraîchies après chaque action

---

### Statuts des contrats (`ContractStatus`)

| Valeur | Libellé UI | Couleur badge |
|--------|------------|---------------|
| `DRAFT` | Brouillon | Gris `#6B7280` |
| `PENDING_SIGNATURE` | En attente de signature | Orange `#F97316` |
| `ACTIVE` | Actif | Vert `#16A34A` |
| `TERMINATED` | Résilié | Noir `#111827` |
| `CANCELLED` | Annulé | Rouge `#DC2626` |

### Statuts des dossiers locataires (`TenantStatus`)

| Valeur | Libellé UI | Couleur badge |
|--------|------------|---------------|
| `INCOMPLETE` | Incomplet | Gris `#6B7280` |
| `PENDING_REVIEW` | En cours de révision | Orange `#F97316` |
| `QUALIFIED` | Qualifié | Vert `#16A34A` |
| `REJECTED` | Rejeté | Rouge `#DC2626` |
| `ARCHIVED` | Archivé | Noir `#111827` |

---

---

## MODULE 11 — PAIEMENTS · PORTAIL AGENCE

**Epic :** `UBAX-FE-PAIEMENTS`  
**Rôle requis (JWT) :** `UBAX_PARTNER` · `UBAX_ADMIN` · `UBAX_SUPER_ADMIN`

### Objectif

L'agence enregistre et suit les paiements reçus de ses locataires (loyers, cautions, commissions) et ses dépenses comptables. Le module permet de visualiser l'historique, d'identifier les retards, d'enregistrer un encaissement manuel, de mettre à jour le statut d'un paiement, et de saisir les dépenses de l'agence.

**Flux principal :**
1. Le système génère automatiquement les échéances de loyer lors de l'activation d'un contrat (bail `LEASE`).
2. Le comptable ou directeur enregistre les encaissements reçus (`POST /v1/payments`).
3. L'historique paginé et filtrable permet de suivre l'état de chaque paiement.
4. Les loyers impayés apparaissent dans la vue « Retards » (`GET /v1/payments/late`).
5. Les dépenses sont saisies séparément (`POST /v1/expenses`) avec ventilation par catégorie et centre de coût.

**Périmètre frontend de ce module :**

| Tâche | SCRUM | Action | Acteur |
|-------|-------|--------|--------|
| UBAX-FE-911 — Liste des paiements / loyers | SCRUM-216 | Lecture | `PARTNER` · `ADMIN` |
| UBAX-FE-912 — Loyers en retard | SCRUM-217 | Lecture | `PARTNER` · `ADMIN` |
| UBAX-FE-913 — Enregistrer un paiement | SCRUM-218 | Écriture | `PARTNER` · `ADMIN` |
| UBAX-FE-914 — Mettre à jour le statut d'un paiement | SCRUM-219 | Écriture | `PARTNER` · `ADMIN` |
| UBAX-FE-915 — Liste et ajout d'une dépense agence | SCRUM-220 | Écriture | `PARTNER` · `ADMIN` |

**Points d'attention :**
- Un paiement `PAID` **ne peut pas être supprimé** — désactiver le bouton de suppression.
- Le statut est calculé **automatiquement** à la création : `PAID` si `paidDate` + `amountPaid >= amount`, `PARTIAL` si paiement incomplet, `LATE` si échéance dépassée sans paiement, sinon `PENDING`.
- Champs obligatoires à la création : `paymentType`, `amount`, `dueDate`.
- Lier systématiquement `contractId` quand le paiement concerne un bail — cela permet la cohérence des rapports.
- `periodLabel` est un libellé libre (ex. `"Juillet 2026"`) — pas de dates ISO start/end.
- `paidDate` = date d'encaissement effectif. Peut être null si le paiement n'est pas encore reçu.
- Pour les dépenses, si `costCenter = PROPERTY_SPECIFIC`, le champ `propertyId` est **obligatoire**.

---

### UBAX-FE-911 · Liste des paiements / loyers (SCRUM-216)

| Champ | Valeur |
|-------|--------|
| **Endpoints** | `GET /v1/payments` · `GET /v1/payments/{id}` |
| **Auth** | Bearer token · `UBAX_PARTNER` · `UBAX_ADMIN` · `UBAX_SUPER_ADMIN` |
| **Query params** | Voir tableau ci-dessous |

**Query params (tous optionnels) :**

| Paramètre | Type | Description | Exemple |
|-----------|------|-------------|---------|
| `status` | `string` | Filtre par statut | `PENDING` |
| `type` | `string` | Filtre par type de paiement | `RENT` |
| `propertyId` | `UUID` | Filtre par bien | `uuid` |
| `contractId` | `UUID` | Filtre par contrat | `uuid` |
| `tenantId` | `UUID` | Filtre par dossier locataire | `uuid` |
| `from` | `date` | Date d'échéance ≥ (format `YYYY-MM-DD`) | `2026-01-01` |
| `to` | `date` | Date d'échéance ≤ (format `YYYY-MM-DD`) | `2026-12-31` |
| `page` | `int` | Numéro de page (défaut `0`) | `0` |
| `size` | `int` | Taille de page (défaut `20`) | `20` |
| `sort` | `string` | Tri (défaut `dueDate,desc`) | `dueDate,asc` |

**Response liste `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "PAYMENT_GET_LIST_SUCCESS",
  "data": {
    "content": [
      {
        "id": "uuid",
        "contractId": "uuid",
        "tenantId": "uuid",
        "propertyId": "uuid",
        "paymentType": "RENT",
        "paymentMethod": "MOBILE_MONEY",
        "status": "PAID",
        "amount": 2150000,
        "amountPaid": 2150000,
        "dueDate": "2026-07-05",
        "paidDate": "2026-07-01",
        "periodLabel": "Juillet 2026",
        "reference": "WAVE-2026070001",
        "overdue": false,
        "note": "Loyer + charges juillet 2026",
        "createdAt": "2026-05-16T10:00:00"
      }
    ],
    "totalElements": 24,
    "totalPages": 2,
    "size": 20,
    "number": 0
  }
}
```

**Response détail `200` (`GET /v1/payments/{id}`) :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "PAYMENT_GET_SUCCESS",
  "data": {
    "id": "uuid",
    "contractId": "1abf05cb-71a1-4be8-9a2e-a952ddd36e4d",
    "tenantId": "8b716e32-6926-4397-9837-580538a48059",
    "agencyId": "uuid",
    "propertyId": "394e1b94-6d87-41b2-8b31-031a9f45944d",
    "recordedById": "uuid",
    "recordedByName": "Mamadou Diallo",
    "paymentType": "RENT",
    "paymentMethod": "MOBILE_MONEY",
    "status": "PAID",
    "amount": 2150000,
    "amountPaid": 2150000,
    "dueDate": "2026-07-05",
    "paidDate": "2026-07-01",
    "periodLabel": "Juillet 2026",
    "reference": "WAVE-2026070001",
    "receiptUrl": null,
    "note": "Loyer + charges juillet 2026",
    "overdue": false,
    "createdAt": "2026-05-16T10:00:00",
    "updatedAt": "2026-05-16T10:00:00"
  }
}
```

**Erreurs possibles :**
- `403 Forbidden` — le paiement appartient à une autre agence
- `404 Not Found` — paiement introuvable

**Critères d'acceptation :**
- [ ] Tableau paginé : référence, bien, locataire, type, statut (badge), montant dû, montant payé, échéance, mode, période
- [ ] Filtres : statut · type · bien · contrat · locataire · plage de dates
- [ ] Badge `overdue: true` → fond rouge même si statut `PENDING`
- [ ] Clic sur une ligne → fiche détail (`GET /v1/payments/{id}`) avec tous les champs
- [ ] Fiche détail : lien vers le contrat lié (`contractId`) et le dossier locataire (`tenantId`)
- [ ] Bouton « Enregistrer un paiement » → formulaire UBAX-FE-913
- [ ] Lien « Retards » → UBAX-FE-912
- [ ] Loader squelette pendant la requête

---

### UBAX-FE-912 · Loyers en retard (SCRUM-217)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/payments/late` |
| **Auth** | Bearer token · `UBAX_PARTNER` · `UBAX_ADMIN` · `UBAX_SUPER_ADMIN` |

> Retourne les paiements dont la date d'échéance est dépassée et dont le statut est `PENDING` ou `PARTIAL`. Triés par échéance croissante (les plus anciens en premier).

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "PAYMENT_GET_LIST_SUCCESS",
  "data": [
    {
      "id": "uuid",
      "contractId": "uuid",
      "tenantId": "uuid",
      "propertyId": "uuid",
      "paymentType": "RENT",
      "status": "PENDING",
      "amount": 2150000,
      "amountPaid": null,
      "dueDate": "2026-06-05",
      "periodLabel": "Juin 2026",
      "overdue": true
    }
  ]
}
```

**Critères d'acceptation :**
- [ ] Section ou onglet dédié « Loyers en retard » dans le tableau de bord financier
- [ ] Badge retard visible (nombre de jours depuis l'échéance)
- [ ] Action rapide « Enregistrer paiement » sur chaque ligne → ouvre UBAX-FE-913 prérempli
- [ ] Compteur total affiché dans les KPIs

---

### UBAX-FE-913 · Enregistrer un paiement (SCRUM-218)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/payments` |
| **Auth** | Bearer token · `UBAX_PARTNER` · `UBAX_ADMIN` · `UBAX_SUPER_ADMIN` |
| **Content-Type** | `application/json` |

**Request body :**
```json
{
  "contractId": "1abf05cb-71a1-4be8-9a2e-a952ddd36e4d",
  "tenantId": "8b716e32-6926-4397-9837-580538a48059",
  "propertyId": "394e1b94-6d87-41b2-8b31-031a9f45944d",
  "paymentType": "RENT",
  "paymentMethod": "MOBILE_MONEY",
  "amount": 2150000,
  "amountPaid": 2150000,
  "dueDate": "2026-07-05",
  "paidDate": "2026-07-01",
  "periodLabel": "Juillet 2026",
  "reference": "WAVE-2026070001",
  "note": "Loyer + charges mois de juillet 2026"
}
```

> **Champs obligatoires :** `paymentType`, `amount`, `dueDate`  
> **`contractId` :** toujours renseigner quand le paiement concerne un bail — assure la cohérence des rapports.  
> **`amountPaid` :** si null ou absent → statut `PENDING`. Si `amountPaid >= amount` + `paidDate` renseigné → statut `PAID` automatiquement.  
> **`paidDate` :** date d'encaissement effectif — différent de `dueDate` (date d'échéance).  
> **`periodLabel` :** libellé libre `"Juillet 2026"` — pas de champs `periodStart`/`periodEnd`.

**`paymentType` disponibles :**

| Valeur | Libellé UI |
|--------|------------|
| `RENT` | Loyer |
| `DEPOSIT` | Caution |
| `AGENCY_FEE` | Frais d'agence |
| `COMMISSION` | Commission |
| `SALE` | Vente |
| `OTHER` | Autre |

**`paymentMethod` disponibles :**

| Valeur | Libellé UI |
|--------|------------|
| `CASH` | Espèces |
| `BANK_TRANSFER` | Virement bancaire |
| `MOBILE_MONEY` | Mobile Money (Wave, Orange Money…) |
| `CHECK` | Chèque |
| `OTHER` | Autre |

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "PAYMENT_CREATE_SUCCESS",
  "data": {
    "id": "uuid",
    "contractId": "1abf05cb-71a1-4be8-9a2e-a952ddd36e4d",
    "paymentType": "RENT",
    "paymentMethod": "MOBILE_MONEY",
    "status": "PAID",
    "amount": 2150000,
    "amountPaid": 2150000,
    "dueDate": "2026-07-05",
    "paidDate": "2026-07-01",
    "periodLabel": "Juillet 2026",
    "reference": "WAVE-2026070001",
    "overdue": false,
    "createdAt": "2026-05-16T18:30:00"
  }
}
```

**Erreurs possibles :**
- `400 Bad Request` — `paymentType`, `amount` ou `dueDate` manquant
- `404 Not Found` — contrat, locataire ou bien introuvable

**Critères d'acceptation :**
- [ ] Formulaire : type · mode · montant dû · montant reçu · échéance · date paiement · période · référence · note
- [ ] Sélecteur de contrat (prérempli si ouvert depuis la fiche contrat ou la liste retards)
- [ ] Champ `amountPaid` optionnel — si omis, statut reste `PENDING`
- [ ] Le statut résultant est affiché dans le toast de confirmation
- [ ] Redirection vers le détail du paiement créé

---

### UBAX-FE-914 · Mettre à jour le statut d'un paiement (SCRUM-219)

| Champ | Valeur |
|-------|--------|
| **Endpoints** | `PATCH /v1/payments/{id}/status` · `DELETE /v1/payments/{id}` |
| **Auth** | Bearer token · `UBAX_PARTNER` · `UBAX_ADMIN` · `UBAX_SUPER_ADMIN` |
| **Path params** | `id` : UUID du paiement |
| **Content-Type** | `application/json` |

**Request body (PATCH) :**
```json
{
  "status": "PAID",
  "paymentMethod": "BANK_TRANSFER",
  "amountPaid": 2150000,
  "paidDate": "2026-07-03",
  "receiptUrl": null,
  "note": "Virement reçu le 3 juillet"
}
```

> **Seul `status` est obligatoire.** Si `status = PARTIAL` et `amountPaid >= amount` → le backend corrige automatiquement en `PAID`.  
> **Transitions autorisées :** `PENDING` → `PAID` · `PARTIAL` · `LATE` · `CANCELLED` | `PARTIAL` → `PAID` · `CANCELLED` | `LATE` → `PAID` · `PARTIAL` · `CANCELLED`.  
> **Un paiement `PAID` ou `CANCELLED` ne peut plus être modifié.**  
> **Suppression (`DELETE`) :** autorisée uniquement si `status ∈ {PENDING, PARTIAL, LATE, CANCELLED}`. Un paiement `PAID` **ne peut pas être supprimé**.

**`status` disponibles pour cette action :**

| Valeur | Libellé UI | Couleur badge |
|--------|------------|---------------|
| `PENDING` | En attente | Orange `#F97316` |
| `PAID` | Payé | Vert `#16A34A` |
| `PARTIAL` | Partiel | Bleu `#3B82F6` |
| `LATE` | En retard | Rouge `#DC2626` |
| `CANCELLED` | Annulé | Gris `#6B7280` |

**Response PATCH `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "PAYMENT_CREATE_SUCCESS",
  "data": {
    "id": "uuid",
    "status": "PAID",
    "amountPaid": 2150000,
    "paidDate": "2026-07-03",
    "paymentMethod": "BANK_TRANSFER",
    "overdue": false
  }
}
```

**Response DELETE `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "PAYMENT_GET_SUCCESS",
  "data": null
}
```

**Erreurs possibles :**
- `400 Bad Request` — transition non autorisée (PATCH) ou tentative de suppression d'un paiement `PAID` (DELETE)
- `404 Not Found` — paiement introuvable

**Critères d'acceptation :**
- [ ] Modale ou drawer « Enregistrer encaissement » avec sélecteur de statut, montant reçu, date, mode
- [ ] Les transitions non autorisées sont désactivées dans le sélecteur de statut
- [ ] Le bouton « Modifier statut » est masqué si `status ∈ {PAID, CANCELLED}`
- [ ] Toast de confirmation avec le nouveau statut
- [ ] Bouton « Supprimer » visible uniquement si `status ≠ PAID`
- [ ] Confirmation modale avant suppression (« Êtes-vous sûr ? Cette action est irréversible. »)
- [ ] Retour à la liste des paiements après suppression

---

### UBAX-FE-915 · Liste et ajout d'une dépense agence (SCRUM-220)

| Champ | Valeur |
|-------|--------|
| **Endpoints** | `GET /v1/expenses` · `POST /v1/expenses` · `DELETE /v1/expenses/{id}` |
| **Auth** | Bearer token · `UBAX_PARTNER` · `UBAX_ADMIN` · `UBAX_SUPER_ADMIN` |
| **Content-Type** | `application/json` |

**Query params liste (tous optionnels) :**

| Paramètre | Type | Description | Exemple |
|-----------|------|-------------|---------|
| `category` | `string` | Filtre par catégorie | `MAINTENANCE` |
| `propertyId` | `UUID` | Filtre par bien | `uuid` |
| `from` | `date` | Date dépense ≥ (format `YYYY-MM-DD`) | `2026-01-01` |
| `to` | `date` | Date dépense ≤ (format `YYYY-MM-DD`) | `2026-12-31` |
| `page` | `int` | Numéro de page (défaut `0`) | `0` |
| `size` | `int` | Taille de page (défaut `20`) | `20` |
| `sort` | `string` | Tri (défaut `expenseDate,desc`) | `amount,desc` |

**Response liste `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "PAYMENT_GET_LIST_SUCCESS",
  "data": {
    "content": [
      {
        "id": "uuid",
        "agencyId": "uuid",
        "propertyId": "uuid",
        "createdById": "uuid",
        "createdByName": "Mamadou Diallo",
        "category": "MAINTENANCE",
        "costCenter": "PROPERTY_SPECIFIC",
        "label": "Réparation toiture villa Cocody",
        "amount": 150000,
        "paymentMethod": "BANK_TRANSFER",
        "expenseDate": "2026-05-10",
        "provider": "SODECI",
        "invoiceReference": "FAC-2026-1042",
        "justificationUrl": null,
        "note": null,
        "createdAt": "2026-05-16T09:00:00",
        "updatedAt": "2026-05-16T09:00:00"
      }
    ],
    "totalElements": 12,
    "totalPages": 1,
    "size": 20,
    "number": 0
  }
}
```

**Request body création (`POST /v1/expenses`) :**
```json
{
  "propertyId": "394e1b94-6d87-41b2-8b31-031a9f45944d",
  "category": "MAINTENANCE",
  "costCenter": "PROPERTY_SPECIFIC",
  "label": "Réparation toiture villa Cocody",
  "amount": 150000,
  "paymentMethod": "BANK_TRANSFER",
  "expenseDate": "2026-05-10",
  "provider": "Entreprise Kouassi BTP",
  "invoiceReference": "FAC-2026-1042",
  "justificationUrl": null,
  "note": "Devis approuvé par le propriétaire"
}
```

> **Champs obligatoires :** `category`, `costCenter`, `label`, `amount`, `expenseDate`  
> **`propertyId` :** obligatoire si `costCenter = PROPERTY_SPECIFIC` — le backend lève une `400` si absent.  
> **Joindre une facture :** uploader d'abord via `GET /v1/storage/presign?bucket=partner-documents`, puis renseigner `justificationUrl`.

**`category` disponibles :**

| Valeur | Libellé UI | Usage typique |
|--------|------------|---------------|
| `MAINTENANCE` | Entretien / Travaux | Réparations, plomberie, électricité, nettoyage |
| `MARKETING` | Marketing | Pub, photo, boost annonces |
| `SALARY` | Salaires / Honoraires | Employés, freelances, commissions commerciaux |
| `UTILITIES` | Charges exploitation | Eau, électricité, loyer bureaux, assurances, SaaS |
| `TAX` | Taxes / Impôts | Patente, TVA, droits d'enregistrement |
| `OTHER` | Autre | Dépenses atypiques (préciser dans `label`) |

**`costCenter` disponibles :**

| Valeur | Libellé UI | Règle |
|--------|------------|-------|
| `AGENCY_GENERAL` | Charges agence | `propertyId` ignoré |
| `PROPERTY_SPECIFIC` | Bien spécifique | `propertyId` **obligatoire** |

**Response création `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "PAYMENT_CREATE_SUCCESS",
  "data": {
    "id": "uuid",
    "agencyId": "uuid",
    "propertyId": "394e1b94-6d87-41b2-8b31-031a9f45944d",
    "category": "MAINTENANCE",
    "costCenter": "PROPERTY_SPECIFIC",
    "label": "Réparation toiture villa Cocody",
    "amount": 150000,
    "paymentMethod": "BANK_TRANSFER",
    "expenseDate": "2026-05-10",
    "provider": "Entreprise Kouassi BTP",
    "invoiceReference": "FAC-2026-1042",
    "createdAt": "2026-05-16T09:00:00"
  }
}
```

**Erreurs possibles :**
- `400 Bad Request` — champ obligatoire manquant ou `costCenter = PROPERTY_SPECIFIC` sans `propertyId`
- `404 Not Found` — bien introuvable

**Critères d'acceptation :**
- [ ] Tableau paginé : libellé, catégorie (badge), centre de coût, montant, date, fournisseur, référence facture
- [ ] Filtres : catégorie · bien · plage de dates
- [ ] Formulaire d'ajout : catégorie · centre de coût · libellé · montant · mode règlement · date · fournisseur · référence · justification (upload optionnel) · note
- [ ] Champ `propertyId` (sélecteur de bien) s'affiche et devient **obligatoire** uniquement si `costCenter = PROPERTY_SPECIFIC`
- [ ] Upload facture via URL présignée `GET /v1/storage/presign?bucket=partner-documents` → renseigner `justificationUrl`
- [ ] Bouton « Supprimer » avec confirmation modale (suppression permanente)
- [ ] Toast de succès après ajout / suppression
- [ ] Loader squelette sur la liste

---

### Statuts des paiements (`PaymentStatus`)

| Valeur | Libellé UI | Couleur badge |
|--------|------------|---------------|
| `PENDING` | En attente | Orange `#F97316` |
| `PAID` | Payé | Vert `#16A34A` |
| `PARTIAL` | Partiel | Bleu `#3B82F6` |
| `LATE` | En retard | Rouge `#DC2626` |
| `CANCELLED` | Annulé | Gris `#6B7280` |

---

## RÉFÉRENCES TRANSVERSES

### Statuts des biens (`PropertyStatus`)

| Valeur | Libellé UI | Couleur badge |
|--------|------------|---------------|
| `DRAFT` | Brouillon | Gris `#6B7280` |
| `PENDING` | En modération | Orange `#F97316` |
| `PUBLISHED` | Publié | Vert `#16A34A` |
| `REJECTED` | Rejeté | Rouge `#DC2626` |
| `ARCHIVED` | Archivé | Noir `#111827` |

### Types de biens (`propertyType`)

| Valeur | Libellé UI | Contexte |
|--------|------------|----------|
| `APARTMENT` | Appartement | Agence / Hôtel |
| `VILLA` | Villa | Agence |
| `HOUSE` | Maison | Agence |
| `LAND` | Terrain | Agence |
| `OFFICE` | Bureau | Agence |
| `WAREHOUSE` | Entrepôt | Agence |
| `STORE` | Boutique | Agence |
| `STUDIO` | Studio | Agence |
| `ROOM` | Chambre | Hôtel |
| `SUITE` | Suite | Hôtel |
| `CONFERENCE_ROOM` | Salle de conférence | Hôtel |

### Types de transaction (`transactionType`)

| Valeur | Libellé UI |
|--------|------------|
| `SALE` | Vente |
| `RENT` | Location (vide) |
| `RENT_FURNISHED` | Location (meublée / nuit) |

### Types de médias (`mediaType`)

| Valeur | Libellé UI |
|--------|------------|
| `PHOTO` | Photo |
| `VIDEO` | Vidéo |
| `PLAN` | Plan |

### Gestion des erreurs HTTP (standard)

| Code | Signification | Comportement UI |
|------|---------------|----------------|
| `400` | Données invalides | Afficher les erreurs de champ en rouge |
| `401` | Non authentifié | Rediriger vers la page de login |
| `403` | Accès refusé | Toast « Accès non autorisé » |
| `404` | Ressource introuvable | Page vide avec message « Introuvable » |
| `409` | Conflit | Toast avec le message retourné par le backend |
| `500` | Erreur serveur | Toast « Une erreur est survenue, réessayez » |

---

## MODULE 12 — TICKETING SAV · PORTAIL AGENCE / CLIENT

**Epic :** `UBAX-FE-TICKETING`  
**Rôle requis (JWT) :** `UBAX_PARTNER` · `UBAX_CLIENT` · `UBAX_OWNER` · `UBAX_ADMIN`

### Objectif

Le module de ticketing SAV permet aux clients et propriétaires de déclarer des incidents (pannes, fuites, problèmes structurels…) directement depuis l'application mobile. L'agence réceptionne les tickets, les assigne à un agent et planifie une intervention avec un technicien.

**Flux principal :**
1. `CLIENT` ou `OWNER` crée un ticket avec photos (`POST /v1/tickets`) — pièces jointes pré-uploadées via URL présignée.
2. Le `PARTNER` (agent SAV) consulte la liste des tickets de l'agence et assigne un agent (`PATCH …/assign`).
3. L'agent planifie une intervention (`PATCH …/schedule`) avec technicien (interne ou libre).
4. Une fois le problème résolu, l'agent clôture (`PATCH …/status` → `CLOSED`).
5. Le client peut consulter l'état de son ticket et échangez via messages (`POST …/messages`).

| Tâche | Action | Acteur |
|-------|--------|--------|
| UBAX-FE-1201 — Créer un ticket | Écriture | `CLIENT` · `OWNER` · `PARTNER` |
| UBAX-FE-1202 — Mes tickets | Lecture | `CLIENT` · `OWNER` · `PARTNER` |
| UBAX-FE-1203 — Détail d'un ticket | Lecture | `CLIENT/OWNER` (reporter) · `PARTNER/ADMIN` |
| UBAX-FE-1204 — Liste tickets agence | Lecture | `PARTNER` · `ADMIN` |
| UBAX-FE-1205 — Assigner un ticket | Écriture | `PARTNER` · `ADMIN` |
| UBAX-FE-1206 — Planifier une intervention | Écriture | `PARTNER` · `ADMIN` |
| UBAX-FE-1207 — Mettre à jour le statut | Écriture | `PARTNER` · `ADMIN` |
| UBAX-FE-1208 — Messages du ticket | Lecture + Écriture | Authentifié |

---

### UBAX-FE-1201 · Créer un ticket

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/tickets` |
| **Auth** | Bearer token · `UBAX_PARTNER`, `UBAX_CLIENT`, `UBAX_OWNER` |
| **Content-Type** | `application/json` |

**Request body :**
```json
{
  "contractId": "uuid-du-contrat",
  "category": "LEAK",
  "priority": "HIGH",
  "title": "Fuite d'eau dans la salle de bain",
  "description": "Une fuite importante sous le lavabo depuis ce matin.",
  "attachments": [
    {
      "fileUrl": "https://minio.ubax.africa/ticket-attachments/agency-slug/fuite1.jpg",
      "fileName": "fuite1.jpg",
      "fileSize": 204800,
      "mimeType": "image/jpeg",
      "attachmentType": "INCIDENT_PHOTO",
      "caption": "Vue d'ensemble"
    }
  ]
}
```

> **Upload préalable :** obtenir une URL présignée via `GET /v1/storage/presign/ticket-attachment`, uploader le fichier, puis inclure l'URL retournée dans `attachments[].fileUrl`.  
> **`attachments` :** tableau optionnel — omis si pas de photo.

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "TICKET_CREATE_SUCCESS",
  "data": {
    "id": "uuid-ticket",
    "contractId": "uuid-du-contrat",
    "reporterId": "uuid-reporter",
    "reporterName": "Salle Diop",
    "category": "LEAK",
    "title": "Fuite d'eau dans la salle de bain",
    "priority": "HIGH",
    "status": "OPEN",
    "createdAt": "2026-05-17T09:00:00",
    "updatedAt": "2026-05-17T09:00:00",
    "attachmentUrls": null
  }
}
```

**Critères d'acceptation :**
- [ ] Formulaire : sélecteur de catégorie (LEAK, ELECTRICAL, LOCK, PLUMBING, APPLIANCE, STRUCTURE, PEST, COMMON_AREA, OTHER), priorité (LOW, NORMAL, HIGH, URGENT), titre, description
- [ ] Upload photos : `GET /v1/storage/presign/ticket-attachment` → upload → ajouter `fileUrl` dans le tableau `attachments`
- [ ] Jusqu'à 5 photos autorisées
- [ ] Toast de succès après création, redirection vers détail du ticket

---

### UBAX-FE-1202 · Mes tickets

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/tickets/mine` |
| **Auth** | Bearer token · `UBAX_CLIENT`, `UBAX_OWNER`, `UBAX_PARTNER` |

**Response `200` :**
```json
{
  "data": {
    "content": [
      {
        "id": "uuid-ticket",
        "title": "Fuite d'eau dans la salle de bain",
        "category": "LEAK",
        "priority": "HIGH",
        "status": "IN_ANALYSIS",
        "createdAt": "2026-05-17T09:00:00",
        "attachmentUrls": null
      }
    ],
    "totalElements": 3
  }
}
```

> **`attachmentUrls`** est `null` sur la liste — les URLs sont disponibles uniquement via le détail (`GET /v1/tickets/{id}`).

**Critères d'acceptation :**
- [ ] Liste paginée triée par `createdAt DESC`
- [ ] Badge coloré par statut (OPEN = gris, IN_ANALYSIS = bleu, IN_PROGRESS = orange, RESOLVED = vert, CLOSED = noir)
- [ ] Tap sur une ligne → détail du ticket

---

### UBAX-FE-1203 · Détail d'un ticket

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/tickets/{id}` |
| **Auth** | Bearer token · `CLIENT/OWNER` (reporter uniquement) · `PARTNER/ADMIN` |

> **Restriction CLIENT/OWNER :** le backend retourne `403` si le caller n'est pas le reporter du ticket.

**Response `200` :**
```json
{
  "data": {
    "id": "uuid-ticket",
    "contractId": "uuid-contrat",
    "reporterId": "uuid-reporter",
    "reporterName": "Salle Diop",
    "assignedToId": "uuid-agent",
    "assignedToName": "Fatou Ndiaye",
    "category": "LEAK",
    "title": "Fuite d'eau dans la salle de bain",
    "description": "Une fuite importante sous le lavabo depuis ce matin.",
    "priority": "HIGH",
    "status": "IN_ANALYSIS",
    "technicienId": "uuid-technicien",
    "technicienProfession": "PLOMBIER",
    "technicianName": "Ibrahima Sow",
    "technicianPhone": "+221771234567",
    "interventionPrice": 35000,
    "interventionScheduledAt": "2026-05-20T10:00:00",
    "resolvedAt": null,
    "closedAt": null,
    "repairCost": null,
    "costImputedTo": null,
    "resolutionNote": null,
    "rating": null,
    "ratingComment": null,
    "createdAt": "2026-05-17T09:00:00",
    "updatedAt": "2026-05-17T09:30:00",
    "attachmentUrls": [
      "https://minio.ubax.africa/ticket-attachments/agency-slug/fuite1.jpg",
      "https://minio.ubax.africa/ticket-attachments/agency-slug/fuite2.jpg"
    ]
  }
}
```

> **`attachmentUrls`** : liste des URLs des photos attachées au ticket — présente uniquement sur cet endpoint (liste retourne `null`).  
> Utiliser directement dans `<img src>` — bucket `ticket-attachments` est privé, URLs directement accessibles via MinIO (pas de présignature nécessaire à l'affichage si le bucket est public, sinon utiliser la présignature `GET /v1/storage/presign`).

**Critères d'acceptation :**
- [ ] Afficher toutes les informations du ticket (statut, catégorie, reporter, agent assigné, technicien)
- [ ] Galerie photo à partir de `attachmentUrls` (swipeable si plusieurs photos)
- [ ] Afficher les dates d'intervention planifiée, de résolution, de clôture si renseignées
- [ ] Afficher `repairCost` et `costImputedTo` si définis
- [ ] Pour `CLIENT/OWNER` : masquer les champs internes (assignedTo, interventionPrice, repairCost)

---

### UBAX-FE-1204 · Liste des tickets agence

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/tickets` |
| **Auth** | Bearer token · `UBAX_PARTNER`, `UBAX_ADMIN` |
| **Paramètres** | `?status=OPEN&assignedToId=uuid&page=0&size=20` |

**Critères d'acceptation :**
- [ ] Tableau paginé filtrable par statut et par agent assigné
- [ ] Indicateur visuel de priorité (couleur ou icône)
- [ ] Colonne « Assigné à » avec avatar ou initiales

---

### UBAX-FE-1205 · Assigner un ticket

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/tickets/{id}/assign` |
| **Auth** | Bearer token · `UBAX_PARTNER`, `UBAX_ADMIN` |

**Request body :**
```json
{ "assignedToId": "uuid-db-agent-sav" }
```

> **`assignedToId`** : UUID **base de données** de l'agent (`user.id`), **pas** le Keycloak ID — utiliser le champ `id` retourné par `GET /v1/agency/team`.  
> **Effet sur le statut** : l'assignation ne change **pas** le statut du ticket — c'est `PATCH /status` → `IN_ANALYSIS` qui formalise la prise en charge.

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "TICKET_UPDATE_SUCCESS",
  "data": {
    "id": "uuid-ticket",
    "contractId": "uuid-contrat",
    "reporterId": "uuid-reporter",
    "reporterName": "Salle Diop",
    "assignedToId": "uuid-db-agent-sav",
    "assignedToName": "Fatou Ndiaye",
    "category": "LEAK",
    "title": "Fuite d'eau dans la salle de bain",
    "priority": "HIGH",
    "status": "OPEN",
    "technicienId": null,
    "technicianName": null,
    "technicianPhone": null,
    "interventionPrice": null,
    "interventionScheduledAt": null,
    "createdAt": "2026-05-17T09:00:00",
    "updatedAt": "2026-05-17T09:45:00",
    "attachmentUrls": null
  }
}
```

> **`status` reste `OPEN`** après assignation — c'est `assignedToId` / `assignedToName` qui reflètent le changement dans la réponse.

**Erreurs possibles :**
- `404 Not Found` — ticket introuvable
- `404 Not Found` — agent introuvable (`assignedToId` ne correspond à aucun utilisateur en base)

**Critères d'acceptation :**
- [ ] Dropdown des membres de l'agence ayant le sous-rôle `AGENT_SAV` — source : `GET /v1/agency/team`, filtrer sur `subRole = AGENT_SAV`
- [ ] Envoyer `user.id` (UUID DB) dans `assignedToId`, pas `user.keycloakId`
- [ ] Après assignation : mettre à jour `assignedToName` dans l'UI depuis la réponse sans rechargement complet
- [ ] Toast de confirmation après assignation

---

### UBAX-FE-1206 · Planifier une intervention

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/tickets/{id}/schedule` |
| **Auth** | Bearer token · `UBAX_PARTNER`, `UBAX_ADMIN` |

**Champs obligatoires par mode :**

| Champ | Mode plateforme | Mode libre | Notes |
|-------|:--------------:|:----------:|-------|
| `technicienId` | ✅ obligatoire | — | UUID du technicien enregistré |
| `technicianName` | — | ✅ obligatoire | Ignoré si `technicienId` présent |
| `technicianPhone` | — | ✅ obligatoire | Format `+XXX…` · Ignoré si `technicienId` présent |
| `interventionScheduledAt` | ✅ obligatoire | ✅ obligatoire | Doit être dans le futur (`@Future`) |
| `interventionPrice` | optionnel | optionnel | ≥ 0 XOF |

**Request body minimal — mode plateforme :**
```json
{
  "technicienId": "uuid-technicien",
  "interventionScheduledAt": "2026-05-20T10:00:00"
}
```

**Request body minimal — mode libre :**
```json
{
  "technicianName": "Ibrahima Sow",
  "technicianPhone": "+221771234567",
  "interventionScheduledAt": "2026-05-20T10:00:00"
}
```

> **Mode plateforme** : nom et téléphone sont récupérés automatiquement depuis le profil du technicien — ne pas les renvoyer.  
> **Mode libre** : `technicianName` **ET** `technicianPhone` sont tous les deux obligatoires — `400` si l'un manque.  
> **Effet automatique sur le statut** : `PATCH /schedule` force le ticket vers `TECHNICIAN_SENT` — **ne pas appeler** `PATCH /status` séparément après planification.  
> **`interventionScheduledAt`** doit être dans le futur — le backend retourne `400` sinon.

**Critères d'acceptation :**
- [ ] Switch UI « Technicien plateforme / Technicien libre »
- [ ] Mode plateforme : liste déroulante `GET /v1/technicians?available=true` — afficher nom + profession
- [ ] Mode libre : deux champs texte `Nom complet` + `Téléphone` (format international `+XXX…`) — les deux obligatoires
- [ ] Date-picker + heure pour `interventionScheduledAt` — bloquer les dates passées (validation `@Future` backend)
- [ ] Champ montant pour `interventionPrice` (optionnel, ≥ 0)
- [ ] Après succès : le ticket passe automatiquement en `TECHNICIAN_SENT` — mettre à jour le badge statut dans l'UI sans appel supplémentaire

---

### UBAX-FE-1207 · Mettre à jour le statut

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/tickets/{id}/status` |
| **Auth** | Bearer token · `UBAX_PARTNER`, `UBAX_ADMIN` |

**Request body :**
```json
{
  "status": "IN_ANALYSIS",
  "resolutionNote": "Note obligatoire pour RESOLVED et CLOSED"
}
```

**Transitions autorisées (backend) :**

| Depuis | Vers (valeurs autorisées) |
|--------|--------------------------|
| `OPEN` | `IN_ANALYSIS` · `CANCELLED` |
| `IN_ANALYSIS` | `TECHNICIAN_SENT`* · `RESOLVED` · `CANCELLED` |
| `TECHNICIAN_SENT` | `RESOLVED` · `CANCELLED` |
| `RESOLVED` | `CLOSED` |
| `CLOSED` · `CANCELLED` | aucune transition possible |

> *`TECHNICIAN_SENT` est positionné **automatiquement** par `PATCH /schedule` — ne pas l'envoyer manuellement via `/status`.  
> **`resolutionNote`** est **obligatoire** (non vide) pour les transitions vers `RESOLVED` et `CLOSED` — le backend retourne `400` sinon.

**Critères d'acceptation :**
- [ ] Boutons d'action contextuels selon le statut courant — afficher uniquement les transitions autorisées (tableau ci-dessus)
- [ ] Champ `resolutionNote` (textarea) affiché et **obligatoire** lors du passage à `RESOLVED` ou `CLOSED`
- [ ] Bouton « Annuler le ticket » (`CANCELLED`) visible depuis `OPEN`, `IN_ANALYSIS` et `TECHNICIAN_SENT` — masqué depuis `RESOLVED` et `CLOSED`
- [ ] Modal de confirmation avant passage à `CLOSED` ou `CANCELLED`
- [ ] Toast de succès après mise à jour

---

### UBAX-FE-1208 · Messages du ticket

| Champ | Valeur |
|-------|--------|
| **Endpoint lecture** | `GET /v1/tickets/{id}/messages` |
| **Endpoint écriture** | `POST /v1/tickets/{id}/messages` |
| **Auth** | Bearer token · Authentifié |

**Request body (nouveau message) :**
```json
{ "message": "L'intervention est planifiée pour demain matin." }
```

**Critères d'acceptation :**
- [ ] Fil de discussion style chat (messages triés par `createdAt ASC`)
- [ ] Distinguer les messages AGENT vs CLIENT via `messageType`
- [ ] Champ de saisie en bas avec bouton Envoyer
- [ ] Rafraîchissement automatique ou pull-to-refresh

---

---

## MODULE 13 — RÉSERVATION DE VISITES · PORTAIL AGENCE / CLIENT MOBILE

**Epic :** `UBAX-FE-VISITS`  
**Rôle requis (JWT) :** `UBAX_PARTNER` (agence) · `UBAX_CLIENT` · `UBAX_OWNER` · Public (consultation créneaux)

### Objectif

Le module de réservation de visites permet aux clients mobiles de consulter les créneaux disponibles pour un bien immobilier et de soumettre une demande de visite. L'agence configure ses disponibilités par bien (jours ouvrables, créneaux horaires, capacité max), gère les dates fermées et traite les demandes (confirmation, rejet, assignation d'agent).

**Flux principal :**
1. L'agence configure les créneaux de visite pour chacun de ses biens (`POST /v1/agency/property-visits/config`).
2. Le client consulte les créneaux disponibles (`GET /v1/property-visits/available-slots/{propertyId}`) — public, sans JWT.
3. Le client soumet une demande pour une date et un créneau (`POST /v1/property-visits`) → statut `PENDING`.
4. L'agence reçoit la demande et la confirme (`PATCH …/confirm`) ou la rejette (`PATCH …/reject`) → `CONFIRMED` / `REJECTED`.
5. L'agence assigne un agent immobilier à la visite (`PATCH …/assign-agent/{agentId}`).
6. Après la visite, le statut est mis à `COMPLETED` (archive).

**Périmètre frontend de ce module :**

| Tâche | Action | Acteur |
|-------|--------|--------|
| UBAX-FE-1301 — Consulter les créneaux disponibles | Lecture | Grand public (sans compte) |
| UBAX-FE-1302 — Demander une visite | Écriture | `CLIENT` · `OWNER` |
| UBAX-FE-1303 — Mes demandes de visite | Lecture | `CLIENT` · `OWNER` |
| UBAX-FE-1304 — Détail d'une demande de visite | Lecture | `CLIENT` · `OWNER` |
| UBAX-FE-1305 — Annuler une demande de visite | Écriture | `CLIENT` · `OWNER` |
| UBAX-FE-1306 — Configurer les créneaux d'un bien | Écriture | `PARTNER` |
| UBAX-FE-1307 — Gérer les dates d'indisponibilité | Écriture | `PARTNER` |
| UBAX-FE-1308 — Liste des demandes reçues (agence) | Lecture | `PARTNER` |
| UBAX-FE-1309 — Confirmer une demande de visite | Écriture | `PARTNER` |
| UBAX-FE-1310 — Rejeter une demande de visite | Écriture | `PARTNER` |
| UBAX-FE-1311 — Assigner un agent à une visite | Écriture | `PARTNER` |

**Points d'attention :**
- `GET /v1/property-visits/available-slots/{propertyId}` est **public** — aucun JWT requis. Intégrer directement dans la fiche bien publique.
- `DELETE /v1/property-visits/{visitRequestId}` retourne **204 No Content** (pas de body) — cas unique dans l'API, adapter le handler côté frontend.
- `confirmedDate` / `confirmedTimeSlot` dans `PATCH …/confirm` peuvent être différents de ceux demandés (proposition alternative de l'agence).
- `agentId` dans `PATCH …/assign-agent/{agentId}` est l'UUID **base de données** (`user.id`), **pas** le Keycloak ID — source : `GET /v1/agency/team`.
- La configuration des créneaux (`timeSlots`) utilise les jours de semaine ISO (0=Dimanche, 1=Lundi … 6=Samedi).

---

### UBAX-FE-1301 · Consulter les créneaux disponibles pour un bien

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/property-visits/available-slots/{propertyId}` |
| **Auth** | **Aucune** (public) |
| **Path params** | `propertyId` : UUID du bien |
| **Query params** | `daysAhead` : nombre de jours à scanner (défaut `30`) |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "VISIT_AVAILABILITY_GET_SUCCESS",
  "data": {
    "propertyId": "550e8400-e29b-41d4-a716-446655440000",
    "availableDates": ["2026-06-10", "2026-06-11", "2026-06-12"],
    "slotsByDate": {
      "2026-06-10": [
        { "slot": "10:00-14:00", "available": 2, "almostFull": false },
        { "slot": "14:00-18:00", "available": 1, "almostFull": true }
      ],
      "2026-06-11": [
        { "slot": "10:00-14:00", "available": 3, "almostFull": false }
      ]
    }
  }
}
```

> **`almostFull = true`** quand il ne reste qu'une seule place — afficher un badge « Dernière place » pour créer de l'urgence.  
> Les dates sans créneaux disponibles sont absentes de `slotsByDate` (blackout ou complets).

**Erreurs possibles :**
- `404 Not Found` — bien introuvable

**Critères d'acceptation :**
- [ ] Calendrier ou liste de dates avec les créneaux disponibles intégrée dans la fiche bien publique
- [ ] Créneaux désactivés ou masqués si `available = 0`
- [ ] Badge « Dernière place » (ou orange) si `almostFull = true`
- [ ] Message « Aucun créneau disponible » si `availableDates` est vide
- [ ] Appel automatique au chargement de la fiche bien (pas d'action utilisateur requise)
- [ ] Paramètre `daysAhead` configurable si le besoin d'étendre la fenêtre de réservation est exprimé

---

### UBAX-FE-1302 · Demander une visite

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/property-visits` |
| **Auth** | Bearer token · `UBAX_CLIENT`, `UBAX_OWNER` |
| **Content-Type** | `application/json` |

**Request body :**
```json
{
  "propertyId": "550e8400-e29b-41d4-a716-446655440000",
  "requestedDate": "2026-06-10",
  "requestedTimeSlot": "10:00-14:00",
  "clientNotes": "Très intéressé par la terrasse, visite rapide SVP"
}
```

> **`clientNotes`** est optionnel.  
> **`requestedTimeSlot`** doit être une valeur parmi les créneaux retournés par `GET /available-slots` — `400` si indisponible.

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "VISIT_REQUEST_CREATE_SUCCESS",
  "data": {
    "id": "uuid-demande",
    "propertyId": "uuid",
    "propertyTitle": "Villa F5 Almadies",
    "clientId": "uuid",
    "clientName": "Jean Kouassi",
    "agentId": null,
    "agentName": null,
    "requestedDate": "2026-06-10",
    "requestedTimeSlot": "10:00-14:00",
    "status": "PENDING",
    "confirmedDate": null,
    "confirmedTimeSlot": null,
    "rejectionReason": null,
    "clientNotes": "Très intéressé par la terrasse, visite rapide SVP",
    "createdAt": "2026-06-01T10:00:00",
    "updatedAt": "2026-06-01T10:00:00"
  }
}
```

**Erreurs possibles :**
- `400 Bad Request` — créneau indisponible ou complet, bien non publié, date passée
- `401 Unauthorized` — token absent
- `403 Forbidden` — rôle insuffisant

**Critères d'acceptation :**
- [ ] Bouton « Demander une visite » visible sur la fiche bien publique pour les utilisateurs connectés (CLIENT/OWNER)
- [ ] Sélecteur de date parmi `availableDates` (pré-rempli depuis UBAX-FE-1301)
- [ ] Sélecteur de créneau parmi les `slotsByDate[date]` disponibles — griser les créneaux `available = 0`
- [ ] Champ texte optionnel `clientNotes` (ex. « Précisez votre demande »)
- [ ] Validation frontend : `requestedDate` et `requestedTimeSlot` requis
- [ ] Après succès : message de confirmation « Votre demande de visite a été envoyée. L'agence vous contactera sous peu. » + redirection vers UBAX-FE-1303
- [ ] Badge statut `PENDING` affiché immédiatement dans la liste

---

### UBAX-FE-1303 · Mes demandes de visite (client)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/property-visits/mine` |
| **Auth** | Bearer token · `UBAX_CLIENT`, `UBAX_OWNER` |
| **Query params** | `page` (défaut `0`) · `size` (défaut `20`) · `sort=createdAt,desc` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "VISIT_REQUEST_GET_LIST_SUCCESS",
  "data": {
    "content": [
      {
        "id": "uuid",
        "propertyTitle": "Villa F5 Almadies",
        "requestedDate": "2026-06-10",
        "requestedTimeSlot": "10:00-14:00",
        "status": "CONFIRMED",
        "confirmedDate": "2026-06-10",
        "confirmedTimeSlot": "10:00-14:00",
        "agentName": "Marie Amani",
        "createdAt": "2026-06-01T10:00:00"
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
- [ ] Liste paginée avec : titre du bien, date demandée, créneau, statut (badge coloré), nom de l'agent si assigné
- [ ] Badge statut : `PENDING` orange · `CONFIRMED` vert · `REJECTED` rouge · `CANCELLED` gris · `COMPLETED` bleu
- [ ] Bouton « Annuler » visible uniquement si `status = PENDING` → déclenche UBAX-FE-1305
- [ ] Clic sur une ligne → redirige vers UBAX-FE-1304
- [ ] Message si la liste est vide : « Vous n'avez aucune demande de visite. »

---

### UBAX-FE-1304 · Détail d'une demande de visite (client)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/property-visits/{visitRequestId}` |
| **Auth** | Bearer token · `UBAX_CLIENT`, `UBAX_OWNER` |
| **Path params** | `visitRequestId` : UUID de la demande |

**Response `200` :** _(objet `VisitRequestResponse` complet — voir UBAX-FE-1302)_

**Erreurs possibles :**
- `403 Forbidden` — demande appartenant à un autre utilisateur
- `404 Not Found` — demande introuvable

**Critères d'acceptation :**
- [ ] Fiche complète : titre du bien, date demandée, créneau demandé, statut, date/créneau confirmés (si `CONFIRMED`), motif de rejet (si `REJECTED`), notes client, agent assigné
- [ ] Bandeau vert si `status = CONFIRMED` avec la date et le créneau confirmés mis en évidence
- [ ] Bandeau rouge si `status = REJECTED` avec le motif visible
- [ ] Bouton « Annuler la demande » visible uniquement si `status = PENDING` → déclenche UBAX-FE-1305
- [ ] Lien vers la fiche du bien (`/v1/properties/{propertyId}`)

---

### UBAX-FE-1305 · Annuler une demande de visite

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `DELETE /v1/property-visits/{visitRequestId}` |
| **Auth** | Bearer token · `UBAX_CLIENT`, `UBAX_OWNER` |
| **Path params** | `visitRequestId` : UUID de la demande |
| **Request body** | _(aucun)_ |

> ⚠️ Retourne **204 No Content** (pas de body JSON). Adapter le handler HTTP — ne pas parser la réponse.  
> Seules les demandes `PENDING` sont annulables — 400 sinon.

**Erreurs possibles :**
- `400 Bad Request` — demande non en statut `PENDING`
- `403 Forbidden` — demande ne vous appartient pas
- `404 Not Found` — demande introuvable

**Critères d'acceptation :**
- [ ] Dialog de confirmation avant annulation (« Êtes-vous sûr de vouloir annuler cette visite ? »)
- [ ] Après succès (204) : mettre à jour le badge `status → CANCELLED` sans rechargement complet de la page
- [ ] Bouton « Annuler » masqué immédiatement après succès
- [ ] Toast de confirmation : « Votre demande de visite a été annulée. »

---

### UBAX-FE-1306 · Configurer les créneaux de visite d'un bien (agence)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `POST /v1/agency/property-visits/config` |
| **Auth** | Bearer token · `UBAX_PARTNER` |
| **Content-Type** | `application/json` |

> L'appel est **idempotent** — si une configuration existe pour ce bien, elle est intégralement remplacée.

**Request body :**
```json
{
  "propertyId": "550e8400-e29b-41d4-a716-446655440000",
  "timeSlots": {
    "1": ["10:00-14:00", "14:00-18:00"],
    "2": ["10:00-14:00"],
    "3": ["10:00-14:00"],
    "4": ["10:00-14:00"],
    "5": ["10:00-18:00"],
    "6": ["10:00-14:00"]
  },
  "blackoutDates": ["2026-07-14", "2026-08-15"],
  "maxVisitsPerSlot": 3
}
```

> **`timeSlots`** : map des jours de semaine (0=Dimanche, 1=Lundi … 6=Samedi) → liste de créneaux au format `HH:mm-HH:mm`. Un jour absent = jour fermé.  
> **`maxVisitsPerSlot`** (défaut 3) : nombre max de demandes simultanées par créneau.  
> **`blackoutDates`** : liste de dates YYYY-MM-DD (peut être vide).

**Response `201` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "VISIT_AVAILABILITY_CONFIGURE_SUCCESS",
  "data": {
    "propertyId": "uuid",
    "availableDates": ["2026-06-10", "2026-06-11"],
    "slotsByDate": { ... }
  }
}
```

**Erreurs possibles :**
- `400 Bad Request` — format de créneau invalide ou `maxVisitsPerSlot < 1`
- `403 Forbidden` — bien n'appartient pas à votre agence
- `404 Not Found` — bien introuvable

**Critères d'acceptation :**
- [ ] Formulaire de configuration accessible depuis la fiche bien (espace partenaire) — bouton « Configurer les visites »
- [ ] Grille hebdomadaire : pour chaque jour (Lun à Sam), bascule ON/OFF + saisie des créneaux
- [ ] Ajout/suppression dynamique de créneaux par jour (bouton « + Ajouter un créneau »)
- [ ] Format créneau validé en frontend : `HH:mm-HH:mm` (ex. `10:00-14:00`)
- [ ] Champ `maxVisitsPerSlot` avec valeur par défaut 3 et validation `>= 1`
- [ ] Section dates fermées : sélecteur multi-dates (calendrier) pour `blackoutDates`
- [ ] Toast de succès après sauvegarde
- [ ] Pré-remplissage si une configuration existe déjà (GET /config/{propertyId} → UBAX-FE-1307)

---

### UBAX-FE-1307 · Gérer les dates d'indisponibilité (blackout)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PUT /v1/agency/property-visits/config/{propertyId}/blackout-dates` |
| **Auth** | Bearer token · `UBAX_PARTNER` |
| **Path params** | `propertyId` : UUID du bien |
| **Content-Type** | `application/json` |

> Remplace **intégralement** les dates fermées existantes.  
> Envoyer `{ "blackoutDates": [] }` pour tout supprimer.

**Request body :**
```json
{
  "blackoutDates": ["2026-07-14", "2026-08-15", "2026-12-25"]
}
```

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "VISIT_AVAILABILITY_UPDATE_SUCCESS",
  "data": null
}
```

**Erreurs possibles :**
- `404 Not Found` — bien ou configuration introuvable

**Critères d'acceptation :**
- [ ] Calendrier multi-sélection pour choisir les dates fermées (ex. librairie date-picker avancé)
- [ ] Les dates déjà sélectionnées sont pré-chargées depuis `GET /config/{propertyId}`
- [ ] Bouton « Enregistrer les dates fermées » déclenche le PUT
- [ ] Toast de succès après mise à jour
- [ ] Bouton « Tout effacer » pour réinitialiser (envoie `blackoutDates: []`)

---

### UBAX-FE-1308 · Liste des demandes de visite (agence)

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `GET /v1/agency/property-visits` |
| **Auth** | Bearer token · `UBAX_PARTNER` |
| **Query params** | `page` (défaut `0`) · `size` (défaut `20`) · `sort=createdAt,asc` |

**Response `200` :**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "VISIT_REQUEST_GET_LIST_SUCCESS",
  "data": {
    "content": [
      {
        "id": "uuid",
        "propertyId": "uuid",
        "propertyTitle": "Villa F5 Almadies",
        "clientId": "uuid",
        "clientName": "Jean Kouassi",
        "agentId": null,
        "agentName": null,
        "requestedDate": "2026-06-10",
        "requestedTimeSlot": "10:00-14:00",
        "status": "PENDING",
        "confirmedDate": null,
        "confirmedTimeSlot": null,
        "rejectionReason": null,
        "clientNotes": "Intéressé par la terrasse",
        "createdAt": "2026-06-01T10:00:00",
        "updatedAt": "2026-06-01T10:00:00"
      }
    ],
    "totalElements": 12,
    "totalPages": 1,
    "size": 20,
    "number": 0
  }
}
```

**Critères d'acceptation :**
- [ ] Tableau paginé avec : titre du bien, nom du client, date demandée, créneau, statut, agent assigné, date de soumission
- [ ] Filtre par statut (`PENDING` / `CONFIRMED` / `REJECTED` / `CANCELLED` / `COMPLETED`)
- [ ] Tri par `createdAt ASC` par défaut (les plus anciennes en priorité de traitement)
- [ ] Badge statut coloré avec indicateur visuel de priorité (`PENDING` en orange — action requise)
- [ ] Clic sur une ligne → ouvre le détail avec les boutons d'action (UBAX-FE-1309 / 1310 / 1311)
- [ ] Compteur « N demandes en attente » visible en haut de la liste

---

### UBAX-FE-1309 · Confirmer une demande de visite

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/agency/property-visits/{visitRequestId}/confirm` |
| **Auth** | Bearer token · `UBAX_PARTNER` |
| **Path params** | `visitRequestId` : UUID de la demande |
| **Content-Type** | `application/json` |

**Request body :**
```json
{
  "confirmedDate": "2026-06-10",
  "confirmedTimeSlot": "10:00-14:00",
  "agencyNotes": "Veuillez arriver 5 minutes en avance. Présenter une pièce d'identité."
}
```

> `confirmedDate` et `confirmedTimeSlot` peuvent être différents de ceux demandés (proposition alternative).  
> `agencyNotes` est optionnel.

**Response `200` :** _(objet `VisitRequestResponse` avec `status: "CONFIRMED"`)_

**Erreurs possibles :**
- `400 Bad Request` — statut invalide (doit être `PENDING`) ou créneau confirmé déjà complet
- `403 Forbidden` — demande n'appartient pas à votre agence
- `404 Not Found` — demande introuvable

**Critères d'acceptation :**
- [ ] Bouton « Confirmer » visible uniquement si `status = PENDING`
- [ ] Modal de confirmation avec date/créneau pré-remplis depuis la demande (modifiables pour proposition alternative)
- [ ] Champ optionnel `agencyNotes` (ex. « Instructions pour le jour J »)
- [ ] Validation frontend : `confirmedDate` et `confirmedTimeSlot` requis
- [ ] Après succès : badge statut → `CONFIRMED` mis à jour sans rechargement
- [ ] Toast de succès : « La visite a été confirmée pour le {confirmedDate} à {confirmedTimeSlot}. »

---

### UBAX-FE-1310 · Rejeter une demande de visite

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/agency/property-visits/{visitRequestId}/reject` |
| **Auth** | Bearer token · `UBAX_PARTNER` |
| **Path params** | `visitRequestId` : UUID de la demande |
| **Content-Type** | `application/json` |

**Request body :**
```json
{
  "reason": "Bien vendu, visite impossible. Nous vous suggérons de consulter nos autres annonces."
}
```

> `reason` est **obligatoire** — 400 si absent ou vide.

**Response `200` :** _(objet `VisitRequestResponse` avec `status: "REJECTED"` et `rejectionReason` renseigné)_

**Erreurs possibles :**
- `400 Bad Request` — statut invalide (doit être `PENDING`) ou `reason` vide
- `403 Forbidden` — demande n'appartient pas à votre agence
- `404 Not Found` — demande introuvable

**Critères d'acceptation :**
- [ ] Bouton « Rejeter » visible uniquement si `status = PENDING`
- [ ] Modal de rejet avec textarea `reason` — champ obligatoire
- [ ] Placeholder : « Ex. Bien vendu, date indisponible, etc. »
- [ ] Bouton « Confirmer le rejet » désactivé si `reason` est vide
- [ ] Après succès : badge statut → `REJECTED` + affichage du motif dans le détail
- [ ] Toast : « La demande de visite a été rejetée. »

---

### UBAX-FE-1311 · Assigner un agent immobilier à une visite

| Champ | Valeur |
|-------|--------|
| **Endpoint** | `PATCH /v1/agency/property-visits/{visitRequestId}/assign-agent/{agentId}` |
| **Auth** | Bearer token · `UBAX_PARTNER` |
| **Path params** | `visitRequestId` : UUID de la demande · `agentId` : UUID DB de l'agent |
| **Request body** | _(aucun)_ |

> **`agentId`** est l'UUID **base de données** (`user.id`), **pas** le Keycloak ID.  
> Source : `GET /v1/agency/team` — utiliser le champ `id` (pas `keycloakId`).  
> L'assignation **ne change pas le statut** de la demande.

**Response `200` :** _(objet `VisitRequestResponse` avec `agentId` et `agentName` renseignés)_

**Erreurs possibles :**
- `403 Forbidden` — demande n'appartient pas à votre agence
- `404 Not Found` — demande introuvable ou agent introuvable dans l'équipe de l'agence

**Critères d'acceptation :**
- [ ] Dropdown des membres de l'agence ayant le sous-rôle `AGENT_IMMOBILIER` — source : `GET /v1/agency/team`
- [ ] Afficher nom + avatar de l'agent dans la liste déroulante
- [ ] Envoyer `user.id` (UUID DB) dans l'URL, **pas** `user.keycloakId`
- [ ] Filtrer les membres inactifs (ne pas afficher `deletedAt != null`)
- [ ] Après assignation : `agentName` mis à jour dans l'UI depuis la réponse sans rechargement complet
- [ ] Toast de confirmation : « {agentName} a été assigné à cette visite. »
- [ ] Possibilité de réassigner un agent (bouton « Changer d'agent » si déjà assigné)
