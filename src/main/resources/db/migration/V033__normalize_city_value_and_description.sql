-- V033 : Rectification des villes (CITY)
--   1. Normalise le champ value  : majuscules, sans accents, espaces/tirets → _
--   2. Reformate le champ description : "Ville de/d' {nom officiel}, {pays}"

-- ── Côte d'Ivoire ──────────────────────────────────────────────────────────
UPDATE administrative.la_code_list SET value = 'ABIDJAN',          description = 'Ville d''Abidjan, Côte d''Ivoire'        WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000001';
UPDATE administrative.la_code_list SET value = 'BOUAKE',           description = 'Ville de Bouaké, Côte d''Ivoire'          WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000002';
UPDATE administrative.la_code_list SET value = 'DALOA',            description = 'Ville de Daloa, Côte d''Ivoire'           WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000003';
UPDATE administrative.la_code_list SET value = 'YAMOUSSOUKRO',     description = 'Ville de Yamoussoukro, Côte d''Ivoire'    WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000004';
UPDATE administrative.la_code_list SET value = 'SAN_PEDRO',        description = 'Ville de San Pedro, Côte d''Ivoire'       WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000005';
UPDATE administrative.la_code_list SET value = 'KORHOGO',          description = 'Ville de Korhogo, Côte d''Ivoire'         WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000006';
UPDATE administrative.la_code_list SET value = 'MAN',              description = 'Ville de Man, Côte d''Ivoire'             WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000007';
UPDATE administrative.la_code_list SET value = 'GAGNOA',           description = 'Ville de Gagnoa, Côte d''Ivoire'          WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000008';
UPDATE administrative.la_code_list SET value = 'DIVO',             description = 'Ville de Divo, Côte d''Ivoire'            WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000009';
UPDATE administrative.la_code_list SET value = 'ABENGOUROU',       description = 'Ville d''Abengourou, Côte d''Ivoire'      WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000010';

-- ── Sénégal ────────────────────────────────────────────────────────────────
UPDATE administrative.la_code_list SET value = 'DAKAR',            description = 'Ville de Dakar, Sénégal'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000011';
UPDATE administrative.la_code_list SET value = 'TOUBA',            description = 'Ville de Touba, Sénégal'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000012';
UPDATE administrative.la_code_list SET value = 'THIES',            description = 'Ville de Thiès, Sénégal'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000013';
UPDATE administrative.la_code_list SET value = 'ZIGUINCHOR',       description = 'Ville de Ziguinchor, Sénégal'            WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000014';
UPDATE administrative.la_code_list SET value = 'KAOLACK',          description = 'Ville de Kaolack, Sénégal'               WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000015';
UPDATE administrative.la_code_list SET value = 'SAINT_LOUIS',      description = 'Ville de Saint-Louis, Sénégal'           WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000016';
UPDATE administrative.la_code_list SET value = 'MBOUR',            description = 'Ville de Mbour, Sénégal'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000017';
UPDATE administrative.la_code_list SET value = 'TAMBACOUNDA',      description = 'Ville de Tambacounda, Sénégal'           WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000018';

-- ── Mali ───────────────────────────────────────────────────────────────────
UPDATE administrative.la_code_list SET value = 'BAMAKO',           description = 'Ville de Bamako, Mali'                   WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000019';
UPDATE administrative.la_code_list SET value = 'SIKASSO',          description = 'Ville de Sikasso, Mali'                  WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000020';
UPDATE administrative.la_code_list SET value = 'MOPTI',            description = 'Ville de Mopti, Mali'                    WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000021';
UPDATE administrative.la_code_list SET value = 'KAYES',            description = 'Ville de Kayes, Mali'                    WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000022';
UPDATE administrative.la_code_list SET value = 'SEGOU',            description = 'Ville de Ségou, Mali'                    WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000023';
UPDATE administrative.la_code_list SET value = 'KOUTIALA',         description = 'Ville de Koutiala, Mali'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000024';
UPDATE administrative.la_code_list SET value = 'GAO',              description = 'Ville de Gao, Mali'                      WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000025';

