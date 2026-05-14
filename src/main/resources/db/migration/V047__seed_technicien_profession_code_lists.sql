-- ============================================================
-- V047 : Seed liste de référence TECHNICIEN_PROFESSION
-- Métiers des prestataires SAV (agences et hôtels)
-- ============================================================

INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES
  ('77229fc0-72b1-47c2-b397-6e13c00fa467', 'TECHNICIEN_PROFESSION', 'PLOMBIER',        'Plomberie et sanitaires',                   TRUE),
  ('911e95e1-2751-4d97-9b6e-2a710d655b31', 'TECHNICIEN_PROFESSION', 'ELECTRICIEN',     'Électricité générale et domotique',         TRUE),
  ('f0a67594-e075-47b4-8b31-2278091af32e', 'TECHNICIEN_PROFESSION', 'SERRURIER',       'Serrurerie, blindage et contrôle d''accès', TRUE),
  ('375b4c2a-c88b-4ffe-a2ab-eb7f36874768', 'TECHNICIEN_PROFESSION', 'MENUISIER',       'Menuiserie bois et aluminium',              TRUE),
  ('21bd7e57-bb26-487f-aa40-e3f599711088', 'TECHNICIEN_PROFESSION', 'MACON',           'Maçonnerie, carrelage et ravalement',       TRUE),
  ('90763733-e67c-4890-b376-43dcd9a52d5b', 'TECHNICIEN_PROFESSION', 'PEINTRE',         'Peinture intérieure et extérieure',         TRUE),
  ('9841bb37-6a96-4a70-ba60-49ae68d80662', 'TECHNICIEN_PROFESSION', 'CLIMATISATION',   'Climatisation et ventilation (CVC)',        TRUE),
  ('afc8e41e-c307-496f-a848-9e8b24f4eb5f', 'TECHNICIEN_PROFESSION', 'VITRERIE',        'Vitrerie, miroiterie et double vitrage',    TRUE),
  ('73b670c8-7ecf-4067-9d84-fd47dbd3db36', 'TECHNICIEN_PROFESSION', 'JARDINAGE',       'Entretien espaces verts et jardinage',      TRUE),
  ('baa35617-6c73-425b-ba79-5dd5a300e009', 'TECHNICIEN_PROFESSION', 'NETTOYAGE',       'Nettoyage industriel et désinfection',      TRUE),
  ('781d6ef0-48da-4f51-97d0-b72bb4a3bde9', 'TECHNICIEN_PROFESSION', 'DESINSECTISATION','Désinsectisation et dératisation',          TRUE),
  ('71cafe6a-d4b8-471f-ba74-d1445e8fee5e', 'TECHNICIEN_PROFESSION', 'AUTRE',           'Autre prestation non listée',               TRUE)
ON CONFLICT (id) DO NOTHING;
