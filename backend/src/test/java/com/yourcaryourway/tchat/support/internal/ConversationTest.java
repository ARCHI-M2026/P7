package com.yourcaryourway.tchat.support.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.yourcaryourway.tchat.support.StatutConversation;
import com.yourcaryourway.tchat.support.SupportException;

/** Règles de transition de l'agrégat Conversation (PF-11). */
class ConversationTest {

    private static final Instant T0 = Instant.parse("2026-10-12T08:00:00Z");
    private final UUID client = UUID.randomUUID();
    private final UUID sophie = UUID.randomUUID();
    private final UUID marc = UUID.randomUUID();

    @Test
    void uneConversationOuverteEstEnAttente() {
        Conversation c = Conversation.ouvrir(client, "Paiement", null, "fr-FR", T0);

        assertThat(c.getStatut()).isEqualTo(StatutConversation.EN_ATTENTE);
        assertThat(c.getConseillerId()).isNull();
    }

    @Test
    void unConseillerPrendEnChargeUneConversationEnAttente() {
        Conversation c = Conversation.ouvrir(client, "Paiement", null, "fr-FR", T0);

        c.prendreEnCharge(sophie, T0.plusSeconds(30));

        assertThat(c.getStatut()).isEqualTo(StatutConversation.PRISE_EN_CHARGE);
        assertThat(c.estSuiviePar(sophie)).isTrue();
        assertThat(c.getPriseEnChargeLe()).isEqualTo(T0.plusSeconds(30));
    }

    @Test
    void uneConversationNePeutPasEtrePriseEnChargeDeuxFois() {
        Conversation c = Conversation.ouvrir(client, "Paiement", null, "fr-FR", T0);
        c.prendreEnCharge(sophie, T0);

        assertThatThrownBy(() -> c.prendreEnCharge(marc, T0))
                .isInstanceOf(SupportException.class)
                .hasMessageContaining("déjà prise en charge");
    }

    @Test
    void uneConversationCloseNePeutPlusEvoluer() {
        Conversation c = Conversation.ouvrir(client, "Paiement", null, "fr-FR", T0);
        c.clore(T0.plusSeconds(60));

        assertThat(c.estClose()).isTrue();
        assertThatThrownBy(() -> c.prendreEnCharge(sophie, T0)).isInstanceOf(SupportException.class);
        assertThatThrownBy(() -> c.clore(T0)).isInstanceOf(SupportException.class);
    }
}
