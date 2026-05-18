-- V052 : Suppression de la contrainte CHECK statique sur properties.transaction_type
-- La validation des valeurs autorisées est gérée par la_code_list (type = 'TRANSACTION_TYPE')
-- et par @Pattern dans PropertyCreateRequest / PropertyUpdateRequest.
-- La contrainte DB bloquait SHORT_STAY ajouté en V034 (types hôteliers).
ALTER TABLE administrative.properties
  DROP CONSTRAINT IF EXISTS properties_transaction_type_check;
