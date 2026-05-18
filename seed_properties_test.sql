-- ============================================================
-- UBAX Platform – Nettoyage et réajustement des données TEST
-- Table  : administrative.properties
-- Date   : 2026-05-18
-- Auteur : Dev UBAX
-- ============================================================
-- COUVERTURE DES CAS DE TEST :
--
--   SALE           → contrats SALE          : rows 1, 2, 4, 7, 11, 14, 17, 18(ARCHIVED), 21(ARCHIVED)
--   RENT           → contrats LEASE / RENT_TO_OWN : rows 3, 8, 10, 12, 13, 19, 23
--   RENT_FURNISHED → contrats LEASE         : rows 5, 6, 9, 16, 22
--   SHORT_STAY     → réservations hôtel     : rows 15 (NIGHTLY), 20 (WEEKLY), 24 (NIGHTLY)
--
-- Prix en XOF (Franc CFA) :
--   SALE      Abidjan : 42M – 250M  |  Dakar : 185M
--   RENT      Abidjan : 80k – 250k/mois  |  Thiès : 80k
--   RENT_FURNISHED Abidjan/Dakar : 350k – 800k/mois
--   SHORT_STAY NIGHTLY : 25k – 35k/nuit  |  WEEKLY : 500k/semaine
-- ============================================================


-- ── ROW 1 | 0258e510 | PUBLISHED | SALE | APARTMENT | ABIDJAN ────────────
UPDATE administrative.properties SET
    title             = 'Appartement 4 pièces à Riviera 3 – Abidjan',
    description       = 'Grand appartement en résidence sécurisée à Riviera 3. Volumes généreux, finitions soignées, parking couvert. Idéal résidence principale ou investissement locatif haut de gamme.',
    property_type     = 'APARTMENT',
    transaction_type  = 'SALE',
    price             = 65000000.00,
    condition         = 'GOOD',
    year_built        = 2014,
    surface_total     = 280.00,
    surface_living    = 210.00,
    rooms             = 6,
    bedrooms          = 4,
    bathrooms         = 2,
    balconies         = 1,
    city              = 'ABIDJAN',
    district          = 'Riviera 3',
    street            = 'Boulevard Latrille',
    address           = 'Résidence Les Palmiers, Appt 6B, Riviera 3, Abidjan',
    latitude          = 5.35680000,
    longitude         = -3.96690000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '0258e510-0051-46a8-81eb-0eb4953c8459';


-- ── ROW 2 | 02deb456 | PENDING | SALE | VILLA | ABIDJAN Cocody ───────────
UPDATE administrative.properties SET
    title             = 'Villa 5 chambres avec piscine – Cocody, Abidjan',
    description       = 'Villa contemporaine R+1 avec piscine à débordement et jardin tropical en résidence fermée de Cocody. Double garage, groupe électrogène, à 5 min des écoles internationales.',
    property_type     = 'VILLA',
    transaction_type  = 'SALE',
    price             = 120000000.00,
    condition         = 'GOOD',
    year_built        = 2016,
    surface_total     = 500.00,
    surface_living    = 350.00,
    rooms             = 8,
    bedrooms          = 5,
    bathrooms         = 4,
    balconies         = 2,
    city              = 'ABIDJAN',
    district          = 'Cocody',
    street            = 'Rue des Bougainvilliers',
    address           = 'Villa 12, Rue des Bougainvilliers, Cocody, Abidjan',
    latitude          = 5.35200000,
    longitude         = -3.99100000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '02deb456-20fc-44cb-9f9e-7fa70afaf6e0';


-- ── ROW 3 | 16b3062e | PUBLISHED | RENT | APARTMENT | ABIDJAN Angré ──────
UPDATE administrative.properties SET
    title             = 'Appartement 3 pièces à louer – Cocody Angré, Abidjan',
    description       = 'Appartement traversant lumineux au 3ème étage, balcon et vue dégagée. Proche marché d''Angré, bus directs Plateau. Eau et gardiennage inclus dans les charges mensuelles.',
    property_type     = 'APARTMENT',
    transaction_type  = 'RENT',
    price             = 250000.00,
    condition         = 'GOOD',
    year_built        = 2015,
    surface_total     = 85.00,
    surface_living    = 75.00,
    rooms             = 3,
    bedrooms          = 2,
    bathrooms         = 1,
    balconies         = 1,
    city              = 'ABIDJAN',
    district          = 'Cocody Angré',
    street            = 'Rue des Anges',
    address           = 'Résidence Angré Palace, Appt 3A, Cocody, Abidjan',
    latitude          = 5.37890000,
    longitude         = -3.97430000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '16b3062e-cbf1-41ee-bc4e-2d0bdb309562';


