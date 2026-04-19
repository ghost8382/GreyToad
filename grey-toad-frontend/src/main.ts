// SockJS pulls in code paths that still expect a Node-like global.
// In the browser we can safely alias it to globalThis before lazy chunks load.
(globalThis as typeof globalThis & { global?: typeof globalThis }).global = globalThis;

import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

bootstrapApplication(AppComponent, appConfig)
  .catch((err: unknown) => console.error(err));
