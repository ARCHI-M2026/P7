-- =====================================================================
-- Your Car Your Way - PoC tchat : schéma de la base (extrait du modèle physique)
-- Une seule base, un schéma par module (cf. Proposition d'architecture, § 4.4 et § 6.3)
-- Exécuté automatiquement par l'image PostgreSQL au premier démarrage (volume vide).
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS clients;
CREATE SCHEMA IF NOT EXISTS support;

-- ---------------------------------------------------------------------
-- Module Clients (extrait : les colonnes adresse, préférences, etc. ne sont pas utiles au tchat)
-- ---------------------------------------------------------------------
CREATE TABLE clients.client (
    id           uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    email        varchar(254) NOT NULL UNIQUE,
    nom          varchar(100) NOT NULL,
    prenom       varchar(100) NOT NULL,
    langue       varchar(10)  NOT NULL DEFAULT 'fr-FR',
    juridiction  varchar(6)   NOT NULL,
    cree_le      timestamptz  NOT NULL DEFAULT now()
);
COMMENT ON COLUMN clients.client.juridiction IS 'Territoire dont la loi s''applique aux données (RG-12) : FR, CA-QC, US-CA, GB...';

-- ---------------------------------------------------------------------
-- Module Support (tchat)
-- ---------------------------------------------------------------------
CREATE TABLE support.conseiller (
    id       uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    nom      varchar(150)  NOT NULL,
    zone     varchar(10)   NOT NULL,
    langues  varchar(10)[] NOT NULL DEFAULT '{}'
);

CREATE TABLE support.conversation (
    id                    uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Référence logique vers clients.client : pas de clé étrangère entre modules
    client_id             uuid         NOT NULL,
    conseiller_id         uuid         REFERENCES support.conseiller (id),
    reservation_ref       varchar(20),
    sujet                 varchar(120) NOT NULL,
    statut                varchar(20)  NOT NULL DEFAULT 'EN_ATTENTE',
    langue                varchar(10)  NOT NULL,
    ouverte_le            timestamptz  NOT NULL DEFAULT now(),
    prise_en_charge_le    timestamptz,
    close_le              timestamptz,
    derniere_activite_le  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT conversation_statut_ck CHECK (statut IN ('EN_ATTENTE', 'PRISE_EN_CHARGE', 'CLOSE')),
    CONSTRAINT conversation_conseiller_ck CHECK (statut <> 'PRISE_EN_CHARGE' OR conseiller_id IS NOT NULL),
    CONSTRAINT conversation_close_ck CHECK (statut <> 'CLOSE' OR close_le IS NOT NULL),
    CONSTRAINT conversation_reservation_ck CHECK (reservation_ref IS NULL OR reservation_ref ~ '^YCYW-[0-9]{4}-[0-9]{6}$')
);
COMMENT ON TABLE support.conversation IS 'Conversation de tchat client / conseiller (PF-11, US-25 à US-29). Conservée 1 an (RGPD-03).';

-- File d'attente des conseillers
CREATE INDEX conversation_file_idx ON support.conversation (statut, ouverte_le);
-- Historique d'un client (US-27)
CREATE INDEX conversation_client_idx ON support.conversation (client_id, derniere_activite_le DESC);

CREATE TABLE support.message (
    id               uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id  uuid         NOT NULL REFERENCES support.conversation (id) ON DELETE CASCADE,
    type_auteur      varchar(12)  NOT NULL,
    auteur_id        uuid,
    contenu          text         NOT NULL,
    envoye_le        timestamptz  NOT NULL DEFAULT now(),
    lu_le            timestamptz,
    CONSTRAINT message_type_auteur_ck CHECK (type_auteur IN ('CLIENT', 'CONSEILLER', 'SYSTEME')),
    CONSTRAINT message_auteur_ck CHECK ((type_auteur = 'SYSTEME') = (auteur_id IS NULL)),
    CONSTRAINT message_contenu_ck CHECK (char_length(contenu) BETWEEN 1 AND 2000)
);
COMMENT ON COLUMN support.message.lu_le IS 'Accusé de lecture : date de lecture par l''autre partie';

-- Affichage chronologique d'une conversation
CREATE INDEX message_conversation_idx ON support.message (conversation_id, envoye_le);
