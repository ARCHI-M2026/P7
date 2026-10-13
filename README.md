# Your Car Your Way : preuve de concept du tchat

Preuve de concept (PoC) du **tchat entre clients et conseillers du service client**. Elle valide les choix de la *Proposition d'architecture* :

- **Front-end :** Angular 22, avec une console conseiller et un espace client.
- **Back-end :** Java 25 et Spring Boot 4.1, organisés en **monolithe modulaire** avec Spring Modulith 2.1.
- **Temps réel :** WebSocket et protocole STOMP, avec un **relais RabbitMQ entre deux instances** du back-end.
- **Données :** PostgreSQL 18, un schéma par module, avec les messages et les conversations.

Périmètre couvert dans le cahier des charges : parcours **PF-11**, user stories **US-25** (échanger avec un conseiller), **US-26** (tchat accessible), **US-27** (retrouver ses conversations) et exigence **ET-03** (canal temps réel multi-instance).

---

## Sommaire

1. [Démarrage rapide](#1-démarrage-rapide)
2. [Lancer, arrêter, remettre à zéro](#2-lancer-arrêter-remettre-à-zéro)
3. [Utiliser l'application](#3-utiliser-lapplication)
4. [Scénario de démonstration](#4-scénario-de-démonstration)
5. [Architecture](#5-architecture)

---

## 1. Démarrage rapide

```bash
cd ycyw-tchat-poc
docker compose up --build -d     
docker compose ps                # attendre que backend soit « healthy »
```

Ouvrir ensuite **http://localhost:8080** et choisir un compte de démonstration.

---

## 2. Lancer, arrêter, remettre à zéro

| Action | Commande |
|---|---|
| Construire et démarrer en arrière-plan | `docker compose up --build -d` |
| Suivre le démarrage | `docker compose logs -f backend` (Ctrl + C pour quitter) |
| État des services | `docker compose ps` |
| Arrêter (données conservées) | `docker compose down` |
| **Remettre à zéro** (base recréée avec les données de démo) | `docker compose down -v` puis `docker compose up --build -d` |
| Reconstruire après une modification du code | `docker compose up --build -d` |

Ordre de démarrage, géré automatiquement par les `healthcheck` :

```
db (PostgreSQL) ─┐
                 ├──► backend (2 instances) ──► frontend (nginx, port 8080)
rabbitmq ────────┘
```

---

## 3. Utiliser l'application

### 3.1 Comptes de démonstration

La PoC n'a pas de mot de passe : l'écran d'accueil liste les comptes et un clic suffit. Dans la cible, l'authentification passe par Keycloak (OpenID Connect, MFA).

| Rôle | Nom | Juridiction / zone | Situation au démarrage |
|---|---|---|---|
| Client | **Camille Durand** | FR, fr-FR | une conversation close (historique) |
| Client | **Lucas Tremblay** | CA-QC, fr-CA | une conversation **en cours** avec Marc |
| Client | **Emily Johnson** | US-CA, en-US | une demande **en attente** |
| Client | **Oliver Smith** | GB, en-GB | une demande **en attente** |
| Conseiller | **Sophie Martin** | zone EU | — |
| Conseiller | **Marc Gagnon** | zone NA | suit la conversation de Lucas |
| Conseiller | **Hannah Weber** | zone EU | — |

> **Astuce :** chaque **onglet** du navigateur garde sa propre session (sessionStorage). Ouvrez un onglet « client » et un onglet « conseiller » côte à côte. Une fenêtre de navigation privée ou un autre navigateur fonctionne aussi.

### 3.2 Espace client (`/client`)

- **Mes conversations :** liste triée par activité, avec le statut (*En attente*, *En cours*, *Close*), un aperçu du dernier message et le nombre de messages non lus.
- **Nouvelle demande :** choix du sujet, référence de réservation facultative (format `YCYW-2026-004512`) et premier message.
- **En attente :** un bandeau indique la **position dans la file** et le temps estimé. La position est mise à jour en direct.
- **Fil de discussion :**
  - bulles façon messagerie mobile (les vôtres à droite, en bleu) ;
  - accusés d'envoi et de lecture : ◷ envoi en cours, ✓ envoyé, ✓✓ lu ;
  - indicateur « … écrit » ;
  - séparateurs par jour et messages système (prise en charge, clôture).
- **Actions :** *Transcription* télécharge un fichier `.txt` (US-26). *Clore* demande une confirmation.
- **Clavier :** Entrée envoie le message, Maj + Entrée passe à la ligne.

### 3.3 Console conseiller (`/conseiller`)

- **Trois filtres :**
  - **File d'attente :** du plus ancien au plus récent, avec la durée d'attente, la juridiction et la langue du client ;
  - **Mes conversations :** celles que vous suivez ;
  - **Historique :** conversations closes ou suivies par un collègue.
- **Prendre en charge :** sur une conversation en attente, le fil est visible en lecture seule et le bouton *Prendre en charge* l'attribue. Si deux conseillers cliquent en même temps, le second reçoit « déjà prise en charge » (verrou en base).
- **Fiche :** référence de réservation, juridiction et langue du client.
- Une conversation suivie par un collègue est en **lecture seule**.

---

## 4. Scénario de démonstration 

1. **Onglet A :** se connecter en **Camille Durand**, puis *Nouvelle demande* avec le sujet « Modifier une réservation », la réservation `YCYW-2026-004512` et le message « Puis-je ajouter un siège enfant ? ». Le bandeau indique *Vous êtes n° 3 dans la file*.
2. **Onglet B :** se connecter en **Sophie Martin**. La file contient 3 demandes, celle de Camille en dernier. L'ouvrir et cliquer sur *Prendre en charge*.
3. **Onglet A :** le message système « Sophie Martin a rejoint la conversation » apparaît, le statut passe à *En cours* et le bandeau d'attente disparaît.
4. Échanger quelques messages et observer :
   - « Sophie Martin écrit… » pendant la frappe ;
   - ✓ puis ✓✓ quand l'autre onglet affiche la conversation ;
   - la pastille de non-lus si l'onglet destinataire regarde une autre conversation.
5. **Multi-instance :** comparer le nom d'instance affiché dans les deux onglets. S'ils sont identiques, recharger un onglet (F5) jusqu'à obtenir deux instances différentes : les messages passent quand même, **via RabbitMQ**.
6. **Reprise après coupure :**
   - `docker ps --filter name=backend` pour lister les instances ;
   - `docker stop <nom-d-une-instance>` ;
   - l'onglet concerné affiche *Reconnexion…* puis repasse *En ligne* sur l'autre instance et recharge l'historique : aucun message perdu ;
   - `docker compose up -d` pour relancer l'instance.
7. **Onglet B :** *Clore*, puis *Transcription*. Dans les journaux, la notification simulée s'affiche :
   `docker compose logs backend | grep "E-mail simulé"`.
8. **Onglet A :** se déconnecter et se reconnecter en **Lucas Tremblay** pour retrouver une conversation en cours avec un message non lu par Marc. Se connecter en **Marc Gagnon** dans l'onglet B pour la reprendre.

---

## 5. Architecture

### 5.1 Services Docker

```
                   ┌──────────────────────────┐
 Navigateur ──────►│ frontend (nginx :80)     │  Angular 22 (fichiers statiques)
 http://…:8080     │  /api/* et /ws ──────────┼──┐  répartition entre les instances
                   └──────────────────────────┘  │
                         ┌───────────────────────┴──────────────┐
                         ▼                                      ▼
               ┌───────────────────┐                  ┌───────────────────┐
               │ backend #1 :8080  │                  │ backend #2 :8080  │  Spring Boot 4.1
               └───┬──────────┬────┘                  └────┬──────────┬───┘  Spring Modulith 2.1
                   │ JDBC     │ STOMP (relais)  :61613     │          │
                   ▼          ▼                            ▼          │
          ┌──────────────┐  ┌──────────────────────────────────┐      │
          │ db           │  │ rabbitmq (STOMP + console :15672)│◄─────┘
          │ PostgreSQL 18│  └──────────────────────────────────┘
          └──────────────┘◄──────────────────────────────────────────── JDBC
```


### 5.4 Modèle de données (extrait)

| Schéma.table | Contenu | Points notables |
|---|---|---|
| `clients.client` | Identité minimale du client | `juridiction` (RG-12), `langue` |
| `support.conseiller` | Conseillers | `zone`, `langues[]` |
| `support.conversation` | Une demande de support | `statut` vérifié par contrainte ; `reservation_ref` au format vérifié ; index `(statut, ouverte_le)` pour la file et `(client_id, derniere_activite_le)` pour l'historique |
| `support.message` | Messages | `type_auteur` CLIENT / CONSEILLER / SYSTEME ; `lu_le` pour l'accusé de lecture ; index `(conversation_id, envoye_le)` |
| `public.event_publication` | Registre des événements Spring Modulith | Garantit qu'un événement n'est jamais perdu |

Il n'y a **pas de clé étrangère entre schémas de modules différents** (`conversation.client_id` → `clients.client`). Les modules ne partagent que des identifiants, ce qui permet d'extraire un module plus tard sans casser la base.

---
