# Dorja Website Design Directions

## Three possible approaches

### 1. Quiet Momentum
**Very Brief Intro:** An editorial Bangladeshi product aesthetic that mixes rich ink tones, warm paper, and considered modular typography. It should feel reliable, articulate, and human.

**Probability:** 0.07

### 2. Civic Signal
**Very Brief Intro:** A clean service-design system built around clarity, utility, and confident information hierarchy. It would make Dorja feel like an everyday essential.

**Probability:** 0.04

### 3. Soft Circuit
**Very Brief Intro:** A contemporary interface-led direction with luminous gradients and floating glass layers. It emphasizes technical polish while retaining a calm, welcoming tone.

**Probability:** 0.09

## Chosen approach: Quiet Momentum

### Design Movement
**Editorial modernism with a vernacular publishing sensibility.** The site is conceived like a refined digital magazine cover meeting a useful product guide: composed, distinctive, and easy to scan.

### Core Principles
1. **Purpose before decoration:** Every visual gesture should clarify Dorja’s value or guide a decision.
2. **Assured asymmetry:** Off-center framing and staggered content give the site a crafted editorial rhythm without compromising readability.
3. **Tactile restraint:** Soft paper, thin ink rules, and subtle shadow establish depth without glossy excess.
4. **Product evidence:** The supplied application screens carry the narrative; supporting graphics only create context and emphasis.

### Color Philosophy
A warm, lightly textured off-white base will make the interface feel accessible rather than sterile. Deep ink blue-black grounds the experience in trust, while Dorja’s own primary logo color will drive action and moments of emphasis. A restrained muted-gold highlight will add warmth and hierarchy without competing with product imagery.

### Layout Paradigm
The page follows a **vertical editorial spine**: a narrow information rail anchors labels and section numbers on wider screens, while offset blocks of copy, product captures, and supporting cards create a paced reading path. This avoids a generic, centrally stacked landing page.

### Signature Elements
1. **The Dorja frame:** a thin nested border motif around key images, echoing a doorway and the product name.
2. **Editorial marginalia:** compact uppercase labels, section numbers, and hairline dividers guide the eye.
3. **Soft field blocks:** large color fields with gently shaped corners only where content needs tactile grouping.

### Interaction Philosophy
Interaction should feel direct and composed. Links reveal a short underline sweep, calls-to-action give a precise press response, and app screenshots lift minimally to reward exploration rather than distract from it.

### Animation
At initial load, major elements rise by 10–14px with opacity transitions over 420–560ms, staggered in 60ms increments. Hover motion uses transform and opacity only, with a crisp `cubic-bezier(0.23, 1, 0.32, 1)` easing. Buttons use a 140ms scale response on press. Motion is removed for people who prefer reduced motion.

### Typography System
**Gazi Jarin** is the visual signature for Bangla-facing display moments and navigation, reinforced by a deliberate weight and generous line-height. **Manrope** is used for English headings, metadata, buttons, and body copy. Display type has compact tracking; body copy is spacious and measured for clarity.

### Brand Essence
**Dorja is the composed, friendly digital door to useful everyday app experiences, created for people who want clarity without clutter.**

**Personality:** considered, warm, capable.

### Brand Voice
Headlines are concise and assured. CTAs describe the next useful action rather than issuing generic commands. Microcopy is practical and reassuring.

Example headline: “Everyday access, thoughtfully arranged.”

Example CTA: “Explore the Dorja experience”

### Wordmark & Logo
The supplied Dorja logo remains the primary identifier and appears prominently in the navigation, launch panel, and footer. Its surrounding **Dorja frame** expands the visual language without redrawing or diluting the supplied mark.

### Signature Brand Color
**Dorja Ember** — the primary warm accent sampled from the supplied logo after asset review — will be used decisively for calls-to-action, key navigation states, and minor graphic details.

## Style Decisions

- **Dorja Ember leads action:** Primary CTAs, section numbers, proof marks, and small evidence details use Dorja Ember as the decisive action accent. Teal and ink remain supporting product colors.
- **Vernacular presence is visual:** Bangla-facing display text appears in the hero as “বিশ্বাসের দরজা” alongside a compact English marginal label, keeping the brand rooted in its intended market rather than only naming Bangladesh in copy.
- **The Dorja frame repeats:** The nested doorway frame now surrounds the hero product evidence, reappears around the proof surface, and returns in the launch CTA moment so the frame becomes a recognizable brand device.
- **Alpha/beta is a first-class action:** The supplied Android release URL is used for the header, hero, mobile navigation, and closing CTA with safe external-link attributes.
- **Motion remains composed:** Hero movement uses layered transform/opacity reveals and restrained floating evidence; the logo loader is brief, status-labeled, and disabled for reduced-motion preferences.

**End of accepted review amendments.**

**No fabricated testimonials, reviews, ratings, or customer metrics.**
