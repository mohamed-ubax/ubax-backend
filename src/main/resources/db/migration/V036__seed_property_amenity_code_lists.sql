-- ============================================================
-- V036 : Seed liste de référence PROPERTY_AMENITY
-- Commodités standard proposées à la création d'un bien
-- ============================================================

INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES
  ('5f3dde35-11c7-4fab-a294-c78648ae610b', 'PROPERTY_AMENITY', 'POOL',       'Piscine (privée ou commune)',                     TRUE),
  ('46a96105-9202-4086-80d7-ec382d6210bf', 'PROPERTY_AMENITY', 'GENERATOR',  'Groupe électrogène',                              TRUE),
  ('bd2db069-7edc-4a99-bd3d-1698927686d8', 'PROPERTY_AMENITY', 'WATER_TANK', 'Réservoir d''eau autonome (château d''eau, citerne)', TRUE),
  ('c90b8e9d-0c40-40d4-aefa-0ffb7e8cc4f6', 'PROPERTY_AMENITY', 'AC',         'Climatisation',                                   TRUE),
  ('9a2c9030-a5f8-4d5f-a1f4-6717ad368f2f', 'PROPERTY_AMENITY', 'SECURITY',   'Gardiennage / sécurité sur site',                 TRUE),
  ('2d4c5539-6602-4407-82b9-7a254ae57fdd', 'PROPERTY_AMENITY', 'PARKING',    'Parking privatif ou en commun',                   TRUE),
  ('1b8eaa70-ca6a-451e-bded-bd85e171cde9', 'PROPERTY_AMENITY', 'ELEVATOR',   'Ascenseur dans l''immeuble',                      TRUE),
  ('f202037e-b12b-41b6-becc-9d5d54d401a2', 'PROPERTY_AMENITY', 'GARDEN',     'Jardin ou espace vert privatif',                  TRUE),
  ('bef6b48e-4a15-4911-bbe1-2a7fe006f1e0', 'PROPERTY_AMENITY', 'FURNISHED',  'Bien livré meublé',                               TRUE),
  ('bf46ecac-884d-42d7-98e0-be2a67c886c7', 'PROPERTY_AMENITY', 'PETS',       'Animaux de compagnie acceptés',                   TRUE);
