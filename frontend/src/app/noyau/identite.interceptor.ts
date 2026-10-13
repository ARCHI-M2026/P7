import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';

import { SessionService } from './session.service';

/** PoC : ajoute l'identifiant de l'utilisateur. Cible : jeton OIDC (Authorization: Bearer ...). */
export const identiteInterceptor: HttpInterceptorFn = (requete, suivant) => {
  const compte = inject(SessionService).compte();
  return suivant(compte ? requete.clone({ setHeaders: { 'X-Utilisateur-Id': compte.id } }) : requete);
};
