# Dorja Website

The Dorja marketing website is built from `dorja-website-portable/` using a custom GitHub Actions workflow.

**Do not build from this folder.** This `docs/` directory is intentionally empty.

## Deployment

The site is deployed via `.github/workflows/deploy-pages.yml` which:
1. Builds the React/Vite app from `dorja-website-portable/`
2. Uploads the production build to GitHub Pages
3. Publishes to https://solez-ai.github.io/dorja/

## Configuration Required

In your GitHub repository settings:
1. Go to **Settings → Pages**
2. Under "Build and deployment"
3. Set **Source** to "GitHub Actions" (not "Deploy from a branch")

This ensures the custom workflow runs instead of Jekyll.
