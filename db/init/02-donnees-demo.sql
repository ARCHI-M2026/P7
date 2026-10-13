-- =====================================================================
-- Données de démonstration (identifiants fixes, repris dans le README)
-- Les dates sont relatives à l'heure du premier démarrage.
-- =====================================================================

-- Clients : quatre juridictions différentes
INSERT INTO clients.client (id, email, nom, prenom, langue, juridiction) VALUES
 ('11111111-1111-4111-8111-000000000001', 'camille.durand@exemple.fr',     'Durand',   'Camille', 'fr-FR', 'FR'),
 ('11111111-1111-4111-8111-000000000002', 'lucas.tremblay@exemple.ca',     'Tremblay', 'Lucas',   'fr-CA', 'CA-QC'),
 ('11111111-1111-4111-8111-000000000003', 'emily.johnson@example.com',     'Johnson',  'Emily',   'en-US', 'US-CA'),
 ('11111111-1111-4111-8111-000000000004', 'oliver.smith@example.co.uk',    'Smith',    'Oliver',  'en-GB', 'GB');

-- Conseillers
INSERT INTO support.conseiller (id, nom, zone, langues) VALUES
 ('22222222-2222-4222-8222-000000000001', 'Sophie Martin', 'EU', '{fr,en,es}'),
 ('22222222-2222-4222-8222-000000000002', 'Marc Gagnon',   'NA', '{fr,en}'),
 ('22222222-2222-4222-8222-000000000003', 'Hannah Weber',  'EU', '{de,en}');

-- 1) Conversation close (historique de Camille, US-27)
INSERT INTO support.conversation (id, client_id, conseiller_id, reservation_ref, sujet, statut, langue,
                                  ouverte_le, prise_en_charge_le, close_le, derniere_activite_le) VALUES
 ('33333333-3333-4333-8333-000000000001', '11111111-1111-4111-8111-000000000001', '22222222-2222-4222-8222-000000000001',
  'YCYW-2026-004512', 'Modifier une réservation', 'CLOSE', 'fr-FR',
  now() - interval '3 days', now() - interval '3 days' + interval '2 minutes',
  now() - interval '3 days' + interval '9 minutes', now() - interval '3 days' + interval '9 minutes');

INSERT INTO support.message (conversation_id, type_auteur, auteur_id, contenu, envoye_le, lu_le) VALUES
 ('33333333-3333-4333-8333-000000000001', 'CLIENT', '11111111-1111-4111-8111-000000000001',
  'Bonjour, je voudrais décaler le retour de ma location de Lyon au dimanche soir. Est-ce possible ?',
  now() - interval '3 days', now() - interval '3 days' + interval '2 minutes'),
 ('33333333-3333-4333-8333-000000000001', 'SYSTEME', NULL, 'Sophie Martin a rejoint la conversation.',
  now() - interval '3 days' + interval '2 minutes', NULL),
 ('33333333-3333-4333-8333-000000000001', 'CONSEILLER', '22222222-2222-4222-8222-000000000001',
  'Bonjour Camille, oui : le départ est dans plus de 48 h, la modification est possible. Je vous propose un retour dimanche à 19 h à l''agence Lyon Part-Dieu, sans supplément.',
  now() - interval '3 days' + interval '4 minutes', now() - interval '3 days' + interval '5 minutes'),
 ('33333333-3333-4333-8333-000000000001', 'CLIENT', '11111111-1111-4111-8111-000000000001',
  'Parfait, merci beaucoup !',
  now() - interval '3 days' + interval '6 minutes', now() - interval '3 days' + interval '6 minutes'),
 ('33333333-3333-4333-8333-000000000001', 'CONSEILLER', '22222222-2222-4222-8222-000000000001',
  'C''est fait, vous allez recevoir la confirmation par e-mail. Bonne route !',
  now() - interval '3 days' + interval '8 minutes', now() - interval '3 days' + interval '8 minutes'),
 ('33333333-3333-4333-8333-000000000001', 'SYSTEME', NULL, 'Conversation close par Sophie Martin.',
  now() - interval '3 days' + interval '9 minutes', NULL);

-- 2) Conversation en cours (Lucas, prise en charge par Marc ; dernier message non lu)
INSERT INTO support.conversation (id, client_id, conseiller_id, reservation_ref, sujet, statut, langue,
                                  ouverte_le, prise_en_charge_le, derniere_activite_le) VALUES
 ('33333333-3333-4333-8333-000000000002', '11111111-1111-4111-8111-000000000002', '22222222-2222-4222-8222-000000000002',
  'YCYW-2026-007731', 'Accessibilité', 'PRISE_EN_CHARGE', 'fr-CA',
  now() - interval '25 minutes', now() - interval '22 minutes', now() - interval '4 minutes');

INSERT INTO support.message (conversation_id, type_auteur, auteur_id, contenu, envoye_le, lu_le) VALUES
 ('33333333-3333-4333-8333-000000000002', 'CLIENT', '11111111-1111-4111-8111-000000000002',
  'Bonjour, je loue un véhicule à Montréal la semaine prochaine. J''ai besoin de commandes au volant (frein et accélérateur manuels). Est-ce disponible ?',
  now() - interval '25 minutes', now() - interval '22 minutes'),
 ('33333333-3333-4333-8333-000000000002', 'SYSTEME', NULL, 'Marc Gagnon a rejoint la conversation.',
  now() - interval '22 minutes', NULL),
 ('33333333-3333-4333-8333-000000000002', 'CONSEILLER', '22222222-2222-4222-8222-000000000002',
  'Bonjour Lucas ! L''agence Montréal Centre-Ville peut équiper une catégorie CDAR avec des commandes au volant. Je vérifie la disponibilité pour vos dates.',
  now() - interval '20 minutes', now() - interval '19 minutes'),
 ('33333333-3333-4333-8333-000000000002', 'CLIENT', '11111111-1111-4111-8111-000000000002',
  'Merci. Le retrait est prévu lundi à 9 h.',
  now() - interval '4 minutes', NULL);

-- 3) et 4) Conversations en attente : la file des conseillers n'est pas vide au démarrage
INSERT INTO support.conversation (id, client_id, sujet, statut, langue, ouverte_le, derniere_activite_le, reservation_ref) VALUES
 ('33333333-3333-4333-8333-000000000003', '11111111-1111-4111-8111-000000000003', 'Annulation et remboursement', 'EN_ATTENTE', 'en-US',
  now() - interval '7 minutes', now() - interval '7 minutes', 'YCYW-2026-009124'),
 ('33333333-3333-4333-8333-000000000004', '11111111-1111-4111-8111-000000000004', 'Paiement', 'EN_ATTENTE', 'en-GB',
  now() - interval '2 minutes', now() - interval '2 minutes', NULL);

INSERT INTO support.message (conversation_id, type_auteur, auteur_id, contenu, envoye_le) VALUES
 ('33333333-3333-4333-8333-000000000003', 'CLIENT', '11111111-1111-4111-8111-000000000003',
  'Hello, I need to cancel my booking in Paris (starts in 5 days). How much will be refunded?',
  now() - interval '7 minutes'),
 ('33333333-3333-4333-8333-000000000004', 'CLIENT', '11111111-1111-4111-8111-000000000004',
  'Hi, my card was charged twice for the same booking. Can you check?',
  now() - interval '2 minutes');
