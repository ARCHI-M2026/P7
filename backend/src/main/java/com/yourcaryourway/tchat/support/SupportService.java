package com.yourcaryourway.tchat.support;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourcaryourway.tchat.clients.ClientInfo;
import com.yourcaryourway.tchat.clients.Clients;
import com.yourcaryourway.tchat.support.internal.Conseiller;
import com.yourcaryourway.tchat.support.internal.ConseillerRepository;
import com.yourcaryourway.tchat.support.internal.Conversation;
import com.yourcaryourway.tchat.support.internal.ConversationRepository;
import com.yourcaryourway.tchat.support.internal.Message;
import com.yourcaryourway.tchat.support.internal.MessageRepository;

/**
 * Interface publique du module Support : cas d'usage du tchat.
 * <p>
 * Chaque changement d'état publie un événement de domaine. Spring Modulith l'enregistre dans la table
 * {@code event_publication} dans la même transaction (outbox), puis le livre aux autres modules
 * (diffusion temps réel, notifications) après validation de la transaction.
 */
@Service
@Transactional
public class SupportService {

    static final int LONGUEUR_MAX_MESSAGE = 2000;
    static final int LONGUEUR_MAX_SUJET = 120;
    private static final Pattern REFERENCE_RESERVATION = Pattern.compile("^YCYW-\\d{4}-\\d{6}$");
    private static final String NOM_SYSTEME = "Your Car Your Way";

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ConseillerRepository conseillers;
    private final Clients clients;
    private final ApplicationEventPublisher evenements;
    private final Clock horloge;

    public SupportService(ConversationRepository conversations, MessageRepository messages,
                          ConseillerRepository conseillers, Clients clients,
                          ApplicationEventPublisher evenements, Clock horloge) {
        this.conversations = conversations;
        this.messages = messages;
        this.conseillers = conseillers;
        this.clients = clients;
        this.evenements = evenements;
        this.horloge = horloge;
    }

    // ------------------------------------------------------------------ identité

    /** Résout l'utilisateur : conseiller (module Support) ou client (module Clients). */
    @Transactional(readOnly = true)
    public Acteur acteur(UUID id) {
        if (id == null) {
            throw SupportException.nonAuthentifie();
        }
        return conseillers.findById(id)
                .map(c -> new Acteur(c.getId(), TypeAuteur.CONSEILLER, c.getNom(), "Conseiller · zone " + c.getZone()))
                .or(() -> clients.trouver(id)
                        .map(c -> new Acteur(c.id(), TypeAuteur.CLIENT, c.nomComplet(),
                                "Client · " + c.juridiction() + " · " + c.langue())))
                .orElseThrow(SupportException::nonAuthentifie);
    }

    @Transactional(readOnly = true)
    public List<Acteur> conseillers() {
        return conseillers.findAllByOrderByNomAsc().stream()
                .map(c -> new Acteur(c.getId(), TypeAuteur.CONSEILLER, c.getNom(), "Conseiller · zone " + c.getZone()))
                .toList();
    }

    // ------------------------------------------------------------------ lecture

    /** Client : ses conversations. Conseiller : toutes les conversations récentes (file, en cours, closes). */
    @Transactional(readOnly = true)
    public List<ConversationResume> conversations(Acteur lecteur) {
        List<Conversation> liste = lecteur.estClient()
                ? conversations.findByClientIdOrderByDerniereActiviteLeDesc(lecteur.id())
                : conversations.findTop200ByOrderByDerniereActiviteLeDesc();
        Map<UUID, Integer> positions = positionsDansLaFile();
        return liste.stream().map(c -> resume(c, positions, lecteur)).toList();
    }

    @Transactional(readOnly = true)
    public ConversationResume conversation(Acteur lecteur, UUID id) {
        return resume(accessible(lecteur, id), positionsDansLaFile(), lecteur);
    }

    @Transactional(readOnly = true)
    public List<MessageVue> messages(Acteur lecteur, UUID id) {
        Conversation c = accessible(lecteur, id);
        Noms noms = noms(c);
        return messages.findByConversationIdOrderByEnvoyeLeAsc(c.getId()).stream()
                .map(m -> vue(m, noms))
                .toList();
    }

    /** Vue « système » d'une conversation, sans contrôle d'accès : réservée aux modules internes. */
    @Transactional(readOnly = true)
    public Optional<ConversationResume> resume(UUID id) {
        return conversations.findById(id).map(c -> resume(c, positionsDansLaFile(), null));
    }

