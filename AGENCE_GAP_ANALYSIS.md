# Analyse des Manques – Intégration Espace Agence UBAX

> Généré le : 2026-04-20  
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
| Partner (candidature) | ✅ | ✅ | ✅ | Complet |
| Tenant (KYC locataire) | ✅ | ✅ | ✅ | Complet |
| Property (biens) | ✅ | ❌ | ❌ | **Entités seules** |
| Contract (contrats) | ✅ | ❌ | ❌ | **Entités seules** |
| Ticketing | ✅ | ❌ | ❌ | **Entités seules** |
| Document (PDF) | ✅ | ❌ | ❌ | **Entités seules** |
| Payment | ❌ | ❌ | ❌ | **Vide** |
| Analytics / Dashboard | ❌ | ❌ | ❌ | **Absent** |
| Appointments / Agenda | ❌ | ❌ | ❌ | **Absent** |
| Prospects | ❌ | ❌ | ❌ | **Absent** |
| Dépenses comptables | ❌ | ❌ | ❌ | **Absent** |
| Rôles internes agence | ❌ | – | – | **Absent** |
| Notifications in-app | ❌ | ❌ | ❌ | **Absent** |

---

## Détail des manques par fonctionnalité

---

### 1. Sous-rôles internes de l'agence

**Ce qui manque :**
- Un enum `AgencyStaffRole` (ou extension de `UbaxAdminRole`) avec : `DIRECTOR`, `COMMERCIAL`, `ACCOUNTANT`, `SAV_AGENT`
- Champ `agencyRole` sur l'entité `User` (ou table de jointure `AgencyMembership`)
- Endpoints de gestion de l'équipe agence :
  - `POST /v1/agency/team` – inviter un collaborateur
  - `GET /v1/agency/team` – lister les membres
  - `PUT /v1/agency/team/{userId}/role` – changer le rôle
  - `DELETE /v1/agency/team/{userId}` – retirer un membre
- `RoleGuard` adapté pour vérifier le rôle interne agence (en plus du rôle Keycloak `UBAX_PARTNER`)

---

### 2. Module Biens (Property) – Controllers & Services

**Ce qui manque (entités existent déjà) :**

#### 2a. CRUD des biens
- `POST /v1/properties` – Création multi-step avec upload photos/docs
- `GET /v1/properties` – Liste paginée + filtres (type, statut, ville, prix)
- `GET /v1/properties/{id}` – Détail d'un bien
- `PUT /v1/properties/{id}` – Mise à jour
- `DELETE /v1/properties/{id}` – Suppression/archivage
- `PATCH /v1/properties/{id}/status` – Changement de statut (publication, archivage…)

#### 2b. Médias & géolocalisation
- `POST /v1/properties/{id}/media` – Upload multiple photos/vidéos
- `DELETE /v1/properties/{id}/media/{mediaId}` – Supprimer un média
- `PATCH /v1/properties/{id}/media/{mediaId}/cover` – Définir la photo de couverture
- Champs `latitude` / `longitude` déjà présents → endpoint de géocodage ou validation des coordonnées

#### 2c. Récapitulatif avant publication (Step 3 du formulaire UI)
- `GET /v1/properties/{id}/preview` – Retourne une vue complète pour confirmation

#### 2d. Détails bailleur & locataire liés au bien
- `GET /v1/properties/{id}/owner` – Profil du propriétaire
- `GET /v1/properties/{id}/tenant` – Profil du locataire actuel + scoring

---

### 3. Module Paiements (vide)

C'est le module le plus critique pour le **Comptable** et le **DG**.

**Entités à créer :**
```
Payment
  - contract (FK), tenant (FK), amount, dueDate, paidDate
  - paymentType (RENT | DEPOSIT | CHARGES | COMMISSION | SALE)
  - paymentMethod (CASH | BANK_TRANSFER | MOBILE_MONEY | CHECK)
  - status (PENDING | PAID | LATE | PARTIAL | CANCELLED)
  - reference, receiptUrl, note

Expense (Dépense)
  - agency (FK), property (FK, nullable), createdBy (FK User)
  - category (SOLUTION | MARKETING | SALARY | UGON | OTHER)
  - amount, paymentMethod, date
  - provider (prestataire), invoiceReference
  - costCenter (AGENCE_GENERAL | PROPERTY_SPECIFIC)
  - justificationUrl
```

