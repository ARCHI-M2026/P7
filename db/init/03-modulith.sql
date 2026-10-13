-- =====================================================================
-- Table technique de Spring Modulith (registre des publications d'événements, « outbox »).
-- Schéma v2 de spring-modulith-events-jdbc 2.1 (fichier schemas/v2/schema-postgresql.sql).
-- Créée ici plutôt qu'au démarrage du backend : deux instances démarrent en parallèle.
-- =====================================================================
CREATE TABLE IF NOT EXISTS public.event_publication
(
  id                     UUID NOT NULL,
  listener_id            TEXT NOT NULL,
  event_type             TEXT NOT NULL,
  serialized_event       TEXT NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON public.event_publication USING hash(serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON public.event_publication (completion_date);
