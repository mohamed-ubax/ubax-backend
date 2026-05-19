-- ============================================================
-- UBAX Platform – Alimentation de la description
--        sur administrative.property_amenities
-- Table   : administrative.property_amenities
-- Date    : 2026-05-19
-- Auteur  : Dev UBAX
-- ============================================================
-- SOURCE DES DESCRIPTIONS :
--   Commodités standard → V036__seed_property_amenity_code_lists.sql
--   Commodités perso    → custom_description de la ligne elle-même
--
-- CODES TRAITÉS (130 enregistrements standard + 2 personnalisés) :
--   POOL (4) · GENERATOR (7) · WATER_TANK (20) · AC (24)
--   SECURITY (23) · PARKING (23) · ELEVATOR (3) · GARDEN (19)
--   FURNISHED (5) · PETS (2) · Personnalisées (2)
-- ============================================================


-- ══════════════════════════════════════════════════════════════
-- POOL – 4 enregistrements
-- Description : 'Piscine (privée ou commune)'
-- ══════════════════════════════════════════════════════════════

UPDATE administrative.property_amenities SET description = 'Piscine (privée ou commune)' WHERE id = '357bb737-8e00-4bbc-adb0-29f958b0bddf';
UPDATE administrative.property_amenities SET description = 'Piscine (privée ou commune)' WHERE id = '36a63109-c5ee-4979-b957-74535e3b9da7';
UPDATE administrative.property_amenities SET description = 'Piscine (privée ou commune)' WHERE id = 'ca16f9f0-4ba8-4706-8bea-f51390ec0adc';
UPDATE administrative.property_amenities SET description = 'Piscine (privée ou commune)' WHERE id = 'd0bff535-8b38-44f7-a3d8-14c62932d10c';


-- ══════════════════════════════════════════════════════════════
-- GENERATOR – 7 enregistrements
-- Description : 'Groupe électrogène'
-- ══════════════════════════════════════════════════════════════

UPDATE administrative.property_amenities SET description = 'Groupe électrogène' WHERE id = '2fac5b51-427d-4f22-a3e3-290cf1596ea1';
UPDATE administrative.property_amenities SET description = 'Groupe électrogène' WHERE id = '5c13522b-0d27-41fa-9dda-0e7e0a951db2';
UPDATE administrative.property_amenities SET description = 'Groupe électrogène' WHERE id = '644b345d-0fe9-451b-aa95-c6eb57a0e1f6';
UPDATE administrative.property_amenities SET description = 'Groupe électrogène' WHERE id = '8dfd5c96-d76a-46b6-90af-0dfaeebdd55a';
UPDATE administrative.property_amenities SET description = 'Groupe électrogène' WHERE id = '9b868405-49f2-4891-bf54-966fa135053c';
UPDATE administrative.property_amenities SET description = 'Groupe électrogène' WHERE id = 'be201a85-06f4-4b5a-b753-d77348002d44';
UPDATE administrative.property_amenities SET description = 'Groupe électrogène' WHERE id = 'f1c6f5c0-4778-47ab-9490-15354ec21d7e';


-- ══════════════════════════════════════════════════════════════
-- WATER_TANK – 20 enregistrements
-- Description : 'Réservoir d''eau autonome (château d''eau, citerne)'
-- ══════════════════════════════════════════════════════════════

UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = '1230af47-6260-44f8-8b9b-0be0b0ebc492';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = '129b2bc1-a219-4bb1-a2e8-95b1ef302f63';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = '388042f3-32df-49bd-9425-3dcf54b61108';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = '4a73f249-8636-4b71-bf02-19a7a0d1abd9';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = '4f5dae68-8eb0-475f-a65b-30876ce7d280';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = '54d1e905-98ad-4705-8db6-ab9e1875930a';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = '669e1a08-2232-4943-a65d-5b40aa53f44e';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = '6a90764c-ff75-438a-85e1-9c36ce00f06d';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = '7299afa7-125b-45a1-9112-459c1f06cd90';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = '8a582f3a-8099-47ac-95e3-a268ca098e1b';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = '8e5db3b6-3282-481b-b569-bc67f0c2622c';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = '920bf456-f498-4d63-b1ec-4e07c8793ecd';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = 'ae97ceb1-0a08-4866-b1af-95ab878b52a5';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = 'b20b09ff-0ee9-4b4f-be7f-acd0c544d315';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = 'b815e88f-320e-4b26-a594-4970d3d266d4';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = 'd47290b3-8f33-4a0a-a281-a7b4a79566fc';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = 'd9af0678-de3a-45d0-88c8-4bb6b176ed6d';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = 'e552518e-bef7-444c-aff3-91d76f5db21b';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = 'ed92a6b2-367a-466c-8fd4-699be9b74201';
UPDATE administrative.property_amenities SET description = 'Réservoir d''eau autonome (château d''eau, citerne)' WHERE id = 'eef79869-271c-431e-8b5a-1906be9199d3';


-- ══════════════════════════════════════════════════════════════
-- AC – 24 enregistrements
-- Description : 'Climatisation'
-- ══════════════════════════════════════════════════════════════

UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = '02292c72-8b91-4d3d-a372-3fc5e4be44c1';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = '0d3a2be0-8acd-4183-be54-9cdb303d1ee7';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = '19925596-badb-4bad-a6cc-8a5bb3e3653b';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = '2549facd-0dca-41c1-848a-6d476132ede7';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = '267fa944-fe8e-4ee7-a037-0b609306d9cf';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = '5761a07e-8923-4aeb-9288-d120f1557c24';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = '6235d76e-8e73-449a-8143-4210d57d7746';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = '6bad1325-73d2-4b5d-a097-fa21f03bdb50';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = '6d9b08b1-325f-429f-af61-6eb480006a19';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = '71c36eca-1f72-43d7-8c0b-24b4b969a328';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = '7e78058d-9fec-4338-b762-4108603cdda0';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = '965048ca-4d93-4ce4-ba87-88f752e0898d';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = '9c27d9f7-1071-4618-bc77-a41572563dc1';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = 'a35d6f3e-f924-4fe1-8e80-bbca105226b8';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = 'b3629c8b-141c-4333-ad42-98c9ef8a5cad';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = 'bcb3bfbb-a88d-490e-b5aa-b43556af573b';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = 'd230eb12-ade8-4767-b34a-f86d962367fa';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = 'd662e941-24e1-4fb1-8b13-3ff5a82a05bb';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = 'e22d93a3-7a26-44e3-8a53-ba449d067d4d';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = 'e338964e-fc67-4dab-8050-b26bccdfb7f4';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = 'f7afd526-41d4-42d6-a51e-ed41158a20f7';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = 'f856164e-3f8c-4565-834d-138509eeb272';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = 'f978fbad-4628-4aef-9ec8-5069c692c576';
UPDATE administrative.property_amenities SET description = 'Climatisation' WHERE id = 'f9f92e28-db5f-462d-bfdc-6aae2addd627';


-- ══════════════════════════════════════════════════════════════
-- SECURITY – 23 enregistrements
-- Description : 'Gardiennage / sécurité sur site'
-- ══════════════════════════════════════════════════════════════

UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '01ff5992-8ef6-4a98-b7ec-5909f3073f38';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '02403db3-6ea8-439d-b96f-1492a5402957';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '1a788d53-dad8-4b11-bbc8-2f9613c2e77e';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '1de28014-336a-4b09-93e4-4b625aaf13aa';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '303f20e5-0fc3-4756-b784-cb59b8cfc998';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '372eebc3-b475-43a6-b3e7-8f9970499b29';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '3ef12b2e-7804-49b1-ae7b-f1ab0b957841';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '444b3afc-13c2-420e-966c-a5e3f98f23a4';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '4c441bc7-2679-4485-87a8-f812ba7f4f1f';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '4eaa60f0-ea3a-477a-a5e8-144479ef8298';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '5c4ab449-d0c0-4992-8762-533bce0549f8';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '872c2f67-7518-4fca-8994-0ba779ed5ce2';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '8bc371c6-c482-482e-a6bb-1d39ebfa3668';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '9657fa4b-e7f7-4896-9b6e-8e6ded63045d';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = '9bb8fccc-3ebd-41ad-b50f-2cec3c9fdbbc';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = 'aca3bc35-c352-4d15-ace0-9978555855f2';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = 'c5b09117-e898-433d-93e5-0cc777aadf4e';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = 'c9b3e946-5770-4bd0-bf67-42c7575e3d38';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = 'e18ae32b-9c7c-4dcf-86f7-1df606b25c38';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = 'e2fe58bc-c27a-4f49-9ff9-6494bdac0eac';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = 'e74f73ea-28cf-4bab-88b5-3f7295944b4c';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = 'fc274094-f9df-4b2a-8e4f-cc518af8d08b';
UPDATE administrative.property_amenities SET description = 'Gardiennage / sécurité sur site' WHERE id = 'fc78f9c1-ff73-4c66-ad9b-b5819936d0df';


-- ══════════════════════════════════════════════════════════════
-- PARKING – 23 enregistrements
-- Description : 'Parking privatif ou en commun'
-- ══════════════════════════════════════════════════════════════

UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = '0501a1d9-baa8-4521-b54b-dc331d1d0652';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = '07934d74-5e10-4164-9708-1db334b513c8';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = '15b10c0a-4a52-4a20-abea-7adf72f8b4bc';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = '1ca6159b-f72a-4593-9722-82dfb4d1911c';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = '344bd0ef-9ca4-45e3-900e-28d674932125';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = '47a0afe8-9cb8-497c-a742-f07acfb6d87f';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = '556b535b-a54e-4049-8866-743637fcfcf7';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = '5c624b34-5d73-486b-91b9-9bbaaeecb6e6';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = '5f3b231c-64ac-4a49-9152-8dab3627baeb';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = '610ff392-f164-487e-9bd8-0be82cae134e';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = '662133f6-349a-4997-8dad-60e81b24e961';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = '6a20d789-6916-4509-b51c-1eee627085f4';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = '7b4e21fb-3777-46c4-adc1-9409263b33a3';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = '7b5c5775-3c4a-48c6-a8b5-b26e363bde88';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = 'b83a0535-ae5a-4b49-ad05-5620db87df24';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = 'bc29d45d-36a1-4cc9-b1b7-022486f7e6c2';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = 'e1bc9cd3-0822-4d8c-991f-153e120984c5';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = 'e5a0bd36-54b9-4477-ba88-bcb0c8ed51e4';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = 'ee80d4dc-cdde-42cb-bdb6-28f80c575156';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = 'fa1770df-ef3d-42fd-934c-bd102fcc58d3';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = 'fe4264ce-9d1e-478d-adc7-7e4e174d9f58';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = 'fe956220-3767-492a-a1be-94091ecefe4d';
UPDATE administrative.property_amenities SET description = 'Parking privatif ou en commun' WHERE id = 'ff73766b-4046-4587-9d4f-bd04fe991bc2';


-- ══════════════════════════════════════════════════════════════
-- ELEVATOR – 3 enregistrements
-- Description : 'Ascenseur dans l''immeuble'
-- ══════════════════════════════════════════════════════════════

UPDATE administrative.property_amenities SET description = 'Ascenseur dans l''immeuble' WHERE id = '168264ae-06a2-46f1-abcc-e4226da0119d';
UPDATE administrative.property_amenities SET description = 'Ascenseur dans l''immeuble' WHERE id = '49429945-95b9-4602-847c-097ddd48bd32';
UPDATE administrative.property_amenities SET description = 'Ascenseur dans l''immeuble' WHERE id = '54f86a0d-bbf0-46f9-bf12-e6fa9a0f18bf';


-- ══════════════════════════════════════════════════════════════
-- GARDEN – 19 enregistrements
-- Description : 'Jardin ou espace vert privatif'
-- ══════════════════════════════════════════════════════════════

UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = '015eb59a-2c3d-4349-8155-a2805343f1b3';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = '018b0811-3c42-4d26-889c-8f26a6958e93';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = '10dbb2fe-2f8d-4493-8507-d2e27885e1bd';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = '159c1241-4f97-4999-bd8d-c53baf9cddf1';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = '30ca9021-50c7-4209-a767-e4f0b50a5b88';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = '3906a995-c339-4f97-a502-589195ba4767';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = '5a33022b-d719-4523-b68a-1c44b74c3ab2';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = '7668b9ea-c744-44b2-bdb3-c88a2e52c2a0';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = '8237926b-bf5b-46a5-b56c-8981d8053a14';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = '84b9745e-ea65-48e7-b388-eebd3c7e2aa4';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = '91c8df8e-13ce-4da1-a230-26fb7792d155';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = '932938a2-cdc4-4e1b-b100-31100f8b9852';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = 'b4012215-46c1-4398-9040-394e7723918b';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = 'b6a13210-a136-4159-ab58-a699fbf1de7d';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = 'c7027a8a-fd95-460b-906a-cbff54cd05a7';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = 'c74381cc-8cc4-40a9-8054-15de23cbd276';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = 'cc1a8fbe-5ae1-4c26-8ad2-0367c57171b7';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = 'db0d17c9-19ee-4e7a-8562-0e2d1f4bac55';
UPDATE administrative.property_amenities SET description = 'Jardin ou espace vert privatif' WHERE id = 'dc38e85c-bc43-423d-b426-1efccfcc8dd4';


-- ══════════════════════════════════════════════════════════════
-- FURNISHED – 5 enregistrements
-- Description : 'Bien livré meublé'
-- ══════════════════════════════════════════════════════════════

UPDATE administrative.property_amenities SET description = 'Bien livré meublé' WHERE id = '0da69d51-2022-44e9-8e8e-7d4381471857';
UPDATE administrative.property_amenities SET description = 'Bien livré meublé' WHERE id = '19f6d5fa-57ba-409b-a21f-e8dd79421dc8';
UPDATE administrative.property_amenities SET description = 'Bien livré meublé' WHERE id = '5820d000-9b2c-4eb4-89fe-f0dba332d435';
UPDATE administrative.property_amenities SET description = 'Bien livré meublé' WHERE id = '61810154-7e9c-42fb-91f3-d125dbe38f80';
UPDATE administrative.property_amenities SET description = 'Bien livré meublé' WHERE id = 'a7680db9-39df-46a5-b78e-8ec0f7e5074d';


-- ══════════════════════════════════════════════════════════════
-- PETS – 2 enregistrements
-- Description : 'Animaux de compagnie acceptés'
-- ══════════════════════════════════════════════════════════════

UPDATE administrative.property_amenities SET description = 'Animaux de compagnie acceptés' WHERE id = '008f79ee-7991-4471-a11f-7d7c3e76817e';
UPDATE administrative.property_amenities SET description = 'Animaux de compagnie acceptés' WHERE id = '78cdb4dc-68c2-4953-9c29-5132a8515f85';


-- ══════════════════════════════════════════════════════════════
-- PERSONNALISÉES – 2 enregistrements
-- Description ← custom_description de la ligne
-- ══════════════════════════════════════════════════════════════

-- ── Domotique ────────────────────────────────────────────────
UPDATE administrative.property_amenities SET description = 'Système domotique intégré (éclairage, volets, alarme)' WHERE id = '3e9b0372-eab2-4aeb-94a4-9e570606f8e7';

-- ── Connexion fibre optique ───────────────────────────────────
UPDATE administrative.property_amenities SET description = 'Internet fibre 100 Mbps inclus dans le loyer' WHERE id = 'eb4c0e30-9f59-4075-b026-85fd2c246639';


-- ══════════════════════════════════════════════════════════════
-- VÉRIFICATION RAPIDE (optionnel — à exécuter séparément)
-- ══════════════════════════════════════════════════════════════
-- SELECT id, code, custom_value, description
-- FROM administrative.property_amenities
-- WHERE description IS NOT NULL
-- ORDER BY code NULLS LAST, custom_value;
