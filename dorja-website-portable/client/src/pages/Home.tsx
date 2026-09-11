/**
 * Quiet Momentum: a trust-first editorial experience with warm paper surfaces,
 * ink-blue information hierarchy, and authentic Dorja product screens as evidence.
 */
import { useEffect, useState } from "react";
import {
  ArrowDownRight,
  ArrowRight,
  BadgeCheck,
  Check,
  ChevronRight,
  FileCheck2,
  Landmark,
  Menu,
  MessageSquareText,
  ScanLine,
  ShieldCheck,
  X,
} from "lucide-react";
import { publicUrl } from "@/lib/assets";

const logo = publicUrl("dorja-assets/dorja-logo.png");
const orbitMark = publicUrl("dorja-assets/dorja-orbit-mark.png");
const heroEditorial = publicUrl("dorja-assets/hero-editorial.jpg");
const featureCollage = publicUrl("dorja-assets/feature-collage.jpg");
const detailSurface = publicUrl("dorja-assets/detail-surface.jpg");
const alphaBetaUrl = "https://github.com/Solez-ai/dorja/releases/download/latest/app-release.apk";
const screens = {
  welcome: publicUrl("dorja-assets/welcome.jpg"),
  listing: publicUrl("dorja-assets/listing.png"),
  records: publicUrl("dorja-assets/records.jpg"),
};

const features = [
  {
    number: "01",
    icon: Landmark,
    name: "Verified listings",
    description:
      "Verify Khatian, Mutation, RAJUK plans, and supporting records before a property receives its trust signal.",
  },
  {
    number: "02",
    icon: ScanLine,
    name: "See the space first",
    description:
      "Create 360° property views with a phone’s camera and gyroscope—no specialised 360° hardware required.",
  },
  {
    number: "03",
    icon: ShieldCheck,
    name: "A safer viewing path",
    description:
      "SafeView QR passes help protect exact addresses, verify the visit, and support emergency-contact workflows.",
  },
  {
    number: "04",
    icon: FileCheck2,
    name: "A handover that holds up",
    description:
      "The Handover Passport captures promises, documents, milestones, and sign-off in one traceable record.",
  },
];

const journey = [
  ["01", "Discover", "Browse properties with essential context, not just surface-level listings."],
  ["02", "Verify", "Review documentation, seller verification, and the information behind the listing."],
  ["03", "Visit safely", "Use a verified SafeView pass before sharing the detail of a physical visit."],
  ["04", "Handover", "Keep each promise and document clear from the listing to the keys."],
];

type LightboxItem = { src: string; alt: string; label: string };

const detailChecks = [
  ["Identity", "NID-verified seller profiles and screening before a listing goes live.", BadgeCheck],
  ["Records", "Khatian, Mutation, RAJUK plans, and tax documents kept in context.", FileCheck2],
  ["Safety", "SafeView visits, protected addresses, and emergency-contact support.", ShieldCheck],
  ["Conversation", "Secure in-app messaging without exposing personal phone numbers.", MessageSquareText],
];

