# Flux de création d'un bien hôtelier — UBAX Platform

Guide complet de publication d'une chambre / suite hôtelière, du brouillon à la publication.  
Tous les exemples sont testés et validés en local.

---

## Prérequis — Authentification

**POST** `{{base_url}}/v1/auth/login`

```json
{
  "email": "votre-email@hotel.com",
  "password": "votre-mot-de-passe"
}
```

> Le script Postman sauvegarde automatiquement `{{access_token}}` dans l'environnement.

---

## Phase 1 — Créer le brouillon (DRAFT)

**POST** `{{base_url}}/v1/properties`

Headers :
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

```json
{
  "title": "Suite Royale Vue Mer — Hôtel Le Savana Dakar",
  "description": "Suite de luxe de 55 m² avec terrasse privée et vue panoramique sur l'océan Atlantique. Lit king-size, salon séparé, salle de bain en marbre avec baignoire balnéo. Service de chambre 24h/24.",
  "propertyType": "HOTEL_ROOM",
  "transactionType": "SHORT_STAY",
  "price": 120000,
  "condition": "NEW",
  "yearBuilt": 2022,
  "surfaceTotal": 55.0,
  "surfaceLiving": 48.0,
  "rooms": 2,
  "bedrooms": 1,
  "bathrooms": 1,
  "balconies": 1,
  "floor": 5,
  "totalFloors": 8,
  "address": "Route de la Corniche Ouest, Dakar",
  "city": "Dakar",
  "district": "Corniche Ouest",
  "street": "Route de la Corniche Ouest",
  "latitude": 14.7246,
  "longitude": -17.5017,
  "amenities": [
    { "code": "AC" },
    { "code": "FURNISHED" },
    { "code": "POOL" },
    { "code": "SECURITY" },
    { "code": "PARKING" },
    { "code": "ELEVATOR" },
    { "customValue": "Room service 24h/24", "customDescription": "Service de chambre disponible toute la nuit" },
    { "customValue": "Coffre-fort", "customDescription": "Coffre-fort électronique intégré dans la chambre" },
    { "customValue": "Minibar", "customDescription": "Minibar réapprovisionné quotidiennement" }
  ],
  "bedType": "KING",
  "maxOccupancy": 2,
  "mealPlan": "BREAKFAST",
  "paymentFrequency": "NIGHTLY"
}
```

**Réponse attendue :** `201 CREATED` — statut `DRAFT`  
**Script Postman :** `{{property_id}}` sauvegardé automatiquement.

### Valeurs de référence — Champs hôtel

| Champ | Valeurs disponibles |
|-------|-------------------|
| `propertyType` | `HOTEL_ROOM` · `HOTEL_SUITE` · `HOTEL_APARTMENT` |
| `transactionType` | `SHORT_STAY` |
| `bedType` | `SINGLE` · `DOUBLE` · `TWIN` · `KING` · `QUEEN` · `BUNK` |
| `mealPlan` | `ROOM_ONLY` · `BREAKFAST` · `HALF_BOARD` · `FULL_BOARD` · `ALL_INCLUSIVE` |
| `paymentFrequency` | `NIGHTLY` · `WEEKLY` · `MONTHLY` |

### Commodités standard disponibles (`code`)

| Code | Description |
|------|-------------|
| `POOL` | Piscine |
| `GENERATOR` | Groupe électrogène |
| `WATER_TANK` | Château d'eau |
| `AC` | Climatisation |
| `SECURITY` | Gardiennage / sécurité |
| `PARKING` | Parking |
| `ELEVATOR` | Ascenseur |
| `GARDEN` | Jardin |
| `FURNISHED` | Meublé |
| `PETS` | Animaux acceptés |

### Commodité personnalisée

```json
{
  "customValue": "Nom de la commodité",
  "customDescription": "Description optionnelle"
}
```

---

## Phase 2 — Upload des photos

**POST** `{{base_url}}/v1/properties/{{property_id}}/media/upload`

Headers :
```
Authorization: Bearer {{access_token}}
```

Body : `form-data`

| # | `type` | `isCover` | Suggestion de contenu |
|---|--------|-----------|----------------------|
| 1 | `PHOTO` | `true` | Vue panoramique / terrasse avec vue mer |
| 2 | `PHOTO` | `false` | Chambre / lit king |
| 3 | `PHOTO` | `false` | Salle de bain avec baignoire balnéo |
| 4 | `PHOTO` | `false` | Vue mer depuis le balcon |

> Répéter la requête pour chaque photo. Une seule photo avec `isCover: true`.

