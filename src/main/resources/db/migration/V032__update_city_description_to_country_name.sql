-- V032 : Remplace les codes ISO dans la description des villes (CITY)
--        par le nom complet du pays pour un affichage lisible côté front.

UPDATE administrative.la_code_list SET description = 'Côte d''Ivoire' WHERE type = 'CITY' AND description = 'CI';
UPDATE administrative.la_code_list SET description = 'Sénégal'        WHERE type = 'CITY' AND description = 'SN';
UPDATE administrative.la_code_list SET description = 'Mali'           WHERE type = 'CITY' AND description = 'ML';
UPDATE administrative.la_code_list SET description = 'Burkina Faso'   WHERE type = 'CITY' AND description = 'BF';
UPDATE administrative.la_code_list SET description = 'Guinée'         WHERE type = 'CITY' AND description = 'GN';
UPDATE administrative.la_code_list SET description = 'Guinée-Bissau'  WHERE type = 'CITY' AND description = 'GW';
UPDATE administrative.la_code_list SET description = 'Niger'          WHERE type = 'CITY' AND description = 'NE';
UPDATE administrative.la_code_list SET description = 'Togo'           WHERE type = 'CITY' AND description = 'TG';
UPDATE administrative.la_code_list SET description = 'Bénin'          WHERE type = 'CITY' AND description = 'BJ';
UPDATE administrative.la_code_list SET description = 'Nigeria'        WHERE type = 'CITY' AND description = 'NG';
UPDATE administrative.la_code_list SET description = 'Ghana'          WHERE type = 'CITY' AND description = 'GH';
UPDATE administrative.la_code_list SET description = 'Cameroun'       WHERE type = 'CITY' AND description = 'CM';
UPDATE administrative.la_code_list SET description = 'Maroc'          WHERE type = 'CITY' AND description = 'MA';