export default function Home() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [visibleSections, setVisibleSections] = useState<Set<string>>(new Set());
  const [lightbox, setLightbox] = useState<LightboxItem | null>(null);

  useEffect(() => {
    const revealTargets = document.querySelectorAll<HTMLElement>("[data-reveal]");
    if (!("IntersectionObserver" in window)) {
      setVisibleSections(new Set(Array.from(revealTargets).map((target) => target.dataset.reveal ?? "")));
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          const id = (entry.target as HTMLElement).dataset.reveal;
          if (!id) return;
          setVisibleSections((current) => {
            if (current.has(id)) return current;
            return new Set(current).add(id);
          });
          observer.unobserve(entry.target);
        });
      },
      { threshold: 0.12, rootMargin: "0px 0px -8% 0px" },
    );

    revealTargets.forEach((target) => observer.observe(target));
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (!lightbox) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setLightbox(null);
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [lightbox]);

  const revealClass = (id: string) => `reveal-on-scroll ${visibleSections.has(id) ? "is-visible" : ""}`;
  const openLightbox = (item: LightboxItem) => setLightbox(item);
  const closeMenu = () => setMenuOpen(false);

  return (
    <div className="site-shell">
      <header className="site-header">
        <a className="brand-lockup" href="#top" aria-label="Dorja home" onClick={closeMenu}>
          <img src={logo} alt="Dorja logo" />
          <span className="brand-name">DORJA</span>
          <span className="brand-subtitle">Property Trust Platform</span>
        </a>

        <nav className="desktop-nav" aria-label="Main navigation">
          <a href="#why-dorja">Why Dorja</a>
          <a href="#features">Trust system</a>
          <a href="#experience">App experience</a>
          <a href="#journey">How it works</a>
        </nav>

        <a className="header-cta" href={alphaBetaUrl} target="_blank" rel="noopener noreferrer">
          Try the alpha / beta <ArrowUpRightIcon />
        </a>

        <button
          className="menu-trigger"
          onClick={() => setMenuOpen((open) => !open)}
          aria-label={menuOpen ? "Close navigation menu" : "Open navigation menu"}
          aria-expanded={menuOpen}
        >
          {menuOpen ? <X size={22} /> : <Menu size={22} />}
        </button>

        {menuOpen && (
          <nav className="mobile-nav" aria-label="Mobile navigation">
            <a href="#why-dorja" onClick={closeMenu}>Why Dorja</a>
            <a href="#features" onClick={closeMenu}>Trust system</a>
            <a href="#experience" onClick={closeMenu}>App experience</a>
            <a href="#journey" onClick={closeMenu}>How it works</a>
            <a className="mobile-nav-cta" href={alphaBetaUrl} target="_blank" rel="noopener noreferrer" onClick={closeMenu}>Try the alpha / beta <ArrowRight size={17} /></a>
          </nav>
        )}
      </header>

      <main id="top">
        <section className="hero-section" aria-labelledby="hero-heading">
          <img
            className="hero-art"
            src={heroEditorial}
            alt="Abstract layered doorway artwork in Dorja brand colors"
          />
          <button
            className="image-trigger hero-art-trigger"
            type="button"
            onClick={() => openLightbox({ src: heroEditorial, alt: "Abstract layered doorway artwork in Dorja brand colors", label: "Hero artwork" })}
            aria-label="View hero artwork full screen"
          />
          <div className="hero-noise" aria-hidden="true" />
          <div className="hero-copy reveal-one">
            <div className="eyebrow"><span className="eyebrow-dot" /> Bangladesh’s property trust platform</div>
            <span className="hero-vernacular">বিশ্বাসের দরজা <i>TRUST, MADE LOCAL</i></span>
            <h1 id="hero-heading">Trust is built<br />into every <em>door.</em></h1>
            <p>
              Dorja brings verified records, safer visits, immersive property views, and a clear digital handover into one considered Android experience.
            </p>
            <div className="hero-actions">
              <a className="button-primary" href="#why-dorja">See what Dorja solves <ArrowDownRight size={19} /></a>
              <a className="text-action" href={alphaBetaUrl} target="_blank" rel="noopener noreferrer">Try the alpha / beta <ArrowRight size={17} /></a>
            </div>
          </div>

          <div className="hero-product reveal-two">
            <div className="product-tag"><BadgeCheck size={14} /> Built for Bangladesh</div>
            <div className="hero-phone-frame">
              <span className="hero-frame-note" aria-hidden="true">DORJA / PROOF 01</span>
              <span className="corner corner-tl" />
              <span className="corner corner-tr" />
              <span className="corner corner-bl" />
              <span className="corner corner-br" />
              <button
                className="image-trigger screenshot-trigger"
                type="button"
                onClick={() => openLightbox({ src: screens.welcome, alt: "Dorja mobile app sign-in screen", label: "Sign-in screen" })}
                aria-label="View sign-in screen full screen"
              >
                <img src={screens.welcome} alt="Dorja mobile app sign-in screen" />
              </button>
            </div>
            <div className="hero-caption"><span>01 / TRUST, MADE VISIBLE</span><span>ANDROID APP</span></div>
          </div>

          <div className="hero-side-note reveal-three">
            <span className="vertical-line" />
            <p>Every listing should give people a clearer, safer way forward.</p>
          </div>
        </section>

        <section className="trust-strip" aria-label="Dorja’s key principles">
          <p><BadgeCheck size={18} /> Verified people &amp; properties</p>
          <p><ScanLine size={18} /> 360° space exploration</p>
          <p><ShieldCheck size={18} /> Safer viewing workflows</p>
          <p><FileCheck2 size={18} /> Traceable handover records</p>
        </section>

        <section className={`problem-section section-pad ${revealClass("problem")}`} data-reveal="problem" id="why-dorja" aria-labelledby="problem-heading">
          <div className="section-rail"><span>01</span><span>THE TRUST GAP</span></div>
          <div className="problem-intro">
            <p className="section-kicker">Property should not begin with uncertainty</p>
            <h2 id="problem-heading">Beyond a listing.<br />A more accountable path.</h2>
          </div>
          <div className="problem-content">
            <p className="large-copy">Dorja is designed to replace fragmented calls, uncertain visits, and paper trails with a connected property experience built around proof.</p>
            <p className="body-copy">Whether someone is browsing, listing, visiting, or preparing for handover, Dorja places the essential information and trust signals in the same calm, straightforward flow.</p>
            <a href="#features" className="inline-link">Meet the trust system <ChevronRight size={18} /></a>
          </div>
          <div className="door-mark" aria-hidden="true"><span /><span /><span /></div>
        </section>

        <section className={`detail-band detail-band--proof ${revealClass("detail-band")}`} data-reveal="detail-band" aria-labelledby="detail-heading">
          <div className="detail-band-intro">
            <p className="section-kicker">What gets checked</p>
            <h2 id="detail-heading">More than a badge.<br /><em>A trail of proof.</em></h2>
            <p>Trust becomes useful when it is visible at the moment a decision is made. Dorja keeps the signals, documents, and conversations that matter close to the property journey.</p>
          </div>
          <div className="detail-check-grid">
            {detailChecks.map(([title, description, Icon]) => (
              <article className="detail-check" key={title as string}>
                <div className="detail-check-icon"><Icon size={19} /></div>
                <div><h3>{title as string}</h3><p>{description as string}</p></div>
                <Check className="detail-check-mark" size={17} />
              </article>
            ))}
          </div>
        </section>

        <section className={`features-section section-pad ${revealClass("features")}`} data-reveal="features" id="features" aria-labelledby="features-heading">
          <div className="feature-heading-wrap">
            <div className="section-rail"><span>02</span><span>THE SYSTEM</span></div>
            <div>
              <p className="section-kicker light-kicker">Designed around proof</p>
              <h2 id="features-heading">Confidence, <em>layer by layer.</em></h2>
            </div>
          </div>
          <div className="feature-grid">
            {features.map((feature) => {
              const Icon = feature.icon;
              return (
                <article className="feature-card" key={feature.number}>
                  <div className="feature-top"><span>{feature.number}</span><Icon size={27} strokeWidth={1.5} /></div>
                  <h3>{feature.name}</h3>
                  <p>{feature.description}</p>
                  <div className="feature-rule" />
                </article>
              );
            })}
          </div>
        </section>

        <section className={`experience-section section-pad ${revealClass("experience")}`} data-reveal="experience" id="experience" aria-labelledby="experience-heading">
          <div className="section-rail"><span>03</span><span>INSIDE THE APP</span></div>
          <div className="experience-heading">
            <p className="section-kicker">A more intentional property workflow</p>
            <h2 id="experience-heading">All the important<br />details, <em>in context.</em></h2>
          </div>

          <div className="experience-stage">
            <button
              className="image-trigger feature-art-trigger"
              type="button"
              onClick={() => openLightbox({ src: featureCollage, alt: "Abstract doorway collage in Dorja brand colors", label: "Product collage" })}
              aria-label="View product collage full screen"
            >
              <img className="feature-art" src={featureCollage} alt="Abstract doorway collage in Dorja brand colors" />
            </button>
            <div className="app-shot shot-listing">
              <button className="image-trigger screenshot-trigger" type="button" onClick={() => openLightbox({ src: screens.listing, alt: "Dorja app create new listing screen", label: "Create listing screen" })} aria-label="View create listing screen full screen">
                <img src={screens.listing} alt="Dorja app create new listing screen" />
              </button>
            </div>
            <div className="app-shot shot-records">
              <button className="image-trigger screenshot-trigger" type="button" onClick={() => openLightbox({ src: screens.records, alt: "Dorja app legal document verification screen", label: "Records screen" })} aria-label="View records screen full screen">
                <img src={screens.records} alt="Dorja app legal document verification screen" />
              </button>
            </div>
            <div className="stage-label label-one"><span>LISTING</span><i /></div>
            <div className="stage-label label-two"><i /><span>VERIFICATION</span></div>
            <div className="stage-copy-panel">
              <div className="stage-copy-eyebrow"><MessageSquareText size={16} /> BUILT FOR THE IMPORTANT DETAILS</div>
              <h3>Information that helps you move with clarity.</h3>
              <p>From protected addresses to legal documents, the experience makes the information behind a property easier to understand and act on.</p>
            </div>
          </div>
        </section>

        <section className={`journey-section section-pad ${revealClass("journey")}`} data-reveal="journey" id="journey" aria-labelledby="journey-heading">
          <div className="journey-lead">
            <div className="section-rail"><span>04</span><span>THE JOURNEY</span></div>
            <p className="section-kicker">From search to keys</p>
            <h2 id="journey-heading">One journey.<br /><em>Fewer blind spots.</em></h2>
            <button
              className="image-trigger journey-collage journey-collage-trigger"
              type="button"
              onClick={() => openLightbox({ src: detailSurface, alt: "Textured abstract Dorja brand surface", label: "Dorja surface artwork" })}
              aria-label="View journey artwork full screen"
            >
              <img src={detailSurface} alt="Textured abstract Dorja brand surface" />
            </button>
          </div>
          <div className="journey-list">
            {journey.map(([number, title, description]) => (
              <article className="journey-item" key={number}>
                <span>{number}</span>
                <div><h3>{title}</h3><p>{description}</p></div>
                <ArrowRight size={20} className="journey-arrow" />
              </article>
            ))}
          </div>
        </section>

        <section className={`closing-section ${revealClass("closing")}`} data-reveal="closing" aria-labelledby="closing-heading">
          <div className="closing-door" aria-hidden="true"><div /><div /><span /></div>
          <div className="closing-copy">
            <img src={orbitMark} alt="Dorja doorway symbol" />
            <p className="section-kicker">Property trust, made more human</p>
            <h2 id="closing-heading">A door should open<br />with <em>confidence.</em></h2>
            <div className="closing-actions">
              <a href={alphaBetaUrl} className="button-light" target="_blank" rel="noopener noreferrer">Try the alpha / beta <ArrowRight size={19} /></a>
              <a href="#top" className="closing-text-action">Back to top <ArrowRight size={17} /></a>
            </div>
          </div>
        </section>
      </main>

      {lightbox && (
        <div className="lightbox" role="dialog" aria-modal="true" aria-labelledby="lightbox-label" onMouseDown={(event) => { if (event.target === event.currentTarget) setLightbox(null); }}>
          <div className="lightbox-panel">
            <div className="lightbox-bar">
              <span id="lightbox-label">{lightbox.label}</span>
              <button className="lightbox-close" type="button" autoFocus onClick={() => setLightbox(null)} aria-label="Close full-screen image"><X size={21} /></button>
            </div>
            <img src={lightbox.src} alt={lightbox.alt} />
            <p>Press Escape or select close to return to the journey.</p>
          </div>
        </div>
      )}

      <footer className="site-footer">
        <div className="footer-brand"><img src={logo} alt="Dorja logo" /><span>DORJA</span></div>
        <p>Because every door should be trustworthy.</p>
        <p className="creator-credit">Created by <strong>Samin Yeasar</strong></p>
      </footer>
    </div>
  );
}

function ArrowUpRightIcon() {
  return <ArrowRight size={16} className="arrow-up-right" />;
}
