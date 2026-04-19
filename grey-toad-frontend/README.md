# Grey Toad Frontend

Angular 17 frontend for the Grey Toad project management & team chat app.

## Stack
- **Angular 17** (lazy-loaded feature modules)
- **SCSS** (dark monochromatic design system)
- **StompJS + SockJS** (real-time WebSocket chat)
- **JWT** (interceptor auto-attaches Bearer token)
- **Space Grotesk** font

## Quick Start

```bash
npm install
ng serve
```

App runs at `http://localhost:4200`. Backend expected at `http://localhost:8080`.

## Structure

```
src/app/
├── auth/                   # Login + Register pages
├── core/
│   ├── guards/             # AuthGuard
│   ├── interceptors/       # JwtInterceptor
│   └── services/           # AuthService, ApiService, ChatWsService
├── shared/
│   └── models/             # TypeScript interfaces (DTOs)
└── features/
    ├── shell/              # Sidebar layout wrapper
    ├── dashboard/          # Overview + stats
    ├── teams/              # Teams list + Team detail (members, channels)
    ├── tasks/              # Tasks list (filter by team/project) + Task detail (comments)
    └── chat/               # Real-time channel chat (WebSocket)
```

## Config

Edit `src/environments/environment.ts` to change API URL:

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  wsUrl: 'http://localhost:8080/ws'
};
```

## Features

| Page | Path | Description |
|------|------|-------------|
| Login | `/auth/login` | JWT login |
| Register | `/auth/register` | Create account |
| Dashboard | `/dashboard` | Stats, team list, quick actions |
| Teams | `/teams` | Create / delete teams |
| Team Detail | `/teams/:id` | Manage members & channels |
| Tasks | `/tasks` | Filter by team → project, create/update status |
| Task Detail | `/tasks/:id` | Full task view + comments |
| Chat | `/chat` | Real-time channel messaging via WebSocket |

## Design System

All styles live in `src/styles.scss`. Key CSS variables:

```scss
--bg-primary:   #0f0f0f  // page background
--bg-secondary: #1a1a1a  // sidebar, card backgrounds
--bg-card:      #1f1f1f  // cards
--text-primary: #e8e8e8
--border:       #2d2d2d
--font:         'Space Grotesk'
```