-- ── ROW 4 | 2dc5e94c | PENDING | SALE | APARTMENT | ABIDJAN Plateau ──────
UPDATE administrative.properties SET
    title             = 'Appartement moderne 4 pièces – Plateau, Abidjan',
    description       = 'Appartement de standing au cœur du Plateau, à proximité des ambassades et centres d''affaires. Immeuble avec ascenseur, parking souterrain et local sécurisé.',
    property_type     = 'APARTMENT',
    transaction_type  = 'SALE',
    price             = 55000000.00,
    condition         = 'GOOD',
    year_built        = 2018,
    surface_total     = 140.00,
    surface_living    = 120.00,
    rooms             = 4,
    bedrooms          = 3,
    bathrooms         = 2,
    balconies         = 1,
    city              = 'ABIDJAN',
    district          = 'Plateau',
    street            = 'Avenue Lamblin',
    address           = 'Tour Résidence Plateau, Appt 8C, Avenue Lamblin, Abidjan',
    latitude          = 5.32100000,
    longitude         = -4.01700000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '2dc5e94c-b609-4277-b408-ac22f4a29b4a';


-- ── ROW 5 | 37829f94 | PENDING | RENT_FURNISHED | APARTMENT | ABIDJAN ─────
UPDATE administrative.properties SET
    title             = 'Studio meublé haut standing – Plateau, Abidjan',
    description       = 'Studio entièrement meublé et équipé au cœur du Plateau. Cuisine américaine, climatisation, WiFi et sécurité 24h/24 inclus. Idéal pour cadre expatrié ou voyageur d''affaires de passage.',
    property_type     = 'APARTMENT',
    transaction_type  = 'RENT_FURNISHED',
    price             = 450000.00,
    condition         = 'NEW',
    year_built        = 2020,
    surface_total     = 45.00,
    surface_living    = 40.00,
    rooms             = 1,
    bedrooms          = 1,
    bathrooms         = 1,
    balconies         = 0,
    city              = 'ABIDJAN',
    district          = 'Plateau',
    street            = 'Rue du Commerce',
    address           = 'Résidence Cristal, Studio 4F, Rue du Commerce, Plateau, Abidjan',
    latitude          = 5.31940000,
    longitude         = -4.02060000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '37829f94-5d4a-49fd-97ac-a014f80d5a7a';


-- ── ROW 6 | 394e1b94 | PUBLISHED | RENT_FURNISHED | VILLA | DAKAR Mermoz ──
UPDATE administrative.properties SET
    title             = 'Villa meublée 6 chambres avec piscine – Mermoz, Dakar',
    description       = 'Grande villa meublée haut standing avec piscine, jardin paysager et double garage en résidence sécurisée de Mermoz. À 10 min des plages des Almadies. Personnel de maison disponible.',
    property_type     = 'VILLA',
    transaction_type  = 'RENT_FURNISHED',
    price             = 800000.00,
    condition         = 'GOOD',
    year_built        = 2000,
    surface_total     = 1200.00,
    surface_living    = 850.00,
    rooms             = 10,
    bedrooms          = 6,
    bathrooms         = 4,
    balconies         = 4,
    city              = 'DAKAR',
    district          = 'Mermoz',
    street            = 'Avenue Cheikh Anta Diop',
    address           = 'Villa Les Baobabs, Rue 10 x 13, Mermoz, Dakar',
    latitude          = 14.71930000,
    longitude         = -17.47290000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '394e1b94-6d87-41b2-8b31-031a9f45944d';