    /** Conversations en attente, dans l'ordre de prise en charge. */
    @Transactional(readOnly = true)
    public List<ConversationResume> fileAttente() {
        Map<UUID, Integer> positions = positionsDansLaFile();
        return conversations.findByStatutOrderByOuverteLeAsc(StatutConversation.EN_ATTENTE).stream()
                .map(c -> resume(c, positions, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean peutAcceder(Acteur lecteur, UUID conversationId) {
        return conversations.findById(conversationId)
                .map(c -> lecteur.estConseiller() || c.getClientId().equals(lecteur.id()))
                .orElse(false);
    }

    // ------------------------------------------------------------------ cas d'usage

    /** PF-11 étape 1 : le client ouvre une conversation avec un premier message. */
    public ConversationResume ouvrir(Acteur client, String sujet, String reservationRef, String premierMessage) {
        if (!client.estClient()) {
            throw SupportException.accesRefuse("Seul un client peut ouvrir une conversation.");
        }
        String sujetValide = texte(sujet, LONGUEUR_MAX_SUJET, "Le sujet");
        String contenu = texte(premierMessage, LONGUEUR_MAX_MESSAGE, "Le message");
        String reference = referenceReservation(reservationRef);
        ClientInfo info = clients.trouver(client.id()).orElseThrow(SupportException::nonAuthentifie);

        Instant maintenant = horloge.instant();
        Conversation c = conversations.save(Conversation.ouvrir(client.id(), sujetValide, reference, info.langue(), maintenant));
        Message m = messages.save(Message.de(c.getId(), TypeAuteur.CLIENT, client.id(), contenu, maintenant));

        evenements.publishEvent(new ConversationOuverte(c.getId(), client.id()));
        evenements.publishEvent(new MessageEnvoye(vue(m, noms(c)), null));
        return resume(c, positionsDansLaFile(), client);
    }

    /** PF-11 étape 2 : un conseiller prend la conversation en charge (une seule fois). */
    public ConversationResume prendreEnCharge(Acteur conseiller, UUID id) {
        if (!conseiller.estConseiller()) {
            throw SupportException.accesRefuse("Réservé aux conseillers.");
        }
        Conversation c = conversations.verrouiller(id).orElseThrow(SupportException::introuvable);
        Instant maintenant = horloge.instant();
        c.prendreEnCharge(conseiller.id(), maintenant);
        Message m = messages.save(Message.de(c.getId(), TypeAuteur.SYSTEME, null,
                conseiller.nom() + " a rejoint la conversation.", maintenant));

        evenements.publishEvent(new ConversationPriseEnCharge(c.getId(), conseiller.id()));
        evenements.publishEvent(new MessageEnvoye(vue(m, noms(c)), null));
        return resume(c, positionsDansLaFile(), conseiller);
    }

    /** PF-11 étape 3 : échange de messages. */
    public MessageVue envoyer(Acteur auteur, UUID id, String texte, String idTemporaire) {
        Conversation c = accessible(auteur, id);
        if (c.estClose()) {
            throw SupportException.regle("Cette conversation est close : ouvrez-en une nouvelle si besoin.");
        }
        if (auteur.estConseiller() && !c.estSuiviePar(auteur.id())) {
            throw SupportException.regle("Prenez la conversation en charge avant de répondre.");
        }
        Instant maintenant = horloge.instant();
        Message m = messages.save(Message.de(c.getId(), auteur.type(), auteur.id(),
                texte(texte, LONGUEUR_MAX_MESSAGE, "Le message"), maintenant));
        c.enregistrerActivite(maintenant);

        MessageVue vue = vue(m, noms(c));
        evenements.publishEvent(new MessageEnvoye(vue, idTemporaire));
        return vue;
    }

    /** PF-11 étape 4 : clôture par le client ou par le conseiller qui suit la conversation. */
    public ConversationResume clore(Acteur acteur, UUID id) {
        Conversation c = accessible(acteur, id);
        if (acteur.estConseiller() && !c.estSuiviePar(acteur.id())) {
            throw SupportException.regle("Seul le conseiller qui suit la conversation peut la clore.");
        }
        Instant maintenant = horloge.instant();
        c.clore(maintenant);
        String texte = acteur.estClient()
                ? "Conversation close par le client."
                : "Conversation close par " + acteur.nom() + ".";
        Message m = messages.save(Message.de(c.getId(), TypeAuteur.SYSTEME, null, texte, maintenant));

        evenements.publishEvent(new ConversationCloturee(c.getId(), c.getClientId(), acteur.type()));
        evenements.publishEvent(new MessageEnvoye(vue(m, noms(c)), null));
        return resume(c, positionsDansLaFile(), acteur);
    }

    /** Accusé de lecture : marque comme lus les messages de l'autre partie. */
    public void marquerLus(Acteur lecteur, UUID id) {
        Conversation c = accessible(lecteur, id);
        if (lecteur.estConseiller() && !c.estSuiviePar(lecteur.id())) {
            return; // un conseiller qui consulte la file ne vaut pas lecture pour le client
        }
        TypeAuteur autre = lecteur.estClient() ? TypeAuteur.CONSEILLER : TypeAuteur.CLIENT;
        Instant maintenant = horloge.instant();
        if (messages.marquerLus(c.getId(), autre, maintenant) > 0) {
            evenements.publishEvent(new MessagesLus(c.getId(), lecteur.type(), maintenant));
        }
    }

    // ------------------------------------------------------------------ outils

    /** Un client ne voit que ses conversations ; on répond « introuvable » pour ne rien divulguer. */
    private Conversation accessible(Acteur lecteur, UUID id) {
        Conversation c = conversations.findById(id).orElseThrow(SupportException::introuvable);
        if (lecteur.estClient() && !c.getClientId().equals(lecteur.id())) {
            throw SupportException.introuvable();
        }
        return c;
    }

    private Map<UUID, Integer> positionsDansLaFile() {
        Map<UUID, Integer> positions = new HashMap<>();
        List<Conversation> file = conversations.findByStatutOrderByOuverteLeAsc(StatutConversation.EN_ATTENTE);
        for (int i = 0; i < file.size(); i++) {
            positions.put(file.get(i).getId(), i + 1);
        }
        return positions;
    }

    private ConversationResume resume(Conversation c, Map<UUID, Integer> positions, Acteur lecteur) {
        Optional<ClientInfo> client = clients.trouver(c.getClientId());
        String conseillerNom = c.getConseillerId() == null ? null
                : conseillers.findById(c.getConseillerId()).map(Conseiller::getNom).orElse(null);
        Optional<Message> dernier = messages.findFirstByConversationIdOrderByEnvoyeLeDesc(c.getId());
        Long nonLus = null;
        if (lecteur != null) {
            TypeAuteur autre = lecteur.estClient() ? TypeAuteur.CONSEILLER : TypeAuteur.CLIENT;
            nonLus = messages.countByConversationIdAndTypeAuteurAndLuLeIsNull(c.getId(), autre);
        }
        return new ConversationResume(
                c.getId(), c.getSujet(), c.getReservationRef(), c.getStatut(), c.getLangue(),
                c.getClientId(),
                client.map(ClientInfo::nomComplet).orElse("Client inconnu"),
                client.map(ClientInfo::juridiction).orElse(null),
                c.getConseillerId(), conseillerNom,
                c.getOuverteLe(), c.getPriseEnChargeLe(), c.getCloseLe(), c.getDerniereActiviteLe(),
                dernier.map(m -> apercu(m.getContenu())).orElse(null),
                dernier.map(Message::getTypeAuteur).orElse(null),
                positions.get(c.getId()),
                nonLus);
    }

    private record Noms(String client, String conseiller) {
    }

    private Noms noms(Conversation c) {
        String client = clients.trouver(c.getClientId()).map(ClientInfo::nomComplet).orElse("Client");
        String conseiller = c.getConseillerId() == null ? "Conseiller"
                : conseillers.findById(c.getConseillerId()).map(Conseiller::getNom).orElse("Conseiller");
        return new Noms(client, conseiller);
    }

    private static MessageVue vue(Message m, Noms noms) {
        String auteur = switch (m.getTypeAuteur()) {
            case CLIENT -> noms.client();
            case CONSEILLER -> noms.conseiller();
            case SYSTEME -> NOM_SYSTEME;
        };
        return new MessageVue(m.getId(), m.getConversationId(), m.getTypeAuteur(), m.getAuteurId(), auteur,
                m.getContenu(), m.getEnvoyeLe(), m.getLuLe());
    }

    private static String apercu(String contenu) {
        String uneLigne = contenu.replaceAll("\\s+", " ").trim();
        return uneLigne.length() <= 90 ? uneLigne : uneLigne.substring(0, 89) + "…";
    }

    private static String texte(String valeur, int longueurMax, String libelle) {
        String t = valeur == null ? "" : valeur.strip();
        if (t.isEmpty()) {
            throw SupportException.regle(libelle + " est obligatoire.");
        }
        if (t.length() > longueurMax) {
            throw SupportException.regle(libelle + " dépasse " + longueurMax + " caractères.");
        }
        return t;
    }

    private static String referenceReservation(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        String ref = valeur.strip().toUpperCase();
        if (!REFERENCE_RESERVATION.matcher(ref).matches()) {
            throw SupportException.regle("Référence de réservation invalide (format attendu : YCYW-2026-004512).");
        }
        return ref;
    }
}
