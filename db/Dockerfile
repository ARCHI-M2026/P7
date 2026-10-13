# Base PostgreSQL 18 avec les scripts d'initialisation (exécutés au premier démarrage, volume vide)
FROM postgres:18-alpine
COPY init/ /docker-entrypoint-initdb.d/