-- ── ROW 7 | 3a7d0ae5 | PUBLISHED | SALE | APARTMENT | ABIDJAN Riviera 3 ──
UPDATE administrative.properties SET
    title             = 'Appartement neuf 4 pièces – Riviera 3, Abidjan',
    description       = 'Appartement neuf livré clé en main dans une résidence fermée avec gardien. Séjour lumineux, cuisine équipée, grande terrasse avec vue sur quartier. Parking couvert privatif.',
    property_type     = 'APARTMENT',
    transaction_type  = 'SALE',
    price             = 52000000.00,
    condition         = 'NEW',
    year_built        = 2021,
    surface_total     = 280.00,
    surface_living    = 210.00,
    rooms             = 6,
    bedrooms          = 4,
    bathrooms         = 2,
    balconies         = 1,
    city              = 'ABIDJAN',
    district          = 'Riviera 3',
    street            = 'Boulevard Latrille',
    address           = 'Résidence Magnolia, Appt 2A, Boulevard Latrille, Riviera 3, Abidjan',
    latitude          = 5.35500000,
    longitude         = -3.96800000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '3a7d0ae5-a747-4794-b02e-c84231501dae';


-- ── ROW 8 | 3c0cf974 | PENDING | RENT | APARTMENT | ABIDJAN Angré ─────────
UPDATE administrative.properties SET
    title             = 'Appartement 3 pièces à louer – Angré, Abidjan',
    description       = 'Appartement bien entretenu avec balcon dans immeuble récent à Angré. À proximité du marché, transports et école. Charges (eau, gardien) en sus du loyer mensuel.',
    property_type     = 'APARTMENT',
    transaction_type  = 'RENT',
    price             = 200000.00,
    condition         = 'GOOD',
    year_built        = 2012,
    surface_total     = 90.00,
    surface_living    = 80.00,
    rooms             = 3,
    bedrooms          = 2,
    bathrooms         = 1,
    balconies         = 1,
    city              = 'ABIDJAN',
    district          = 'Angré',
    street            = 'Rue des Tulipes',
    address           = 'Immeuble Tulipes, Appt 5, Angré, Abidjan',
    latitude          = 5.38210000,
    longitude         = -3.97150000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '3c0cf974-32df-49b9-b4bf-c61e7ba7823e';


-- ── ROW 9 | 42471d52 | PENDING | RENT_FURNISHED | APARTMENT | ABIDJAN ─────
UPDATE administrative.properties SET
    title             = 'Appartement meublé 3 pièces – Marcory Zone 4, Abidjan',
    description       = 'Appartement meublé de standing à Marcory Zone 4. Salon climatisé, cuisine équipée, WiFi inclus. Résidence sécurisée. Idéal pour expat ou cadre en mission courte ou longue durée.',
    property_type     = 'APARTMENT',
    transaction_type  = 'RENT_FURNISHED',
    price             = 350000.00,
    condition         = 'GOOD',
    year_built        = 2017,
    surface_total     = 95.00,
    surface_living    = 85.00,
    rooms             = 3,
    bedrooms          = 2,
    bathrooms         = 1,
    balconies         = 1,
    city              = 'ABIDJAN',
    district          = 'Marcory Zone 4',
    street            = 'Avenue 15',
    address           = 'Résidence Zone 4 Palace, Appt 7B, Marcory, Abidjan',
    latitude          = 5.29950000,
    longitude         = -3.99060000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '42471d52-32f8-4ad4-9670-58532ed8b7d7';


-- ── ROW 10 | 4b51c312 | PUBLISHED | RENT | APARTMENT | ABIDJAN Deux Plateaux
UPDATE administrative.properties SET
    title             = 'Appartement 2 pièces à louer – Deux Plateaux, Abidjan',
    description       = 'Appartement propre de 2 pièces dans quartier calme des Deux Plateaux. Immeuble récent avec gardien. Idéal pour jeune couple ou professionnel en poste à Abidjan.',
    property_type     = 'APARTMENT',
    transaction_type  = 'RENT',
    price             = 180000.00,
    condition         = 'GOOD',
    year_built        = 2015,
    surface_total     = 60.00,
    surface_living    = 55.00,
    rooms             = 2,
    bedrooms          = 1,
    bathrooms         = 1,
    balconies         = 1,
    city              = 'ABIDJAN',
    district          = 'Deux Plateaux',
    street            = 'Rue Chevalier de Clieu',
    address           = 'Résidence Les Plateaux, Appt 4A, Deux Plateaux, Abidjan',
    latitude          = 5.36300000,
    longitude         = -3.98200000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '4b51c312-7654-499b-9257-383fe532cf4d';


