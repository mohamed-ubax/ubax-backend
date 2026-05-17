-- ============================================================
-- V051 — Location-vente (RENT_TO_OWN)
--   1. Nouveau type de contrat dans la_code_list
--   2. Colonne monthly_installment sur contracts
--      (mensualité versée vers l'acquisition du bien)
-- ============================================================

-- ── Code list ────────────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign)
VALUES ('7ed44bf8-3850-4482-afdc-2446ae044b21', 'CONTRACT_TYPE', 'RENT_TO_OWN', 'Location-vente', TRUE);

-- ── Colonne mensualité location-vente ────────────────────────
-- sale_price  → prix total du bien   (déjà présent)
-- end_date    → fin du programme     (déjà présent)
-- monthly_installment → mensualité versée vers l'achat (nouveau)
ALTER TABLE administrative.contracts
    ADD COLUMN monthly_installment NUMERIC(15, 2);
