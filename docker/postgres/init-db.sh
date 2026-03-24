#!/bin/bash
# ===================================================================
# PostgreSQL Init Script â€“ UBAX Platform
# ExÃ©cutÃ© une seule fois lors de la crÃ©ation du volume PostgreSQL
# ===================================================================
# âš ï¸  Ce fichier doit avoir des fins de ligne LF (Unix), pas CRLF
#     Sur Windows : git config core.autocrlf false
# ===================================================================
set -e

echo ">>> Initialisation des bases de donnÃ©es UBAX..."

# â”€â”€â”€ CrÃ©ation de db-keycloak â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE "db-keycloak"'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'db-keycloak')
    \gexec
EOSQL
echo ">>> Base db-keycloak : OK"

# â”€â”€â”€ CrÃ©ation de db-minio â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE "db-minio"'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'db-minio')
    \gexec
EOSQL
echo ">>> Base db-minio : OK"

# â”€â”€â”€ SchÃ©ma "administrative" dans la base principale (db-ubax) â”€â”€â”€â”€
# Requis par Hibernate : spring.jpa.properties.hibernate.default_schema
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE SCHEMA IF NOT EXISTS administrative;
EOSQL
echo ">>> SchÃ©ma 'administrative' dans $POSTGRES_DB : OK"

echo ">>> Initialisation terminÃ©e."