-- ── ROW 11 | 4e0f9103 | PUBLISHED | SALE | VILLA | ABIDJAN Riviera Golf ──
UPDATE administrative.properties SET
    title             = 'Villa de prestige avec piscine – Riviera Golf, Abidjan',
    description       = 'Villa d''exception avec vue sur le golf de la Riviera. Architecture contemporaine, piscine à débordement, domotique complète. Résidence ultra-sécurisée, accès contrôlé 24h/24.',
    property_type     = 'VILLA',
    transaction_type  = 'SALE',
    price             = 250000000.00,
    condition         = 'NEW',
    year_built        = 2022,
    surface_total     = 1500.00,
    surface_living    = 800.00,
    rooms             = 12,
    bedrooms          = 6,
    bathrooms         = 5,
    balconies         = 4,
    city              = 'ABIDJAN',
    district          = 'Riviera Golf',
    street            = 'Allée des Palmiers',
    address           = 'Résidence Golf Premium, Villa 3, Riviera Golf, Abidjan',
    latitude          = 5.37500000,
    longitude         = -3.96100000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '4e0f9103-2d57-44a6-b58b-442c9da7d180';


-- ── ROW 12 | 4e781d5e | PUBLISHED | RENT | APARTMENT | ABIDJAN Yopougon ──
UPDATE administrative.properties SET
    title             = 'Appartement 3 pièces économique – Yopougon Attié, Abidjan',
    description       = 'Appartement fonctionnel de 3 pièces dans immeuble bien entretenu à Yopougon Attié. Quartier animé, proche commerces et transports communs. Parfait pour famille à budget modéré.',
    property_type     = 'APARTMENT',
    transaction_type  = 'RENT',
    price             = 120000.00,
    condition         = 'GOOD',
    year_built        = 2008,
    surface_total     = 75.00,
    surface_living    = 65.00,
    rooms             = 3,
    bedrooms          = 2,
    bathrooms         = 1,
    balconies         = 0,
    city              = 'ABIDJAN',
    district          = 'Yopougon Attié',
    street            = 'Boulevard de Yopougon',
    address           = 'Immeuble Attié, Appt 2B, Boulevard de Yopougon, Abidjan',
    latitude          = 5.33600000,
    longitude         = -4.07300000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '4e781d5e-a743-43c7-8aaf-5c82f3d192a6';


-- ── ROW 13 | 6162c46a | PUBLISHED | RENT | APARTMENT | THIES ────────────
-- Correction des coordonnées (pointaient sur Abidjan → remplacées par Thiès)
UPDATE administrative.properties SET
    title             = 'Appartement 3 pièces à louer – Médina Fall, Thiès',
    description       = 'Appartement de 3 pièces bien entretenu dans résidence calme de Médina Fall. Idéal pour fonctionnaire ou enseignant. Proche gare ferroviaire et marchés centraux de Thiès.',
    property_type     = 'APARTMENT',
    transaction_type  = 'RENT',
    price             = 80000.00,
    condition         = 'GOOD',
    year_built        = 2010,
    surface_total     = 80.00,
    surface_living    = 70.00,
    rooms             = 3,
    bedrooms          = 2,
    bathrooms         = 1,
    balconies         = 1,
    city              = 'THIES',
    district          = 'Médina Fall',
    street            = 'Rue Malick Sy',
    address           = 'Résidence Fall, Appt 1C, Médina Fall, Thiès, Sénégal',
    latitude          = 14.79160000,
    longitude         = -16.93520000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '6162c46a-85e6-4171-97b9-fe1e0722d549';


-- ── ROW 14 | 643632cb | PUBLISHED | SALE | LAND | ABIDJAN Bingerville ─────
UPDATE administrative.properties SET
    title             = 'Terrain à bâtir 500 m² titré – Bingerville, Abidjan',
    description       = 'Terrain viabilisé de 500 m² en lotissement résidentiel à Bingerville. Accès goudronné, bornes eau et électricité disponibles. Titre foncier en ordre, prêt à construire immédiatement.',
    property_type     = 'LAND',
    transaction_type  = 'SALE',
    price             = 18000000.00,
    condition         = 'NEW',
    year_built        = NULL,
    surface_total     = 500.00,
    surface_living    = NULL,
    rooms             = 0,
    bedrooms          = 0,
    bathrooms         = 0,
    balconies         = 0,
    city              = 'ABIDJAN',
    district          = 'Bingerville',
    street            = 'Rue du Lotissement Nord',
    address           = 'Lot 12, Lotissement Résidentiel Nord, Bingerville, Abidjan',
    latitude          = 5.35670000,
    longitude         = -3.87400000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '643632cb-a453-4939-84ba-1a97d094d3cb';


