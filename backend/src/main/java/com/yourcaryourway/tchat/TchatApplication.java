package com.yourcaryourway.tchat;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.Modulithic;

/**
 * Preuve de concept du tchat Your Car Your Way.
 * <p>
 * Monolithe modulaire : chaque sous-package direct ({@code clients}, {@code support}, {@code diffusion},
 * {@code notifications}, {@code identite}) est un module Spring Modulith. Les règles d'accès entre modules
 * sont vérifiées par le test {@code ModulariteTest}.
 */
@Modulithic(systemName = "Your Car Your Way - tchat")
@SpringBootApplication
public class TchatApplication {

    public static void main(String[] args) {
        SpringApplication.run(TchatApplication.class, args);
    }

    /** Horloge injectable : les règles basées sur le temps restent testables. */
    @Bean
    Clock horloge() {
        return Clock.systemUTC();
    }
}