-- ── Burkina Faso ───────────────────────────────────────────────────────────
UPDATE administrative.la_code_list SET value = 'OUAGADOUGOU',      description = 'Ville d''Ouagadougou, Burkina Faso'      WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000026';
UPDATE administrative.la_code_list SET value = 'BOBO_DIOULASSO',   description = 'Ville de Bobo-Dioulasso, Burkina Faso'   WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000027';
UPDATE administrative.la_code_list SET value = 'KOUDOUGOU',        description = 'Ville de Koudougou, Burkina Faso'        WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000028';
UPDATE administrative.la_code_list SET value = 'BANFORA',          description = 'Ville de Banfora, Burkina Faso'          WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000029';
UPDATE administrative.la_code_list SET value = 'OUAHIGOUYA',       description = 'Ville d''Ouahigouya, Burkina Faso'       WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000030';
UPDATE administrative.la_code_list SET value = 'DEDOUGOU',         description = 'Ville de Dédougou, Burkina Faso'         WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000031';

-- ── Guinée ─────────────────────────────────────────────────────────────────
UPDATE administrative.la_code_list SET value = 'CONAKRY',          description = 'Ville de Conakry, Guinée'                WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000032';
UPDATE administrative.la_code_list SET value = 'NZEREKORE',        description = 'Ville de Nzérékoré, Guinée'              WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000033';
UPDATE administrative.la_code_list SET value = 'KINDIA',           description = 'Ville de Kindia, Guinée'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000034';
UPDATE administrative.la_code_list SET value = 'KANKAN',           description = 'Ville de Kankan, Guinée'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000035';
UPDATE administrative.la_code_list SET value = 'LABE',             description = 'Ville de Labé, Guinée'                   WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000036';
UPDATE administrative.la_code_list SET value = 'BOKE',             description = 'Ville de Boké, Guinée'                   WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000037';

-- ── Guinée-Bissau ──────────────────────────────────────────────────────────
UPDATE administrative.la_code_list SET value = 'BISSAU',           description = 'Ville de Bissau, Guinée-Bissau'          WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000038';
UPDATE administrative.la_code_list SET value = 'BAFATA',           description = 'Ville de Bafatá, Guinée-Bissau'          WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000039';
UPDATE administrative.la_code_list SET value = 'GABU',             description = 'Ville de Gabú, Guinée-Bissau'            WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000040';

-- ── Niger ──────────────────────────────────────────────────────────────────
UPDATE administrative.la_code_list SET value = 'NIAMEY',           description = 'Ville de Niamey, Niger'                  WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000041';
UPDATE administrative.la_code_list SET value = 'ZINDER',           description = 'Ville de Zinder, Niger'                  WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000042';
UPDATE administrative.la_code_list SET value = 'MARADI',           description = 'Ville de Maradi, Niger'                  WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000043';
UPDATE administrative.la_code_list SET value = 'AGADEZ',           description = 'Ville d''Agadez, Niger'                  WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000044';
UPDATE administrative.la_code_list SET value = 'TAHOUA',           description = 'Ville de Tahoua, Niger'                  WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000045';

-- ── Togo ───────────────────────────────────────────────────────────────────
UPDATE administrative.la_code_list SET value = 'LOME',             description = 'Ville de Lomé, Togo'                     WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000046';
UPDATE administrative.la_code_list SET value = 'SOKODE',           description = 'Ville de Sokodé, Togo'                   WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000047';
UPDATE administrative.la_code_list SET value = 'KARA',             description = 'Ville de Kara, Togo'                     WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000048';
UPDATE administrative.la_code_list SET value = 'ATAKPAME',         description = 'Ville d''Atakpamé, Togo'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000049';
UPDATE administrative.la_code_list SET value = 'KPALIME',          description = 'Ville de Kpalimé, Togo'                  WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000050';