-- ── ROW 15 | 65966c44 | PUBLISHED | SHORT_STAY | HOTEL_ROOM | ABIDJAN ─────
-- Correction : APARTMENT SALE → HOTEL_ROOM SHORT_STAY NIGHTLY
-- Correction : coordonnées Sahara → Cocody Abidjan
UPDATE administrative.properties SET
    title             = 'Chambre Deluxe vue piscine – Hôtel Ivoire, Abidjan',
    description       = 'Chambre deluxe entièrement rénovée avec vue directe sur la piscine olympique de l''Hôtel Ivoire. Lit double, climatisation, minibar, TV satellite et accès spa inclus.',
    property_type     = 'HOTEL_ROOM',
    transaction_type  = 'SHORT_STAY',
    price             = 35000.00,
    condition         = 'NEW',
    year_built        = 2020,
    surface_total     = 35.00,
    surface_living    = 30.00,
    rooms             = 1,
    bedrooms          = 1,
    bathrooms         = 1,
    balconies         = 1,
    city              = 'ABIDJAN',
    district          = 'Cocody',
    street            = 'Boulevard de France',
    address           = 'Hôtel Ivoire, Boulevard de France, Cocody, Abidjan',
    latitude          = 5.35400000,
    longitude         = -3.99800000,
    bed_type          = 'DOUBLE',
    max_occupancy     = 2,
    meal_plan         = 'BREAKFAST',
    payment_frequency = 'NIGHTLY'
WHERE id = '65966c44-f2df-46f0-83fe-9d045c461149';


-- ── ROW 16 | 680092f4 | PUBLISHED | RENT_FURNISHED | APARTMENT | ABIDJAN ──
UPDATE administrative.properties SET
    title             = 'Appartement meublé 4 pièces – Riviera 2, Abidjan',
    description       = 'Appartement meublé haut de gamme en résidence sécurisée à Riviera 2. Salon spacieux, cuisine américaine équipée, master bedroom avec dressing. Idéal famille ou expatrié en mission longue durée.',
    property_type     = 'APARTMENT',
    transaction_type  = 'RENT_FURNISHED',
    price             = 400000.00,
    condition         = 'GOOD',
    year_built        = 2016,
    surface_total     = 140.00,
    surface_living    = 120.00,
    rooms             = 6,
    bedrooms          = 4,
    bathrooms         = 2,
    balconies         = 1,
    city              = 'ABIDJAN',
    district          = 'Riviera 2',
    street            = 'Rue des Orchidées',
    address           = 'Résidence Les Orchidées, Appt 3D, Riviera 2, Abidjan',
    latitude          = 5.36100000,
    longitude         = -3.97500000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '680092f4-c5d8-4b1b-98bc-193a0358ced4';


-- ── ROW 17 | 68fc85df | PENDING | SALE | HOUSE | ABIDJAN Port-Bouët ───────
UPDATE administrative.properties SET
    title             = 'Maison 4 chambres en vente – Port-Bouët, Abidjan',
    description       = 'Maison individuelle de plain-pied avec cour arborée et dépendances à Port-Bouët. Proche aéroport international Félix Houphouët-Boigny. Idéale pour famille ou investissement locatif.',
    property_type     = 'HOUSE',
    transaction_type  = 'SALE',
    price             = 42000000.00,
    condition         = 'GOOD',
    year_built        = 2005,
    surface_total     = 300.00,
    surface_living    = 180.00,
    rooms             = 6,
    bedrooms          = 4,
    bathrooms         = 2,
    balconies         = 0,
    city              = 'ABIDJAN',
    district          = 'Port-Bouët',
    street            = 'Rue de l''Aéroport',
    address           = 'Villa 7, Rue de l''Aéroport, Port-Bouët, Abidjan',
    latitude          = 5.26400000,
    longitude         = -3.92500000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '68fc85df-8734-4c2a-af4e-11e23635c5db';


