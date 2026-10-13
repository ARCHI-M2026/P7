package com.yourcaryourway.tchat.clients.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ClientRepository extends JpaRepository<Client, UUID> {

    List<Client> findAllByOrderByNomAsc();
}
