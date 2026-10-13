package com.yourcaryourway.tchat.support.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.yourcaryourway.tchat.support.SupportException;

/** Traduit les erreurs en réponses RFC 9457 (application/problem+json) avec un message lisible. */
@RestControllerAdvice
class ErreursApi {

    @ExceptionHandler(SupportException.class)
    ProblemDetail erreurSupport(SupportException e) {
        HttpStatus statut = switch (e.raison()) {
            case NON_AUTHENTIFIE -> HttpStatus.UNAUTHORIZED;
            case ACCES_REFUSE -> HttpStatus.FORBIDDEN;
            case INTROUVABLE -> HttpStatus.NOT_FOUND;
            case REGLE_METIER -> HttpStatus.CONFLICT;
        };
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(statut, e.getMessage());
        probleme.setProperty("code", e.raison().name());
        return probleme;
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail enteteManquant(MissingRequestHeaderException e) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "En-tête " + e.getHeaderName() + " manquant : connectez-vous.");
        probleme.setProperty("code", "NON_AUTHENTIFIE");
        return probleme;
    }
}