-- ── ROW 18 | 7af0b0ae | ARCHIVED | SALE | VILLA | DAKAR Sacré-Cœur 3 ──────
-- Statut ARCHIVED conservé — données corrigées pour cohérence
UPDATE administrative.properties SET
    title             = 'Villa R+1 avec piscine – Sacré-Cœur 3, Dakar',
    description       = 'Belle villa de prestige sur deux niveaux, clôturée avec portail électrique. Piscine, jardin paysager, 5 chambres en suite. Rue calme et sécurisée, à 15 min du centre-ville de Dakar. Annonce archivée.',
    property_type     = 'VILLA',
    transaction_type  = 'SALE',
    price             = 185000000.00,
    condition         = 'GOOD',
    year_built        = 2017,
    surface_total     = 350.00,
    surface_living    = 240.00,
    rooms             = 7,
    bedrooms          = 5,
    bathrooms         = 3,
    balconies         = 2,
    city              = 'DAKAR',
    district          = 'Sacré-Cœur 3',
    street            = 'Rue 10',
    address           = 'Villa 14, Rue 10 x 13, Sacré-Cœur 3, Dakar',
    latitude          = 14.71930000,
    longitude         = -17.47290000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = '7af0b0ae-f39c-4ff7-bafe-716ed983e250';


-- ── ROW 19 | b31a807b | PUBLISHED | RENT | APARTMENT | ABIDJAN Port-Bouët ─
UPDATE administrative.properties SET
    title             = 'Appartement 3 pièces à louer – Port-Bouët, Abidjan',
    description       = 'Appartement calme et bien entretenu à 10 min de l''aéroport d''Abidjan. Idéal pour personnel navigant ou professionnel en déplacement. Gardiennage de nuit, accès direct goudronné.',
    property_type     = 'APARTMENT',
    transaction_type  = 'RENT',
    price             = 150000.00,
    condition         = 'GOOD',
    year_built        = 2011,
    surface_total     = 80.00,
    surface_living    = 70.00,
    rooms             = 3,
    bedrooms          = 2,
    bathrooms         = 1,
    balconies         = 1,
    city              = 'ABIDJAN',
    district          = 'Port-Bouët',
    street            = 'Rue des Navigateurs',
    address           = 'Résidence Naviga, Appt 1A, Port-Bouët, Abidjan',
    latitude          = 5.26800000,
    longitude         = -3.92300000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = 'b31a807b-0a35-4453-a76f-245b644468c6';


-- ── ROW 20 | bce6e652 | PUBLISHED | SHORT_STAY | HOTEL_SUITE | DAKAR ───────
-- Correction : HOUSE RENT → HOTEL_SUITE SHORT_STAY WEEKLY
UPDATE administrative.properties SET
    title             = 'Suite Junior vue Atlantique – Terrou-Bi Resort, Dakar',
    description       = 'Suite junior luxueuse avec terrasse panoramique sur l''Atlantique au Terrou-Bi Resort 5 étoiles. Service en chambre 24h/24, accès piscine à débordement et restaurant gastronomique inclus.',
    property_type     = 'HOTEL_SUITE',
    transaction_type  = 'SHORT_STAY',
    price             = 500000.00,
    condition         = 'NEW',
    year_built        = 2015,
    surface_total     = 65.00,
    surface_living    = 55.00,
    rooms             = 2,
    bedrooms          = 1,
    bathrooms         = 1,
    balconies         = 1,
    city              = 'DAKAR',
    district          = 'Corniche Ouest',
    street            = 'Boulevard de la Corniche',
    address           = 'Terrou-Bi Resort, Corniche Ouest, Dakar',
    latitude          = 14.68530000,
    longitude         = -17.46180000,
    bed_type          = 'KING',
    max_occupancy     = 2,
    meal_plan         = 'BREAKFAST',
    payment_frequency = 'WEEKLY'
WHERE id = 'bce6e652-1e8e-47e2-a3e4-c93649362d75';


-- ── ROW 21 | cc0bf184 | ARCHIVED | SALE | APARTMENT | ABIDJAN Riviera 3 ───
-- Statut ARCHIVED conservé — annonce vendue, référence historique
UPDATE administrative.properties SET
    title             = 'Appartement 4 pièces – Riviera 3, Abidjan (archivé)',
    description       = 'Appartement vendu, annonce conservée pour historique. Résidence sécurisée à Riviera 3, 280 m², parking couvert. Transaction finalisée en 2026.',
    property_type     = 'APARTMENT',
    transaction_type  = 'SALE',
    price             = 48000000.00,
    condition         = 'GOOD',
    year_built        = 2014,
    surface_total     = 280.00,
    surface_living    = 210.00,
    rooms             = 6,
    bedrooms          = 4,
    bathrooms         = 2,
    balconies         = 0,
    city              = 'ABIDJAN',
    district          = 'Riviera 3',
    street            = 'Boulevard Latrille',
    address           = 'Résidence Latrille Garden, Appt 5A, Riviera 3, Abidjan',
    latitude          = 5.35680000,
    longitude         = -3.96690000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = 'cc0bf184-e703-454c-a9c8-682514b6f9d8';


