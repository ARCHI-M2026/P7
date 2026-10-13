package com.yourcaryourway.tchat.clients.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourcaryourway.tchat.clients.ClientInfo;
import com.yourcaryourway.tchat.clients.Clients;

@Service
@Transactional(readOnly = true)
class ClientsJpa implements Clients {

    private final ClientRepository repository;

    ClientsJpa(ClientRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ClientInfo> trouver(UUID id) {
        return id == null ? Optional.empty() : repository.findById(id).map(ClientsJpa::vers);
    }

    @Override
    public List<ClientInfo> tous() {
        return repository.findAllByOrderByNomAsc().stream().map(ClientsJpa::vers).toList();
    }

    private static ClientInfo vers(Client c) {
        return new ClientInfo(c.getId(), c.getPrenom(), c.getNom(), c.getEmail(), c.getLangue(), c.getJuridiction());
    }
}
