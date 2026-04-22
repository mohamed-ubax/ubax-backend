-- V021__fix_user_sub_roles_scope_constraint.sql
-- Correction de la contrainte chk_user_sub_roles_scope :
-- les valeurs sont stockées en majuscules par Hibernate (EnumType.STRING)

ALTER TABLE administrative.user_sub_roles
DROP CONSTRAINT chk_user_sub_roles_scope;

ALTER TABLE administrative.user_sub_roles
    ADD CONSTRAINT chk_user_sub_roles_scope
        CHECK (scope IN ('UBAX_INTERNAL', 'AGENCE', 'HOTEL'));