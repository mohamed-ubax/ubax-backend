-- ============================================================
-- V048 : Mise à jour TICKET_CATEGORY — alignement avec les
--        catégories métier affichées dans l'UI et les
--        professions TECHNICIEN_PROFESSION (V047)
-- Remplace les 9 catégories génériques par les 12 catégories
-- métier spécifiques aux interventions SAV.
-- ============================================================

DELETE FROM administrative.la_code_list
WHERE type = 'TICKET_CATEGORY';

INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES
  ('a1b2c3d4-0001-4000-8000-000000000001', 'TICKET_CATEGORY', 'PLOMBIER',         'Plomberie et sanitaires',                   TRUE),
  ('a1b2c3d4-0002-4000-8000-000000000002', 'TICKET_CATEGORY', 'ELECTRICIEN',      'Électricité générale et domotique',         TRUE),
  ('a1b2c3d4-0003-4000-8000-000000000003', 'TICKET_CATEGORY', 'SERRURIER',        'Serrurerie, blindage et contrôle d''accès', TRUE),
  ('a1b2c3d4-0004-4000-8000-000000000004', 'TICKET_CATEGORY', 'MENUISIER',        'Menuiserie bois et aluminium',              TRUE),
  ('a1b2c3d4-0005-4000-8000-000000000005', 'TICKET_CATEGORY', 'MACON',            'Maçonnerie, carrelage et ravalement',       TRUE),
  ('a1b2c3d4-0006-4000-8000-000000000006', 'TICKET_CATEGORY', 'PEINTRE',          'Peinture intérieure et extérieure',         TRUE),
  ('a1b2c3d4-0007-4000-8000-000000000007', 'TICKET_CATEGORY', 'CLIMATISATION',    'Climatisation et ventilation (CVC)',        TRUE),
  ('a1b2c3d4-0008-4000-8000-000000000008', 'TICKET_CATEGORY', 'VITRERIE',         'Vitrerie, miroiterie et double vitrage',    TRUE),
  ('a1b2c3d4-0009-4000-8000-000000000009', 'TICKET_CATEGORY', 'JARDINAGE',        'Entretien espaces verts et jardinage',      TRUE),
  ('a1b2c3d4-0010-4000-8000-000000000010', 'TICKET_CATEGORY', 'NETTOYAGE',        'Nettoyage industriel et désinfection',      TRUE),
  ('a1b2c3d4-0011-4000-8000-000000000011', 'TICKET_CATEGORY', 'DESINSECTISATION', 'Désinsectisation et dératisation',          TRUE),
  ('a1b2c3d4-0012-4000-8000-000000000012', 'TICKET_CATEGORY', 'AUTRE',            'Autre prestation non listée',               TRUE);
