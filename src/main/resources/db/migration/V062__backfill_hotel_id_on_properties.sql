-- V062 : Backfill hotel_id sur les propriétés hôtelières existantes
-- -----------------------------------------------------------------------
-- Contexte : la colonne hotel_id a été ajoutée en V044 mais n'était pas
-- systématiquement renseignée par les versions antérieures de
-- PropertyServiceImpl. Pour les biens dont le propriétaire (owner) est
-- rattaché à un hôtel (users.hotel_id IS NOT NULL) et dont hotel_id est
-- encore NULL, on propage le hotel_id de l'owner.
--
-- Impact : les requêtes réservation qui filtrent sur property.hotel_id
-- trouvent désormais les réservations de ces biens sans fallback.
-- -----------------------------------------------------------------------

UPDATE administrative.properties p
SET    hotel_id  = u.hotel_id,
       updated_at = NOW()
FROM   administrative.users u
WHERE  p.owner_id  = u.id
  AND  p.hotel_id  IS NULL
  AND  u.hotel_id  IS NOT NULL;
