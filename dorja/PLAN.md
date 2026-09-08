# DORJA Global Update — Implementation Plan

Authentic Apple Liquid Glass UI/UX Overhaul Plan (`apple-design-skill`) - C:\Projects\Dorja Homestation\apple-design-skill, TAKE HELP FORM C:\Projects\Dorja Homestation\apple-design-skill MUST MUST MUST

### 7.1 Objective
Transform Dorja from a raw developer prototype into a sleek, premium, Apple-grade mobile experience using **authentic Apple Liquid Glass** (not generic glassmorphism). Grounded in **Apple HIG Liquid Glass specifications** and native Android backdrop sampling (`RenderEffect` / `AndroidLiquidGlass` / Kyant `Backdrop`), while **keeping the original brand color scheme** (`Jol600` `#0061A4`, `Ink950`, `Paper50`) and **splash screen 100% intact**.

### 7.2 Core Concept: Layer Separation & Liquid Glass Rules
- **Content Layer** (Cards, Lists, Property details): Built using clean, opaque/subtle surfaces (`#FFFFFF` White cards on `#F8F9FE` `Paper50` canvas) with `0.5dp` translucent alpha borders (`Color(0x0C000000)`). No glass inside content cards.
- **Functional Layer** (Navigation, Toolbars, Primary CTAs, Sheets): Built using authentic **Liquid Glass**:
  - Real-time backdrop blur (`20–40px` dynamic radius) + 1.35x saturation boost.
  - Adaptive luminosity tinting (monochromatic contrast adjustment based on live underlying content).
  - Specular lens refraction border (`0.5dp` top-edge highlight stroke).

### 7.3 Liquid Glass Target Components - MUST SEE this - https://kyant.gitbook.io/backdrop 

| Element | HIG Variant | Liquid Glass Execution |
| --- | --- | --- |
| **Floating Bottom Navigation Bar** | Regular Variant | `28dp` rounded floating pill, `24dp` backdrop blur, specular top refraction stroke, animated liquid accent pill (`Jol600`) sliding under selected tab, haptic bump. | - https://kyant.gitbook.io/backdrop/tutorials/interactive-glass-bottom-bar
| **Top App Bars & Search Header** | Regular Variant | Floating Liquid Glass search container (`52dp` height, `26dp` pill radius, `Color(0xE6FAFBFD)` tint), real-time scroll edge blur of content underneath. | - https://kyant.gitbook.io/backdrop/tutorials/glass-bottom-bar
| **Primary Hero CTAs (3D Tour, SafeView Pass)** | Stained Glass Variant | Color-stained Liquid Glass (`Jol600` accent at 85% opacity over `20dp` backdrop blur with crisp white typography and specular stroke). | - https://kyant.gitbook.io/backdrop/tutorials/progressive-blur
| **Modal Sheets (Country & Language Pickers)** | Regular Variant | Floating `28dp` top-rounded glass container (`32dp` backdrop blur over 35% dark dimming layer) with grab handle. | - https://kyant.gitbook.io/backdrop/api/backdrop-effects

### 7.4 Typography & Spatial Grid
- **Strict 8dp Grid System**: `20dp` card squircles, `12dp` sub-tiles, `8dp` badges, `16dp` outer screen margins.
- **Typography Scale**: SF/Roboto typography scale (`Hero 28sp`, `Title 18sp`, `Body 15sp`, `Label 11sp`). Eliminate monospace from section headers.
- **Touch Targets**: Minimum `48dp x 48dp` interactive hit regions on all buttons, chips, and list rows with spring scale press feedback (`0.97f`).
