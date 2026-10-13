import { inject } from '@angular/core';
import { CanActivateFn, Router, Routes } from '@angular/router';

import { Role } from './noyau/modeles';
import { SessionService } from './noyau/session.service';

// Garde : l'utilisateur doit être connecté avec le bon rôle, sinon retour à l'écran de connexion.
const role = (attendu: Role): CanActivateFn => () => {
  const compte = inject(SessionService).compte();
  return compte?.role === attendu ? true : inject(Router).parseUrl('/');
};

export const routes: Routes = [
  {
    path: '',
    title: 'Connexion · Support Your Car Your Way',
    loadComponent: () => import('./connexion/connexion').then((m) => m.Connexion),
  },
  {
    path: 'client',
    title: 'Mes conversations · Support Your Car Your Way',
    canActivate: [role('CLIENT')],
    loadComponent: () => import('./tchat/tchat').then((m) => m.Tchat),
  },
  {
    path: 'conseiller',
    title: 'Console conseiller · Support Your Car Your Way',
    canActivate: [role('CONSEILLER')],
    loadComponent: () => import('./tchat/tchat').then((m) => m.Tchat),
  },
  { path: '**', redirectTo: '' },
];
