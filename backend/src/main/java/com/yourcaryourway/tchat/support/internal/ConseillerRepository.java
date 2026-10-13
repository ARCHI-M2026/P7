package com.yourcaryourway.tchat.support.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConseillerRepository extends JpaRepository<Conseiller, UUID> {

    List<Conseiller> findAllByOrderByNomAsc();
}
