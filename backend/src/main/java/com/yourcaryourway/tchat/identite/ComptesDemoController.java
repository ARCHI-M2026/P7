package com.yourcaryourway.tchat.identite;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yourcaryourway.tchat.clients.Clients;
import com.yourcaryourway.tchat.support.SupportService;

@RestController
@RequestMapping("/api/v1/demo")
class ComptesDemoController {

    private final Clients clients;
    private final SupportService support;

    ComptesDemoController(Clients clients, SupportService support) {
        this.clients = clients;
        this.support = support;
    }

    record CompteDemo(UUID id, String nom, String role, String detail) {
    }

    @GetMapping("/comptes")
    List<CompteDemo> comptes() {
        List<CompteDemo> comptes = new ArrayList<>();
        clients.tous().forEach(c -> comptes.add(new CompteDemo(c.id(), c.nomComplet(), "CLIENT",
                c.email() + " · " + c.juridiction() + " · " + c.langue())));
        support.conseillers().forEach(c -> comptes.add(new CompteDemo(c.id(), c.nom(), "CONSEILLER", c.detail())));
        return comptes;
    }
}
