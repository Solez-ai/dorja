# GitHub Pages Deployment Fix

## Problem

The GitHub Pages deployment is failing because GitHub is trying to use Jekyll to build from the `docs/` folder, but the Dorja website is a React/Vite application that should be built using the custom GitHub Actions workflow.

**Error:** `No such file or directory @ dir_chdir0 - /github/workspace/docs`

## Root Cause

The GitHub Pages **Source** setting is configured to "Deploy from a branch" (probably `main` branch, `/docs` folder), which triggers Jekyll builds. This is the wrong setting for our React app.

## Solution

You **MUST** change the GitHub Pages source setting to use GitHub Actions instead of Jekyll.

### Steps to Fix (Required - Manual Action in GitHub UI)

1. **Go to your repository on GitHub:**
   https://github.com/Solez-ai/dorja

2. **Click "Settings"** (top navigation bar)

3. **Click "Pages"** (left sidebar under "Code and automation")

4. **Under "Build and deployment":**
   - **Source:** Change from "Deploy from a branch" to **"GitHub Actions"**
   
5. **Save** (if there's a save button) or the change takes effect immediately

6. **Trigger a new deployment:**
   - Go to "Actions" tab
   - Find the "Deploy GitHub Pages" workflow
   - Click "Run workflow" → select `main` branch → "Run workflow"

## What This Does

When you set the source to "GitHub Actions":
- GitHub stops trying to use Jekyll automatically
- Your custom workflow `.github/workflows/deploy-pages.yml` takes over
- The workflow properly builds the React/Vite app from `dorja-website-portable/`
- The production build is uploaded and deployed to Pages

## Verification

After changing the setting and triggering the workflow:

1. **Check Actions tab:** The workflow should show "Deploy GitHub Pages" running
2. **Build step should:**
   - Install pnpm
   - Run `pnpm install` in `dorja-website-portable/`
   - Run `pnpm exec vite build` with `GITHUB_PAGES=true`
   - Upload artifact from `dorja-website-portable/dist/public`
3. **Deploy step should:** Deploy the artifact to Pages
4. **Site URL:** https://solez-ai.github.io/dorja/

## Files Created/Modified

- ✅ `.nojekyll` - Disables Jekyll processing (already committed)
- ✅ `docs/README.md` - Documents why docs/ is empty (already committed)
- ✅ `.github/workflows/deploy-pages.yml` - Custom build workflow (already exists, no changes needed)

## Current Status

- [x] Code fixes committed and pushed
- [ ] **GitHub Pages source setting needs manual change** ← **YOU MUST DO THIS**
- [ ] Test deployment after setting change

---

## Quick Reference

**Correct Pages Settings:**
```
Settings → Pages → Build and deployment
Source: GitHub Actions (not "Deploy from a branch")
```

**Workflow File:** `.github/workflows/deploy-pages.yml`  
**Build Directory:** `dorja-website-portable/`  
**Output Directory:** `dorja-website-portable/dist/public`  
**Site URL:** https://solez-ai.github.io/dorja/

---

**Important:** The `.nojekyll` file and workflow are correctly configured. The only remaining issue is the GitHub UI setting for Pages source. Once you change that to "GitHub Actions", deployments will work correctly.
