package com.yourcaryourway.tchat.support;

/** Cycle de vie d'une conversation : EN_ATTENTE → PRISE_EN_CHARGE → CLOSE (ou EN_ATTENTE → CLOSE). */
public enum StatutConversation {
    EN_ATTENTE,
    PRISE_EN_CHARGE,
    CLOSE
}
