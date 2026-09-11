# Dorja — Portable Website Bundle

This folder contains the complete Dorja marketing website, including the React source code, configuration, production build, and every logo, screenshot, and artwork asset used by the page. The visual assets are stored locally in `client/public/dorja-assets/` and are also copied into `dist/public/dorja-assets/` by the production build, so the site does not depend on Manus storage, Vercel storage, GitHub-hosted images, or external asset downloads.

## Requirements

Install **Node.js 20 or newer**. The project uses Vite, React, and TypeScript. Package dependencies are described in `package.json` and locked in `pnpm-lock.yaml`.

## Run the editable development version

From this folder, install dependencies and start the development server:

```bash
pnpm install
pnpm dev
```

If pnpm is not installed, enable it with Corepack first:

```bash
corepack enable
corepack prepare pnpm@10.4.1 --activate
pnpm install
pnpm dev
```

Open the local URL printed by Vite, normally `http://localhost:5173` or the host/port supplied by your environment.

## Build and run the production version

```bash
pnpm install
pnpm run check
pnpm run build
pnpm start
```

The bundled server serves the built site from `dist/public`. The build also retains the local Dorja asset directory at `dist/public/dorja-assets/`.

## Serve the included build immediately

This ZIP includes a ready-built `dist/public/` directory. If you only need to preview the static website and do not want to start the bundled Node server, use any static-file server from the extracted folder. For example:

```bash
npx --yes serve dist/public
```

You can also use Python if it is installed:

```bash
cd dist/public
python3 -m http.server 4173
```

Then open `http://localhost:4173`.

## GitHub Pages

The site is published automatically by `.github/workflows/deploy-pages.yml` to <https://solez-ai.github.io/dorja/>. Production builds set `GITHUB_PAGES=true` so Vite uses `base: /dorja/` and every asset, favicon, and client route resolves under that prefix.

In the GitHub repo: **Settings → Pages → Source → GitHub Actions**.

Do not compile the site locally for publishing. Push to `main` (or run **Deploy GitHub Pages** via `workflow_dispatch`) and wait for the Actions run.

## Asset guarantee

All page image references use repository-local paths beginning with `/dorja-assets/`. The bundle includes these files:

| Asset group | Included files |
|---|---|
| Brand | `dorja-logo.png`, `dorja-orbit-mark.png` |
| Product screens | `welcome.jpg`, `listing.png`, `records.jpg` |
| Editorial artwork | `hero-editorial.jpg`, `feature-collage.jpg`, `detail-surface.jpg` |

Do not delete `client/public/dorja-assets/` when editing the source. If you rebuild the project, Vite copies this directory automatically into `dist/public/dorja-assets/`.

## Main project commands

| Command | Purpose |
|---|---|
| `pnpm dev` | Start the editable Vite development server |
| `pnpm run check` | Run the TypeScript check |
| `pnpm run build` | Create the production build and bundled server |
| `pnpm start` | Serve the production build |
| `pnpm run preview` | Preview the Vite production output |

The alpha/beta download button remains connected to the supplied Dorja APK release URL. The site is static and does not require environment variables for its local images or primary landing-page experience.