**Endpoints à créer :**
- `GET /v1/payments` – Liste des paiements (filtres : statut, mois, bien)
- `POST /v1/payments` – Enregistrer un paiement
- `GET /v1/payments/late` – Loyers en retard
- `GET /v1/expenses` – Liste des dépenses du mois
- `POST /v1/expenses` – Ajouter une dépense (formulaire avec pièce jointe)
- `DELETE /v1/expenses/{id}` – Supprimer une dépense

---

### 4. Module Analytics / Dashboard

Chaque rôle a un tableau de bord avec des KPIs distincts. Aucune couche analytics n'existe.

**Endpoints à créer :**

#### DG / Directeur Général
- `GET /v1/dashboard/dg/summary` → 
  ```json
  { "totalProperties": 45, "activeListings": 10, "rentedProperties": 32, "soldProperties": 2 }
  ```
- `GET /v1/dashboard/dg/revenue-flux?period=monthly` – Flux de revenus (données graphique)
- `GET /v1/dashboard/dg/transactions/recent` – Dernières transactions

#### Commercial
- `GET /v1/dashboard/commercial/summary` →
  ```json
  { "totalProperties": 120, "newProspects": 15, "appointments": 15, "closedDeals": 6 }
  ```
- `GET /v1/dashboard/commercial/properties-state` – État des biens par statut (barres de progression)
- `GET /v1/dashboard/commercial/prospect-activity?period=week` – Activité prospects (graphique)

#### Comptable
- `GET /v1/dashboard/accountant/summary` →
  ```json
  { "monthlyRevenue": 13750000, "unpaidRents": 9750000, "pendingPayments": 4000000, "agencyCommission": 1750000 }
  ```
- `GET /v1/dashboard/accountant/revenue-evolution` – Courbe revenus sur 12 mois
- `GET /v1/dashboard/accountant/revenue-breakdown` – Répartition (loyers durée / locations / ventes)
- `GET /v1/dashboard/accountant/expenses-by-category` – Dépenses du mois par catégorie
- `GET /v1/dashboard/accountant/late-payments` – Paiements en retard avec détails locataires

#### Agent SAV
- `GET /v1/dashboard/sav/summary` – Stats tickets ouverts, en cours, résolus
- `GET /v1/dashboard/sav/team` – Vue équipe agents avec tickets assignés

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

## Migrations Flyway nécessaires

Chaque nouveau module nécessite une migration versionnée :

| Migration | Tables concernées |
|-----------|-------------------|
| `V6__create_payments.sql` | `payments` |
| `V7__create_expenses.sql` | `expenses` |
| `V8__create_prospects.sql` | `prospects` |
| `V9__create_appointments.sql` | `appointments` |
| `V10__create_reservations.sql` | `reservations` |
| `V11__create_notifications.sql` | `notifications` |
| `V12__add_agency_staff_role.sql` | Colonne `agency_role` sur `users` |
| `V13__add_tenant_score.sql` | Colonnes `score`, `score_breakdown` sur `tenants` |

---

## Ordre de priorité recommandé

| Priorité | Module | Justification |
|----------|--------|---------------|
| 🔴 P1 | **Property Controllers + Services** | Bloquant pour tous les dashboards (KPIs basés sur les biens) |
| 🔴 P1 | **Payment Module** | Bloquant pour le Comptable et les KPIs revenus du DG |
| 🔴 P1 | **Dashboard Analytics** | Raison d'être de l'espace agence |
| 🟠 P2 | **Ticketing Controllers** | Opérationnel pour l'Agent SAV |
| 🟠 P2 | **Sous-rôles agence** | Nécessaire pour le RBAC par tableau de bord |
| 🟠 P2 | **Prospects + Appointments** | Cœur du dashboard Commercial |
| 🟡 P3 | **Contrats Controllers** | Déjà modélisé, effort faible |
| 🟡 P3 | **Notifications in-app** | UX importante, effort modéré |
| 🟡 P3 | **Export données** | Fonctionnalité complémentaire |
| 🟢 P4 | **Réservations** | À spécifier davantage |
| 🟢 P4 | **Scoring locataire** | Amélioration du module Tenant existant |

---

## Résumé chiffré

| Catégorie | Modules manquants | Entités à créer | Endpoints à créer |
|-----------|-------------------|-----------------|-------------------|
| Critique (P1) | 3 | 2 | ~25 |
| Important (P2) | 3 | 3 | ~20 |
| Secondaire (P3-P4) | 4 | 2 | ~15 |
| **Total** | **10** | **7** | **~60** |
