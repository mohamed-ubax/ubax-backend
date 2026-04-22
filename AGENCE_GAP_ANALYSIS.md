# Analyse des Manques – Intégration Espace Agence UBAX

> Généré le : 2026-04-20 · Mis à jour le : 2026-04-21  
> Basé sur : captures d'écran "UBAX WEB AGENCE" (7 screens) + exploration du code existant

---

## Vue d'ensemble

L'interface agence expose **4 rôles internes** avec des tableaux de bord distincts :

| Rôle UI | Description |
|---------|-------------|
| **Directeur Général (DG)** | Vue globale : biens, revenus, transactions |
| **Commercial** | Prospects, rendez-vous, pipeline ventes/locations |
| **Comptable** | Finances, dépenses, loyers impayés, commissions |
| **Agent SAV** | Tickets maintenance, planning interventions, équipe |

La navigation commune contient : **Tableau de bord · Biens · Réservations · Demandes clientèles · Finances · Archivages**

---

## Résumé de l'état actuel du backend

| Module | Entités | Controllers | Services | Status |
|--------|---------|-------------|---------|--------|
| Auth / Users | ✅ | ✅ | ✅ | Complet |
| Sous-rôles (UserSubRole) | ✅ | ✅ | ✅ | **Complet** (V016) |
| Hôtels (Hotel) | ✅ | – | – | **Entité + migration** (V017) |
| Partner (candidature) | ✅ | ✅ | ✅ | Complet |
| Tenant (KYC locataire) | ✅ | ✅ | ✅ | Complet |
| Property (biens) | ✅ | ✅ | ✅ | **Complet** (V013) |
| Payment | ✅ | ✅ | ✅ | **Complet** (V014) |
| Expense (dépenses) | ✅ | ✅ | ✅ | **Complet** (V015) |
| Dashboard / Analytics | ✅ | ✅ | ✅ | **Complet** (KPIs agence) |
| Storage (MinIO) | ✅ | ✅ | ✅ | Complet |
| Ticketing | ✅ | ❌ | ❌ | Entités + enums (V018/V019) |
| Contract (contrats) | ✅ | ❌ | ❌ | **Entités seules** |
| Document (PDF) | ✅ | ❌ | ❌ | **Entités seules** |
| Appointments / Agenda | ❌ | ❌ | ❌ | **Absent** |
| Prospects | ❌ | ❌ | ❌ | **Absent** |
| Notifications in-app | ❌ | ❌ | ❌ | **Absent** |

---

## Détail des manques par fonctionnalité

---

### 1. Sous-rôles internes de l'agence ✅ IMPLÉMENTÉ

**Ce qui a été livré (refactoring `feature/refactor`) :**
- Enums `AgenceRole` (DIRECTEUR_AGENCE, COMMERCIAL, COMPTABLE_AGENCE, AGENT_SAV), `HotelRole`, `UbaxAdminRole`, `RoleScope`
- Table `user_sub_roles` (V016) avec contrainte unique `(user_id, role, scope)` et check scope
- `UserSubRoleController` : `POST/GET/DELETE /v1/admin/users/{userId}/sub-roles`
- `UserRoleService` : `assignSubRoles`, `getSubRoles`, `revokeSubRole`, `hasSubRole`
- `RoleGuard` niveau 2 : `checkAgenceRole()`, `checkHotelRole()`, `checkAdminSubRole()`
- `AgencyTeamController` : gestion équipe agence (inviter, lister, changer rôle, retirer)

---

### 2. Module Biens (Property) ✅ IMPLÉMENTÉ

CRUD complet, médias, documents, modération, boost, expiration — voir référence rapide dans CLAUDE.md.

---

### 3. Module Paiements & Dépenses ✅ IMPLÉMENTÉ

`Payment` (V014) et `Expense` (V015) avec controllers et services complets.
KPIs financiers disponibles dans `GET /v1/dashboard/agency`.

---

### 4. Module Analytics / Dashboard ✅ IMPLÉMENTÉ

