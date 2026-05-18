-- ============================================================
-- UBAX Platform – Nettoyage et rattachement des données TEST
-- Tables  : administrative.hotels + administrative.properties
-- Date    : 2026-05-18
-- Auteur  : Dev UBAX
-- ============================================================
-- PROBLÈMES CORRIGÉS :
--
--   Hotel le Cauris (3a64e0d0)
--     • city  : "Accra" (Ghana ?) → "ABIDJAN"
--     • country : "ML" (Mali)    → "CI" (Côte d'Ivoire)
--     • address : "Almadies 2" (Dakar) → "Rue des Lauriers, Cocody"
--     • phone : +221 (Sénégal) → +225 (Côte d'Ivoire)
--     • description / email / stars / total_rooms : corrigés
--
--   Noom Hôtel (52ee7dbd)
--     • phone : "+221221773987059" (trop long) → corrigé
--     • description / email / stars / total_rooms : corrigés
--
--   Hôtel La Gondole (86a374c6)
--     • city : "Dakar Corniche" → "DAKAR"
--     • email / stars / total_rooms : corrigés
--
-- RATTACHEMENTS hotel_id :
--   row 15 (65966c44 – HOTEL_ROOM,   Cocody ABJ)      → Hotel le Cauris
--   row 20 (bce6e652 – HOTEL_SUITE,  Corniche Ouest)  → Hôtel La Gondole
--   row 24 (ea5a1e81 – HOTEL_STUDIO, Almadies DAK)    → Noom Hôtel
-- ============================================================


-- ══════════════════════════════════════════════════════════════
-- PARTIE 1 – Mise à jour des hôtels
-- ══════════════════════════════════════════════════════════════

-- ── Hotel le Cauris | Cocody, Abidjan ────────────────────────
UPDATE administrative.hotels SET
    name                = 'Hôtel Le Cauris',
    description         = 'Hôtel boutique 3 étoiles niché au cœur de Cocody, à 5 min du Golf Club et des ambassades. 45 chambres climatisées avec WiFi haut débit, restaurant de cuisine ivoirienne et internationale, piscine extérieure et parking sécurisé. Idéal pour voyageurs d''affaires et familles en séjour à Abidjan.',
    registration_number = 'CI-ABJ-2024-00312',
    address             = 'Rue des Lauriers, Cocody',
    city                = 'ABIDJAN',
    country             = 'CI',
    phone               = '+22527224050',
    email               = 'contact@hotelcauris.ci',
    stars               = 3,
    total_rooms         = 45
WHERE id = '3a64e0d0-68af-41f7-b263-b8a175cf6873';


-- ── Noom Hôtel | Almadies, Dakar ─────────────────────────────
UPDATE administrative.hotels SET
    description         = 'Hôtel 5 étoiles au plateau des Almadies avec vue sur l''Atlantique. 150 chambres et suites contemporaines, 2 restaurants gastronomiques, rooftop bar, piscine à débordement et centre de bien-être. Standard international pour clientèle d''affaires et tourisme haut de gamme à Dakar.',
    registration_number = 'SN-DKR-2026-00100',
    address             = 'Route des Almadies, Plateau des Almadies',
    city                = 'DAKAR',
    country             = 'SN',
    phone               = '+221338601000',
    email               = 'reservations@noomhotel.sn',
    stars               = 5,
    total_rooms         = 150
WHERE id = '52ee7dbd-902f-47ae-a76b-325afc479105';


-- ── Hôtel La Gondole | Corniche Ouest, Dakar ─────────────────
UPDATE administrative.hotels SET
    description         = 'Hôtel 4 étoiles en bord de mer sur la Corniche Ouest de Dakar. 100 chambres avec vue Atlantique, piscine à débordement, restaurant de fruits de mer, bar et parking privatif. Architecture senegalaise moderne, à 15 min du centre-ville et 20 min de l''aéroport Blaise Diagne.',
    address             = 'Route de la Corniche Ouest',
    city                = 'DAKAR',
    country             = 'SN',
    phone               = '+221770000022',
    email               = 'contact@hotellagondole.sn',
    stars               = 4,
    total_rooms         = 100
WHERE id = '86a374c6-7428-49be-bf2b-5e3da33d3e44';


-- ══════════════════════════════════════════════════════════════
-- PARTIE 2 – Rattachement hotel_id sur les biens hôteliers
-- ══════════════════════════════════════════════════════════════

-- ── ROW 15 | Chambre Deluxe, Cocody ABJ → Hotel le Cauris ────
UPDATE administrative.properties
SET hotel_id = '3a64e0d0-68af-41f7-b263-b8a175cf6873'
WHERE id = '65966c44-f2df-46f0-83fe-9d045c461149';


-- ── ROW 20 | Suite Junior, Corniche Ouest → Hôtel La Gondole ─
UPDATE administrative.properties
SET hotel_id = '86a374c6-7428-49be-bf2b-5e3da33d3e44'
WHERE id = 'bce6e652-1e8e-47e2-a3e4-c93649362d75';


-- ── ROW 24 | Studio Almadies, Dakar → Noom Hôtel ─────────────
UPDATE administrative.properties
SET hotel_id = '52ee7dbd-902f-47ae-a76b-325afc479105'
WHERE id = 'ea5a1e81-106b-4fd4-a911-54facb22f1a6';


-- ══════════════════════════════════════════════════════════════
-- VÉRIFICATION RAPIDE (optionnel — à exécuter séparément)
-- ══════════════════════════════════════════════════════════════
-- SELECT h.name, h.city, h.country, h.stars, h.total_rooms,
--        p.title, p.property_type, p.transaction_type, p.city AS prop_city
-- FROM administrative.hotels h
-- LEFT JOIN administrative.properties p ON p.hotel_id = h.id
-- ORDER BY h.name, p.title;