**Réponse attendue :** `201 CREATED` — `{{media_id}}` sauvegardé automatiquement.

---

## Phase 3 — Documents légaux

### Document 1 — Licence d'exploitation hôtelière

#### 3.1 — Obtenir l'URL presignée

**GET** `{{base_url}}/v1/storage/presign/property-document`

Headers :
```
Authorization: Bearer {{access_token}}
```

Paramètres :
```
?propertyId={{property_id}}&fileName=licence-exploitation.pdf&contentType=application/pdf
```

> Le paramètre `contentType` est obligatoire. `{{upload_url}}` et `{{object_key}}` sauvegardés automatiquement.

#### 3.2 — Uploader vers MinIO

**PUT** `{{upload_url}}`

Headers :
```
Content-Type: application/pdf
```

Auth : **No Auth** — l'URL presignée contient déjà les credentials.  
Body : `Binary` → sélectionner le fichier PDF.

**Réponse attendue :** `200 OK` vide.

#### 3.3 — Enregistrer dans la base

**POST** `{{base_url}}/v1/properties/{{property_id}}/documents`

Headers :
```
Authorization: Bearer {{access_token}}
Content-Type: application/json
```

```json
{
  "docType": "AUTRE",
  "title": "Licence d'exploitation hôtelière — Hôtel Le Savana",
  "fileUrl": "{{publicUrl retourné en 3.1}}",
  "fileName": "licence-exploitation.pdf",
  "mimeType": "application/pdf",
  "visibleToPublic": false
}
```

**Réponse attendue :** `201 CREATED` — `{{document_id}}` sauvegardé automatiquement.

---

### Document 2 — Permis de construire

Répéter les étapes 3.1 → 3.3 avec les paramètres suivants.

#### 3.1 — URL presignée

```
?propertyId={{property_id}}&fileName=permis-construire.pdf&contentType=application/pdf
```

#### 3.3 — Enregistrer

```json
{
  "docType": "PERMIS_CONSTRUIRE",
  "title": "Permis de construire — Hôtel Le Savana",
  "fileUrl": "{{publicUrl retourné en 3.1}}",
  "fileName": "permis-construire.pdf",
  "mimeType": "application/pdf",
  "visibleToPublic": false
}
```

---

### Types de documents disponibles (`docType`)

| Code | Description |
|------|-------------|
| `TITRE_FONCIER` | Titre foncier officiel |
| `PERMIS_CONSTRUIRE` | Permis de construire |
| `DIAGNOSTIC` | Diagnostic immobilier |
| `CONTRAT_BAIL` | Contrat de bail existant |
| `AUTRE` | Licence d'exploitation, classification, assurance… |

---

## Phase 4 — Soumettre en modération (DRAFT → PENDING)

**PATCH** `{{base_url}}/v1/properties/{{property_id}}/submit`

Headers :
```
Authorization: Bearer {{access_token}}
```

Body : *(vide)*

**Pré-requis :** au moins une photo de couverture uploadée.  
**Réponse attendue :** `200 OK` — statut `PENDING`.

---

## Phase 5 — Modération Admin (PENDING → PUBLISHED)

**PATCH** `{{base_url}}/v1/properties/{{property_id}}/status`

Headers :
```
Authorization: Bearer {{access_token}}   ← token ADMIN
Content-Type: application/json
```

### Approuver

```json
{
  "status": "PUBLISHED"
}
```

### Rejeter

```json
{
  "status": "REJECTED",
  "rejectionReason": "Documents de licence manquants ou non conformes."
}
```

**Réponse attendue :** `200 OK` — statut `PUBLISHED` ou `REJECTED`.

---

## Résumé du flux complet

```
POST   /v1/auth/login                                    → access_token

POST   /v1/properties                                    → DRAFT  (property_id)

POST   /v1/properties/{id}/media/upload  (× 4 photos)   → médias uploadés

GET    /v1/storage/presign/property-document             → uploadUrl  (licence)
PUT    {{upload_url}}                                    → licence sur MinIO
POST   /v1/properties/{id}/documents                    → licence enregistrée

GET    /v1/storage/presign/property-document             → uploadUrl  (permis)
PUT    {{upload_url}}                                    → permis sur MinIO
POST   /v1/properties/{id}/documents                    → permis enregistré

PATCH  /v1/properties/{id}/submit                        → PENDING
PATCH  /v1/properties/{id}/status  (Admin)               → PUBLISHED
```

---

## Statuts du cycle de vie

```
DRAFT → PENDING → PUBLISHED → RESERVED → SOLD
                → REJECTED  (retour possible vers DRAFT)
                → ARCHIVED
```
