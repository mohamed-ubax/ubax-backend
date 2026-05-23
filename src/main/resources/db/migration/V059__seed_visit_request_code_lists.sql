-- V059 : Seed code lists pour Property Visit (VisitRequestStatus)

-- Statuts des demandes de visite
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign, created_at, updated_at)
VALUES
    ('dd3f2ec6-2d48-4ed7-a07e-98fcfab3bf2a','VISIT_REQUEST_STATUS', 'PENDING', 'En attente de réponse de l''agence', true, now(), now()),
    ('60657c35-f710-4366-b6b7-40d9274ba021','VISIT_REQUEST_STATUS', 'CONFIRMED', 'Visite confirmée par l''agence', true, now(), now()),
    ('ce11c782-1a64-43f2-bd73-97f38575104c','VISIT_REQUEST_STATUS', 'REJECTED', 'Visite rejetée par l''agence', true, now(), now()),
    ('5538cfc5-23be-455c-82f7-640b0d5ce6f1','VISIT_REQUEST_STATUS', 'CANCELLED', 'Visite annulée par le client', true, now(), now()),
    ('d50f0cd0-5149-404e-9d34-7f229d58790a','VISIT_REQUEST_STATUS', 'COMPLETED', 'Visite effectuée', true, now(), now())
ON CONFLICT DO NOTHING;
