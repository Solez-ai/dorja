# Dorja enhancement checklist

## Completed baseline and first refinement

- [x] Add richer product detail content for verification, safety, scanning, and handover.
- [x] Add polished scroll-reveal and hover motion with reduced-motion support.
- [x] Recompose the app-experience stage so its explanatory copy has a protected text panel below the screenshots.
- [x] Validate desktop and mobile layouts.
- [x] Save the revised checkpoint and deliver it.
- [x] Add a consistent left content gutter to the “What gets checked” section on desktop.
- [x] Constrain the section to the editorial content rail instead of letting it hug the viewport edge.
- [x] Preserve comfortable mobile padding and keep the four detail items readable.
- [x] Recheck the refined section at desktop and mobile widths, then save a checkpoint.

## Alpha/beta and motion refinement iteration

- [x] Increase the left and right breathing room around the “More than a badge. A trail of proof.” section, with alignment to the main editorial rail.
- [x] Add alpha/beta CTAs linking to the supplied APK release URL: https://github.com/Solez-ai/dorja/releases/download/latest/app-release.apk
- [x] Add safe external-link attributes to APK CTAs.
- [x] Improve hero entrance motion with layered reveals, paper movement, product-frame movement, and restrained floating evidence.
- [x] Add a supplied-Dorja-logo loading overlay for the full site with accessible status text.
- [x] Respect `prefers-reduced-motion` for the loader and hero motion.
- [x] Add visible Bangla-facing hero marginalia using the specified Gazi Jarin/Noto Sans Bengali font stack.
- [x] Strengthen the Dorja frame motif around product evidence and launch moments.
- [x] Make Dorja Ember the decisive action accent for primary CTAs and proof marks.
- [x] Clean the stylesheet and documentation after implementation.
- [x] Validate desktop and mobile compositions and run the production build.
- [x] Save a final checkpoint for this alpha/beta and motion refinement.

**Design direction:** Quiet Momentum — editorial Bangladeshi product design with warm paper surfaces, deep ink-blue hierarchy, Dorja Ember actions, visible vernacular marginalia, thin rules, and supplied app evidence.

**Brand requirements:** Preserve the supplied Dorja logo, authentic app screenshots, Gazi Jarin font stack, and Samin Yeasar creator credit.

**Motion rule:** Use transform and opacity for motion; keep interaction feedback crisp; gate non-essential motion with `prefers-reduced-motion`.

**Layout rule:** Keep proof-section content on a generous editorial rail with visible breathing room on desktop and restrained 12px framing on mobile.

**CTA rule:** The alpha/beta action uses the user-supplied APK release URL and does not attempt to download or execute the file during validation.

**Accessibility rule:** The loader includes `role="status"`, `aria-live="polite"`, and a text label; the APK links use clear action text.

**Content policy:** No fabricated testimonials, reviews, ratings, customer metrics, or invented user-generated content.

**Documentation note:** `todo.md.repetitive-backup` retains the previous oversized checklist history; this file is the active concise checklist.

**Scope:** Frontend-only changes in `client/src`, plus `ideas.md` and this checklist.

**Current phase:** Validated; final checkpoint saved for delivery.

**Next action:** Deliver the saved project version and publish only through the Management UI if desired.

**End of checklist.**

## Vercel image-loading repair

- [x] Inspect all Dorja image references and identify paths that are only available inside the Manus sandbox.
- [x] Inspect GitHub/Vercel build configuration and confirm whether the referenced assets are committed or externally hosted.
- [x] Replace broken image references with deployment-safe committed assets or stable public asset URLs.
- [x] Verify every image request in the production build and confirm no broken-image console/network errors.
- [ ] Save a checkpoint containing the Vercel image fix.

**User report:** The GitHub-connected Vercel site renders, but images do not load.

**Scope:** Preserve the existing layout, logo, screenshots, typography, and motion while fixing only asset delivery.

**Current phase:** Asset paths repaired and validated; media optimized; checkpoint retry pending.

## Lightbox, sticky navigation, and cursor iteration

- [ ] Make the navbar sticky with reliable contrast over the hero and while scrolling.
- [ ] Add an accessible full-screen lightbox for every screenshot and artwork surface.
- [ ] Support close button, Escape key, backdrop click, focus-visible styling, and descriptive image labels.
- [ ] Integrate the supplied macOS cursor treatment for interactive pointer contexts without replacing text-selection cursors.
- [ ] Respect reduced motion and coarse-pointer/mobile behavior for the cursor layer.
- [ ] Validate the new interactions at desktop and mobile sizes, then save a checkpoint.

**Current phase:** Requirements recorded; inspect interaction surfaces and cursor option next.

## Interaction follow-up validation

- [x] Recheck the lightbox click targets, Escape/backdrop close behavior, sticky navbar layering, and cursor treatment after the latest implementation pass.
- [x] Confirm the cursor treatment follows the supplied macOS cursor API intent without requiring Remotion rendering in the landing page.
- [ ] Save the interaction-upgrade checkpoint after final validation.

**Current phase:** Interaction behavior validated; checkpoint save pending.