`GET /v1/dashboard/agency` retourne des KPIs adaptés au sous-rôle de l'appelant :
- **DIRECTEUR_AGENCE** : vision globale (revenus, dépenses, biens, contrats, paiements)
- **COMMERCIAL** : portfolio biens, publications
- **COMPTABLE_AGENCE** : finances détaillées (totalRevenue, totalExpenses, netRevenue, overdueAmount, recoveryRate, revenueByType, expensesByCategory)
- **AGENT_SAV** : données tickets (à enrichir quand le module ticketing aura son service)

---

### 5. Module Ticketing – Controllers & Services

**Ce qui manque (entités existent déjà) :**
- `POST /v1/tickets` – Créer un ticket
- `GET /v1/tickets` – Liste avec filtres (statut, priorité, assigné, bien)
- `GET /v1/tickets/{id}` – Détail avec messages et pièces jointes
- `PATCH /v1/tickets/{id}/assign` – Assigner à un agent SAV
- `PATCH /v1/tickets/{id}/status` – Changer le statut
- `PATCH /v1/tickets/{id}/schedule` – Planifier une intervention (technicien, date)
- `POST /v1/tickets/{id}/messages` – Ajouter un message au fil
- `POST /v1/tickets/{id}/attachments` – Uploader une pièce jointe

---

### 6. Module Contrats – Controllers & Services

**Ce qui manque (entité existe déjà) :**
- `POST /v1/contracts` – Créer un contrat (LEASE / SALE / RESERVATION / MANDATE)
- `GET /v1/contracts` – Liste paginée
- `GET /v1/contracts/{id}` – Détail
- `PATCH /v1/contracts/{id}/status` – Mettre à jour le statut
- `POST /v1/contracts/{id}/sign` – Lancer la signature numérique (DocuSeal)
- Génération PDF via le module `document` (Thymeleaf template à créer)

---

### 7. Module Prospects (Commercial)

**Tout est absent – nouvelle entité à créer :**
```
Prospect
  - agency (FK), assignedTo (FK User/Commercial)
  - firstName, lastName, phone, email
  - budget, preferredLocation, propertyType
  - source (WEBSITE | PHONE | REFERRAL | WALK_IN | SOCIAL_MEDIA)
  - status (NEW | CONTACTED | VISIT_SCHEDULED | OFFER_MADE | CONVERTED | LOST)
  - notes, lastContactAt
```

**Endpoints :**
- `POST /v1/prospects` – Créer un prospect
- `GET /v1/prospects` – Liste avec filtres (statut, commercial assigné)
- `GET /v1/prospects/{id}` – Détail
- `PUT /v1/prospects/{id}` – Mettre à jour
- `PATCH /v1/prospects/{id}/assign` – Assigner à un commercial

---

### 8. Module Rendez-vous / Agenda (Commercial)

Le dashboard commercial affiche un **planning hebdomadaire** avec créneaux horaires.

**Nouvelle entité à créer :**
```
Appointment
  - property (FK), prospect (FK), agent (FK User)
  - scheduledAt, durationMinutes
  - type (VISIT | SIGNATURE | MEETING | CALL)
  - status (SCHEDULED | CONFIRMED | DONE | CANCELLED | NO_SHOW)
  - location (address ou "à distance")
  - note
```

**Endpoints :**
- `POST /v1/appointments` – Créer un rendez-vous
- `GET /v1/appointments` – Vue liste/agenda (filtres : semaine, agent, bien)
- `GET /v1/appointments/{id}` – Détail
- `PATCH /v1/appointments/{id}/status` – Confirmer / annuler / marquer effectué
- `GET /v1/appointments/calendar?week=2026-W16` – Vue calendrier hebdomadaire

---

### 9. Module Réservations

La navigation agence contient un onglet **Réservations** distinct des contrats.

**À clarifier / créer :**
```
Reservation
  - property (FK), prospect ou customer (FK)
  - startDate, endDate (pour location courte durée)
  - amount, depositPaid, status
  - type (SHORT_TERM | LONG_TERM | SALE)
```

---

### 10. Notifications in-app

Les maquettes affichent une **cloche de notification** dans le header de tous les dashboards.

