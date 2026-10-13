package com.yourcaryourway.tchat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Vérifie l'architecture modulaire sans démarrer l'application :
 * pas de cycle entre modules, pas d'accès aux packages internes d'un autre module.
 * Génère aussi la documentation (diagrammes PlantUML, fiches des modules) dans target/spring-modulith-docs.
 */
class ModulariteTest {

    private final ApplicationModules modules = ApplicationModules.of(TchatApplication.class);

    @Test
    void lesFrontieresEntreModulesSontRespectees() {
        modules.forEach(System.out::println);
        modules.verify();
    }

    @Test
    void genererLaDocumentationDesModules() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
