package com.yourcaryourway.tchat.clients;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Interface publique du module Clients. */
public interface Clients {

    Optional<ClientInfo> trouver(UUID id);

    List<ClientInfo> tous();
}