-- ── Bénin ──────────────────────────────────────────────────────────────────
UPDATE administrative.la_code_list SET value = 'COTONOU',          description = 'Ville de Cotonou, Bénin'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000051';
UPDATE administrative.la_code_list SET value = 'PORTO_NOVO',       description = 'Ville de Porto-Novo, Bénin'              WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000052';
UPDATE administrative.la_code_list SET value = 'PARAKOU',          description = 'Ville de Parakou, Bénin'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000053';
UPDATE administrative.la_code_list SET value = 'DJOUGOU',          description = 'Ville de Djougou, Bénin'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000054';
UPDATE administrative.la_code_list SET value = 'BOHICON',          description = 'Ville de Bohicon, Bénin'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000055';

-- ── Ghana ──────────────────────────────────────────────────────────────────
UPDATE administrative.la_code_list SET value = 'ACCRA',            description = 'Ville d''Accra, Ghana'                   WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000056';
UPDATE administrative.la_code_list SET value = 'KUMASI',           description = 'Ville de Kumasi, Ghana'                  WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000057';
UPDATE administrative.la_code_list SET value = 'TAMALE',           description = 'Ville de Tamale, Ghana'                  WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000058';
UPDATE administrative.la_code_list SET value = 'SEKONDI_TAKORADI', description = 'Ville de Sekondi-Takoradi, Ghana'        WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000059';
UPDATE administrative.la_code_list SET value = 'CAPE_COAST',       description = 'Ville de Cape Coast, Ghana'              WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000060';

-- ── Nigeria ────────────────────────────────────────────────────────────────
UPDATE administrative.la_code_list SET value = 'LAGOS',            description = 'Ville de Lagos, Nigeria'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000061';
UPDATE administrative.la_code_list SET value = 'ABUJA',            description = 'Ville d''Abuja, Nigeria'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000062';
UPDATE administrative.la_code_list SET value = 'KANO',             description = 'Ville de Kano, Nigeria'                  WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000063';
UPDATE administrative.la_code_list SET value = 'IBADAN',           description = 'Ville d''Ibadan, Nigeria'                WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000064';
UPDATE administrative.la_code_list SET value = 'PORT_HARCOURT',    description = 'Ville de Port Harcourt, Nigeria'         WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000065';
UPDATE administrative.la_code_list SET value = 'BENIN_CITY',       description = 'Ville de Benin City, Nigeria'            WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000066';
UPDATE administrative.la_code_list SET value = 'ENUGU',            description = 'Ville d''Enugu, Nigeria'                 WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000067';

-- ── Cameroun ───────────────────────────────────────────────────────────────
UPDATE administrative.la_code_list SET value = 'DOUALA',           description = 'Ville de Douala, Cameroun'               WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000068';
UPDATE administrative.la_code_list SET value = 'YAOUNDE',          description = 'Ville de Yaoundé, Cameroun'              WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000069';
UPDATE administrative.la_code_list SET value = 'BAFOUSSAM',        description = 'Ville de Bafoussam, Cameroun'            WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000070';
UPDATE administrative.la_code_list SET value = 'GAROUA',           description = 'Ville de Garoua, Cameroun'               WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000071';
UPDATE administrative.la_code_list SET value = 'MAROUA',           description = 'Ville de Maroua, Cameroun'               WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000072';

-- ── Maroc ──────────────────────────────────────────────────────────────────
UPDATE administrative.la_code_list SET value = 'CASABLANCA',       description = 'Ville de Casablanca, Maroc'              WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000073';
UPDATE administrative.la_code_list SET value = 'RABAT',            description = 'Ville de Rabat, Maroc'                   WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000074';
UPDATE administrative.la_code_list SET value = 'FES',              description = 'Ville de Fès, Maroc'                     WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000075';
UPDATE administrative.la_code_list SET value = 'MARRAKECH',        description = 'Ville de Marrakech, Maroc'               WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000076';
UPDATE administrative.la_code_list SET value = 'AGADIR',           description = 'Ville d''Agadir, Maroc'                  WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000077';
UPDATE administrative.la_code_list SET value = 'TANGER',           description = 'Ville de Tanger, Maroc'                  WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000078';
UPDATE administrative.la_code_list SET value = 'MEKNES',           description = 'Ville de Meknès, Maroc'                  WHERE id = 'b2e4f1a3-2222-4d3b-9f02-000000000079';
