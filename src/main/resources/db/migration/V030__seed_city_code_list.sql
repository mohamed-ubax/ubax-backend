-- ============================================================
-- V030 – Seed : liste des villes (CITY)
-- description = code ISO du pays (pour filtrage par pays)
-- Accessible via GET /v1/code-list/type/CITY
-- ============================================================

INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign, created_at, updated_at)
VALUES
    -- Côte d'Ivoire (CI)
    ('b2e4f1a3-2222-4d3b-9f02-000000000001', 'CITY', 'Abidjan',          'CI', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000002', 'CITY', 'Bouaké',           'CI', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000003', 'CITY', 'Daloa',            'CI', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000004', 'CITY', 'Yamoussoukro',     'CI', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000005', 'CITY', 'San Pedro',        'CI', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000006', 'CITY', 'Korhogo',          'CI', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000007', 'CITY', 'Man',              'CI', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000008', 'CITY', 'Gagnoa',           'CI', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000009', 'CITY', 'Divo',             'CI', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000010', 'CITY', 'Abengourou',       'CI', TRUE, now(), now()),

    -- Sénégal (SN)
    ('b2e4f1a3-2222-4d3b-9f02-000000000011', 'CITY', 'Dakar',            'SN', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000012', 'CITY', 'Touba',            'SN', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000013', 'CITY', 'Thiès',            'SN', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000014', 'CITY', 'Ziguinchor',       'SN', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000015', 'CITY', 'Kaolack',          'SN', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000016', 'CITY', 'Saint-Louis',      'SN', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000017', 'CITY', 'Mbour',            'SN', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000018', 'CITY', 'Tambacounda',      'SN', TRUE, now(), now()),

    -- Mali (ML)
    ('b2e4f1a3-2222-4d3b-9f02-000000000019', 'CITY', 'Bamako',           'ML', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000020', 'CITY', 'Sikasso',          'ML', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000021', 'CITY', 'Mopti',            'ML', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000022', 'CITY', 'Kayes',            'ML', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000023', 'CITY', 'Ségou',            'ML', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000024', 'CITY', 'Koutiala',         'ML', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000025', 'CITY', 'Gao',              'ML', TRUE, now(), now()),

    -- Burkina Faso (BF)
    ('b2e4f1a3-2222-4d3b-9f02-000000000026', 'CITY', 'Ouagadougou',      'BF', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000027', 'CITY', 'Bobo-Dioulasso',   'BF', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000028', 'CITY', 'Koudougou',        'BF', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000029', 'CITY', 'Banfora',          'BF', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000030', 'CITY', 'Ouahigouya',       'BF', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000031', 'CITY', 'Dédougou',         'BF', TRUE, now(), now()),

    -- Guinée (GN)
    ('b2e4f1a3-2222-4d3b-9f02-000000000032', 'CITY', 'Conakry',          'GN', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000033', 'CITY', 'Nzérékoré',        'GN', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000034', 'CITY', 'Kindia',           'GN', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000035', 'CITY', 'Kankan',           'GN', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000036', 'CITY', 'Labé',             'GN', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000037', 'CITY', 'Boké',             'GN', TRUE, now(), now()),

    -- Guinée-Bissau (GW)
    ('b2e4f1a3-2222-4d3b-9f02-000000000038', 'CITY', 'Bissau',           'GW', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000039', 'CITY', 'Bafatá',           'GW', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000040', 'CITY', 'Gabú',             'GW', TRUE, now(), now()),

    -- Niger (NE)
    ('b2e4f1a3-2222-4d3b-9f02-000000000041', 'CITY', 'Niamey',           'NE', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000042', 'CITY', 'Zinder',           'NE', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000043', 'CITY', 'Maradi',           'NE', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000044', 'CITY', 'Agadez',           'NE', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000045', 'CITY', 'Tahoua',           'NE', TRUE, now(), now()),

    -- Togo (TG)
    ('b2e4f1a3-2222-4d3b-9f02-000000000046', 'CITY', 'Lomé',             'TG', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000047', 'CITY', 'Sokodé',           'TG', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000048', 'CITY', 'Kara',             'TG', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000049', 'CITY', 'Atakpamé',         'TG', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000050', 'CITY', 'Kpalimé',          'TG', TRUE, now(), now()),

    -- Bénin (BJ)
    ('b2e4f1a3-2222-4d3b-9f02-000000000051', 'CITY', 'Cotonou',          'BJ', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000052', 'CITY', 'Porto-Novo',       'BJ', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000053', 'CITY', 'Parakou',          'BJ', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000054', 'CITY', 'Djougou',          'BJ', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000055', 'CITY', 'Bohicon',          'BJ', TRUE, now(), now()),

    -- Ghana (GH)
    ('b2e4f1a3-2222-4d3b-9f02-000000000056', 'CITY', 'Accra',            'GH', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000057', 'CITY', 'Kumasi',           'GH', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000058', 'CITY', 'Tamale',           'GH', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000059', 'CITY', 'Sekondi-Takoradi', 'GH', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000060', 'CITY', 'Cape Coast',       'GH', TRUE, now(), now()),

    -- Nigeria (NG)
    ('b2e4f1a3-2222-4d3b-9f02-000000000061', 'CITY', 'Lagos',            'NG', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000062', 'CITY', 'Abuja',            'NG', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000063', 'CITY', 'Kano',             'NG', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000064', 'CITY', 'Ibadan',           'NG', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000065', 'CITY', 'Port Harcourt',    'NG', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000066', 'CITY', 'Benin City',       'NG', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000067', 'CITY', 'Enugu',            'NG', TRUE, now(), now()),

    -- Cameroun (CM)
    ('b2e4f1a3-2222-4d3b-9f02-000000000068', 'CITY', 'Douala',           'CM', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000069', 'CITY', 'Yaoundé',          'CM', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000070', 'CITY', 'Bafoussam',        'CM', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000071', 'CITY', 'Garoua',           'CM', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000072', 'CITY', 'Maroua',           'CM', TRUE, now(), now()),

    -- Maroc (MA)
    ('b2e4f1a3-2222-4d3b-9f02-000000000073', 'CITY', 'Casablanca',       'MA', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000074', 'CITY', 'Rabat',            'MA', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000075', 'CITY', 'Fès',              'MA', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000076', 'CITY', 'Marrakech',        'MA', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000077', 'CITY', 'Agadir',           'MA', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000078', 'CITY', 'Tanger',           'MA', TRUE, now(), now()),
    ('b2e4f1a3-2222-4d3b-9f02-000000000079', 'CITY', 'Meknès',           'MA', TRUE, now(), now());
