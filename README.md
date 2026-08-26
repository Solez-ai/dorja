# DORJA 🚪

**A Bangladesh-first property discovery platform.**

DORJA turns a street-side property sign into a live, explorable, negotiable, and appointment-safe property record.

## Quick Start

```bash
# 1. Start infrastructure
pnpm dev:infra

# 2. Install dependencies
pnpm install

# 3. Set up environment
cp .env.example .env
# Edit .env with your values (minimum: generate random secrets)

# 4. Run migrations and seed
pnpm db:migrate
pnpm db:seed

# 5. Start all apps
pnpm dev
```

- **API:** http://localhost:4000
- **Web:** http://localhost:3000
- **Mobile:** http://localhost:19000

## Architecture

```
dorja/
├── apps/
│   ├── api/        # Fastify + Prisma backend
│   ├── web/        # Next.js discovery app
│   └── mobile/     # Expo React Native app
├── packages/
│   ├── contracts/  # Zod schemas + shared types
│   ├── domain/     # State machines, permissions, capture logic
│   ├── ui-tokens/  # Brand system (colors, typography, spacing)
│   └── config/     # Shared TypeScript config
├── infra/          # Docker Compose (Postgres, Redis, MinIO)
└── docs/           # Security, demo runbook
```

## Six Connected Systems

1. **Sign-to-Space:** QR codes on physical signs open live digital records
2. **Live Pulse:** Lister reconfirms availability; stale listings become visibly unconfirmed
3. **Reality Passport:** Guided phone capture produces navigable room tours with provenance
4. **Twin View:** Compare equivalent rooms across shortlisted properties
5. **Offer Room:** Structured rent/sale offers replace unrecorded negotiation
6. **SafeView:** Protected messaging, time-limited passes, check-in/out, safety reporting

## Tech Stack

| Area | Technology |
|------|-----------|
| Package manager | pnpm workspaces |
| Mobile | Expo SDK + React Native + Expo Router |
| Web | Next.js App Router |
| API | Fastify + Zod + Prisma |
| Database | PostgreSQL 16 |
| Cache/Queue | Redis 7 + BullMQ |
| Storage | MinIO (S3-compatible) |
| 3D/Tour | Marzipano/Pannellum |

## Commands

```bash
pnpm dev           # Start all apps
pnpm dev:infra     # Start Docker services
pnpm typecheck     # Type check all packages
pnpm test          # Run all tests
pnpm lint          # Lint all packages
pnpm build         # Build all packages
```

## Demo Listings

After seeding, two demo properties are available:
- **Mirpur 11:** `http://localhost:3000/properties/mirpur-family-apartment-demo-a1b2c3`
- **Uttara 7:** `http://localhost:3000/properties/uttara-family-apartment-demo-d4e5f6`

## Brand System

- **Jol Teal** `#007C78` — signature DORJA colour
- **Space Grotesk** — English display headings
- **IBM Plex Sans** — English body text
- **Hind Siliguri** — Bangla labels
- **IBM Plex Mono** — Numeric/time data, price blocks

## License

Proprietary — DORJA Team