**Ce qui manque :**
```
Notification (entité)
  - recipient (FK User)
  - type (PAYMENT_DUE | TICKET_ASSIGNED | CONTRACT_SIGNED | PROSPECT_ADDED | …)
  - title, message, isRead, readAt, link (optionnel)
```

**Endpoints :**
- `GET /v1/notifications` – Mes notifications (non lues en premier)
- `PATCH /v1/notifications/{id}/read` – Marquer comme lue
- `PATCH /v1/notifications/read-all` – Tout marquer comme lu

---

### 11. Export de données

Le header des maquettes montre un bouton **"Exporter les données"** présent sur :
- Liste des biens (DG)
- Tableau de bord comptable

**Endpoints :**
- `GET /v1/properties/export?format=csv` – Export CSV des biens
- `GET /v1/payments/export?format=csv&month=2026-04` – Export transactions/paiements
- `GET /v1/expenses/export?format=csv` – Export dépenses

---

### 12. Locataire – Scoring / Rating (écran détails)

L'écran "Détails locataire" (img6) montre un **score visuel** du locataire.

**Ce qui manque sur l'entité `Tenant` :**
- Champ `score` (calculé ou manuel, 0–100 ou 0–5 étoiles)
- `scoreBreakdown` : JSON avec critères (revenus, historique, garantie, ponctualité)
- Endpoint `GET /v1/tenants/{id}/score` ou inclure dans la réponse détail

---

## Migrations Flyway — état au 2026-04-21

| Migration | Statut | Tables concernées |
|-----------|--------|-------------------|
| V014 | ✅ Livré | `payments` |
| V015 | ✅ Livré | `expenses` |
| V016 | ✅ Livré | `user_sub_roles` |
| V017 | ✅ Livré | `hotels` + `hotel_id` sur `users` |
| V018 | ✅ Livré | `tickets`, `ticket_messages`, `ticket_attachments` |
| V019 | ✅ Livré | Seed `la_code_list` (TICKET_CATEGORY, TICKET_PRIORITY, TICKET_ATTACHMENT_TYPE) |
| **V020** | ⏳ À créer | `prospects` |
| **V021** | ⏳ À créer | `appointments` |
| **V022** | ⏳ À créer | `reservations` |
| **V023** | ⏳ À créer | `notifications` |
| **V024** | ⏳ À créer | Colonnes `score`, `score_breakdown` sur `tenants` |

---

## Ordre de priorité — mise à jour

| Priorité | Module | Statut | Justification |
|----------|--------|--------|---------------|
| ✅ P1 | **Property** | **Livré** | CRUD complet + médias + documents + modération |
| ✅ P1 | **Payment + Expense** | **Livré** | KPIs financiers opérationnels |
| ✅ P1 | **Dashboard Analytics** | **Livré** | KPIs agence par sous-rôle |
| ✅ P2 | **Sous-rôles agence** | **Livré** | RBAC niveau 2 complet |
| 🟠 P2 | **Ticketing Controllers** | En attente | Entités V018 prêtes — service/controller à créer |
| 🟠 P2 | **Prospects + Appointments** | En attente | Cœur du dashboard Commercial |
| 🟡 P3 | **Contrats Controllers** | En attente | Entité existante, effort faible |
| 🟡 P3 | **Notifications in-app** | En attente | UX importante |
| 🟡 P3 | **Export données** | En attente | Fonctionnalité complémentaire |
| 🟢 P4 | **Réservations** | En attente | À spécifier davantage |
| 🟢 P4 | **Scoring locataire** | En attente | Amélioration module Tenant |

---

## Résumé chiffré — mise à jour

| Catégorie | Modules | Statut |
|-----------|---------|--------|
| P1 critique | Property, Payment, Dashboard, Sous-rôles | ✅ **Tous livrés** |
| P2 important | Ticketing controllers, Prospects, Appointments | ⏳ En attente |
| P3-P4 secondaire | Contrats, Notifications, Export, Réservations, Scoring | ⏳ En attente |
| **Total restant** | **~7 modules** | **~45 endpoints à créer** |