-- ── ROW 22 | d6b2b685 | PENDING | RENT_FURNISHED | APARTMENT | DAKAR Mermoz
UPDATE administrative.properties SET
    title             = 'Appartement meublé 4 pièces – Mermoz, Dakar',
    description       = 'Bel appartement meublé au 3ème étage d''une résidence sécurisée à Mermoz. Lumineux avec balcon et vue dégagée. Cuisine équipée, salon climatisé. Idéal pour famille ou expatrié.',
    property_type     = 'APARTMENT',
    transaction_type  = 'RENT_FURNISHED',
    price             = 350000.00,
    condition         = 'GOOD',
    year_built        = 2019,
    surface_total     = 95.00,
    surface_living    = 85.00,
    rooms             = 4,
    bedrooms          = 3,
    bathrooms         = 2,
    balconies         = 1,
    city              = 'DAKAR',
    district          = 'Mermoz',
    street            = 'Rue 10',
    address           = 'Résidence Les Jacarandas, Appt 3A, Mermoz, Dakar',
    latitude          = 14.72850000,
    longitude         = -17.49570000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = 'd6b2b685-7c53-492b-8ea2-81fb34a85d1c';


-- ── ROW 23 | dc75496c | PUBLISHED | RENT | APARTMENT | ABIDJAN Adjamé ─────
UPDATE administrative.properties SET
    title             = 'Appartement 2 pièces abordable – Adjamé, Abidjan',
    description       = 'Appartement de 2 pièces dans quartier central et accessible d''Adjamé. Proche gras-market, gares et commerces de proximité. Bon état général, eau et électricité fonctionnelles.',
    property_type     = 'APARTMENT',
    transaction_type  = 'RENT',
    price             = 100000.00,
    condition         = 'GOOD',
    year_built        = 2005,
    surface_total     = 55.00,
    surface_living    = 48.00,
    rooms             = 2,
    bedrooms          = 1,
    bathrooms         = 1,
    balconies         = 0,
    city              = 'ABIDJAN',
    district          = 'Adjamé',
    street            = 'Boulevard de la République',
    address           = 'Immeuble République, Appt 3, Adjamé, Abidjan',
    latitude          = 5.35200000,
    longitude         = -4.01100000,
    bed_type          = NULL,
    max_occupancy     = NULL,
    meal_plan         = NULL,
    payment_frequency = NULL,
    hotel_id          = NULL
WHERE id = 'dc75496c-1f16-485d-a492-d4ef5cc78cec';


-- ── ROW 24 | ea5a1e81 | PUBLISHED | SHORT_STAY | HOTEL_STUDIO | DAKAR ──────
-- Correction : APARTMENT RENT_FURNISHED → HOTEL_STUDIO SHORT_STAY NIGHTLY
-- Correction : coordonnées Tchad (12, 13) → Almadies Dakar
UPDATE administrative.properties SET
    title             = 'Studio hôtelier climatisé – Almadies, Dakar',
    description       = 'Studio moderne avec kitchenette équipée dans résidence hôtelière des Almadies. Terrasse commune avec vue Atlantique. Parking gratuit. Idéal pour voyageur solo ou couple en court séjour.',
    property_type     = 'HOTEL_STUDIO',
    transaction_type  = 'SHORT_STAY',
    price             = 25000.00,
    condition         = 'GOOD',
    year_built        = 2018,
    surface_total     = 28.00,
    surface_living    = 25.00,
    rooms             = 1,
    bedrooms          = 1,
    bathrooms         = 1,
    balconies         = 0,
    city              = 'DAKAR',
    district          = 'Almadies',
    street            = 'Route des Almadies',
    address           = 'Résidence Atlantique, Studio 5, Route des Almadies, Dakar',
    latitude          = 14.74970000,
    longitude         = -17.51840000,
    bed_type          = 'SINGLE',
    max_occupancy     = 2,
    meal_plan         = 'ROOM_ONLY',
    payment_frequency = 'NIGHTLY'
WHERE id = 'ea5a1e81-106b-4fd4-a911-54facb22f1a6';
