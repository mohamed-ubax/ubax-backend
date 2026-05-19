-- ============================================================
-- V053 : Mise à jour des descriptions PROPERTY_AMENITY
-- Descriptions détaillées en français pour l'affichage mobile
-- ============================================================

UPDATE administrative.la_code_list
  SET description = 'Piscine disponible (usage privatif ou commun à la résidence)'
  WHERE type = 'PROPERTY_AMENITY' AND value = 'POOL';

UPDATE administrative.la_code_list
  SET description = 'Groupe électrogène assurant la continuité d''alimentation en cas de coupure de courant'
  WHERE type = 'PROPERTY_AMENITY' AND value = 'GENERATOR';

UPDATE administrative.la_code_list
  SET description = 'Château d''eau ou citerne garantissant l''autonomie en eau potable'
  WHERE type = 'PROPERTY_AMENITY' AND value = 'WATER_TANK';

UPDATE administrative.la_code_list
  SET description = 'Système de climatisation installé dans les pièces principales du bien'
  WHERE type = 'PROPERTY_AMENITY' AND value = 'AC';

UPDATE administrative.la_code_list
  SET description = 'Service de gardiennage ou dispositif de sécurité permanent sur le site'
  WHERE type = 'PROPERTY_AMENITY' AND value = 'SECURITY';

UPDATE administrative.la_code_list
  SET description = 'Emplacement de stationnement réservé (privatif ou en accès commun à la résidence)'
  WHERE type = 'PROPERTY_AMENITY' AND value = 'PARKING';

UPDATE administrative.la_code_list
  SET description = 'Ascenseur en état de fonctionnement dans l''immeuble'
  WHERE type = 'PROPERTY_AMENITY' AND value = 'ELEVATOR';

UPDATE administrative.la_code_list
  SET description = 'Espace vert ou jardin privatif attenant au bien'
  WHERE type = 'PROPERTY_AMENITY' AND value = 'GARDEN';

UPDATE administrative.la_code_list
  SET description = 'Bien livré avec mobilier et équipements de base inclus'
  WHERE type = 'PROPERTY_AMENITY' AND value = 'FURNISHED';

UPDATE administrative.la_code_list
  SET description = 'Propriétaire acceptant la présence d''animaux de compagnie'
  WHERE type = 'PROPERTY_AMENITY' AND value = 'PETS';
