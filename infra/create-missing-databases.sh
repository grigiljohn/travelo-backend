#!/bin/bash
# Script to create missing databases in existing PostgreSQL instance

echo "Creating missing databases..."

docker exec -i infra-postgres-1 psql -U travelo -d postgres < create-missing-databases.sql

echo "Done! Databases created."

