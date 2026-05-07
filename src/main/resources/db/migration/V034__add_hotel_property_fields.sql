-- ============================================================
-- V034 : Champs hôtel sur properties + code lists hôtel
-- Nouveaux types de biens, formules, types de lits, fréquence
-- ============================================================

-- ── PROPERTY_TYPE : valeurs hôtel ─────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES
  ('a3674e2b-2108-4f7e-8239-4a7d58190ac9', 'PROPERTY_TYPE', 'HOTEL_ROOM',       'Chambre d''hôtel',                     TRUE),
  ('cb1b5df8-030f-4320-b7d4-2a01c871da80', 'PROPERTY_TYPE', 'HOTEL_SUITE',      'Suite hôtelière',                       TRUE),
  ('c5aa85cf-f97a-4299-a854-06bc9b8f069d', 'PROPERTY_TYPE', 'HOTEL_STUDIO',     'Studio / chambre avec kitchenette',     TRUE),
  ('a826fee7-c333-4592-9459-bcf7e4beb1b9', 'PROPERTY_TYPE', 'EVENT_SPACE',      'Salle événementielle',                  TRUE),
  ('adc798d7-fdd8-425d-a665-aa4ac319fa05', 'PROPERTY_TYPE', 'CONFERENCE_ROOM',  'Salle de conférence',                   TRUE),
  ('251477da-792c-44a5-bfe4-2d1e4ddfa273', 'PROPERTY_TYPE', 'RESTAURANT_SPACE', 'Espace restaurant / bar',               TRUE);

-- ── TRANSACTION_TYPE : séjour court (hôtel) ───────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES
  ('81a85124-18a6-4beb-937c-b7a02fc4cbaa', 'TRANSACTION_TYPE', 'SHORT_STAY', 'Séjour court (nuitée / semaine)', TRUE);

-- ── BED_TYPE ──────────────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES
  ('6b3d5135-6263-4396-adb0-1ce56f4bb844', 'BED_TYPE', 'SINGLE', 'Lit simple (1 personne)',         TRUE),
  ('6a005a2f-ba96-4ba7-8491-80a6567b9ac2', 'BED_TYPE', 'DOUBLE', 'Grand lit double',                TRUE),
  ('39a0dfe5-41b5-4d43-b9d0-4f75f881a351', 'BED_TYPE', 'TWIN',   'Lits jumeaux (2 lits simples)',   TRUE),
  ('79ebb578-677b-4844-9759-e2a7a0f85a47', 'BED_TYPE', 'KING',   'Lit king size',                   TRUE),
  ('26a0c962-9737-4f75-9c83-39dc9a345ac1', 'BED_TYPE', 'QUEEN',  'Lit queen size',                  TRUE),
  ('55f172b1-0e72-4ed5-8d78-ec807614366c', 'BED_TYPE', 'BUNK',   'Lits superposés',                 TRUE);

-- ── MEAL_PLAN ─────────────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES
  ('a3c1fad9-93fb-4f53-8c60-e4ffda124258', 'MEAL_PLAN', 'ROOM_ONLY',    'Chambre seule (sans repas)',               TRUE),
  ('463a83ca-1c46-4ea3-a647-6ea51b18dcb4', 'MEAL_PLAN', 'BREAKFAST',    'Petit-déjeuner inclus',                   TRUE),
  ('490bf288-ccf5-4252-8279-8300acdeef1b', 'MEAL_PLAN', 'HALF_BOARD',   'Demi-pension (petit-déjeuner + dîner)',   TRUE),
  ('6cad56ce-f7ce-4a6b-95e7-531350f82a91', 'MEAL_PLAN', 'FULL_BOARD',   'Pension complète (3 repas/jour)',          TRUE),
  ('51fc4df6-4392-456d-b3ff-b8f96b28fcee', 'MEAL_PLAN', 'ALL_INCLUSIVE', 'Tout inclus',                            TRUE);

-- ── PAYMENT_FREQUENCY ─────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES
  ('fd8fdfe6-373d-452e-bf1a-1f0c87a97ef5', 'PAYMENT_FREQUENCY', 'NIGHTLY', 'Prix par nuit',    TRUE),
  ('c566ef5e-cbcf-45d0-b6bb-4814ac9a1957', 'PAYMENT_FREQUENCY', 'WEEKLY',  'Prix par semaine', TRUE),
  ('62b0e3c5-281c-4566-8862-8addd3ee344f', 'PAYMENT_FREQUENCY', 'MONTHLY', 'Prix par mois',    TRUE);

-- ── Colonnes hôtel sur properties ─────────────────────────────
ALTER TABLE administrative.properties
  ADD COLUMN IF NOT EXISTS bed_type          VARCHAR(20),
  ADD COLUMN IF NOT EXISTS max_occupancy     INTEGER,
  ADD COLUMN IF NOT EXISTS meal_plan         VARCHAR(30),
  ADD COLUMN IF NOT EXISTS payment_frequency VARCHAR(20);
