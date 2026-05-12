-- V042 : Suppression de la contrainte CHECK statique sur properties.property_type
-- La validation des valeurs autorisées est gérée par la_code_list (type = 'PROPERTY_TYPE'),
-- la contrainte DB empêchait l'usage des types hôteliers ajoutés en V034 (HOTEL_SUITE, etc.).
ALTER TABLE administrative.properties
  DROP CONSTRAINT IF EXISTS properties_property_type_check;
