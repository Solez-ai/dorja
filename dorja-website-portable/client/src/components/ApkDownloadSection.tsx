/**
 * APK download section: fetches the per-ABI builds from the `latest` GitHub
 * release and presents each download with its CPU architecture, build type,
 * size, hash and step-by-step install instructions.
 */
import { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  ArrowRight,
  BadgeCheck,
  Check,
  Cpu,
  Download,
  ExternalLink,
  Fingerprint,
  Hourglass,
  Smartphone,
  ListOrdered,
  Wrench,
} from "lucide-react";

const RELEASES_API =
  "https://api.github.com/repos/Solez-ai/dorja/releases/tags/latest";
const RELEASES_PAGE =
  "https://github.com/Solez-ai/dorja/releases/tag/latest";

type ReleaseAsset = {
  name: string;
  size: number;
  browser_download_url: string;
  digest?: string | null;
  created_at?: string;
  updated_at?: string;
};

type ApkFile = {
  name: string;
  abi: string;
  abiLabel: string;
  buildType: "debug" | "release";
  sizeMb: string;
  digest: string | null;
  publishedAt: string | null;
  url: string;
  recommended: boolean;
};

const ABI_LABELS: Record<string, string> = {
  "arm64-v8a": "Modern phones · 64-bit",
  "armeabi-v7a": "Older phones · 32-bit",
};

function detectDeviceAbi(): string | null {
  if (typeof navigator === "undefined") return null;
  const ua = navigator.userAgent;
  if (!/android/i.test(ua)) return "arm64-v8a"; // Desktop users: preview the most common build.
  if (/x86_64|emulator/i.test(ua)) return "x86_64";
  // Every in-market Android 10+ phone ships 64-bit; 32-bit roots only predate ~2016.
  return "arm64-v8a";
}

function formatSize(bytes: number): string {
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function formatAge(iso: string | undefined): string {
  if (!iso) return "";
  const minutes = Math.max(1, Math.round((Date.now() - new Date(iso).getTime()) / 60_000));
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} h ago`;
  const days = Math.round(hours / 24);
  return `${days} d ago`;
}

function parseApk(asset: ReleaseAsset, deviceAbi: string | null): ApkFile | null {
  // Per-ABI split output only: skips the retired universal app-debug.apk /
  // app-release.apk files that linger on older release entries.
  const match = asset.name.match(/^app-(arm64-v8a|armeabi-v7a|x86_64|x86)-(debug|release)\.apk$/);
  if (!match) return null;
  const [, abi, buildType] = match;
  const digest = asset.digest && asset.digest.startsWith("sha256:")
    ? asset.digest.slice("sha256:".length)
    : null;
  return {
    name: asset.name,
    abi,
    abiLabel: ABI_LABELS[abi] ?? abi,
    buildType: buildType as "debug" | "release",
    sizeMb: formatSize(asset.size),
    digest,
    publishedAt: formatAge(asset.updated_at ?? asset.created_at),
    url: asset.browser_download_url,
    recommended: deviceAbi !== null && abi === deviceAbi,
  };
}

const INSTALL_STEPS = [
  "Tap the APK for your phone above — most Android phones use the arm64-v8a row.",
  "Android asks permission to install from the browser: choose Settings → allow “Install unknown apps” for this browser, then return.",
  "Open the downloaded file and tap Install.",
  "Updating an older Dorja build? Uninstall it first — every release build signs with a fresh key, so Android refuses an in-place upgrade.",
];

export default function ApkDownloadSection({ revealClass }: { revealClass: (id: string) => string }) {
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [files, setFiles] = useState<ApkFile[]>([]);
  const [releasePublished, setReleasePublished] = useState<string | null>(null);
  const deviceAbi = useMemo(detectDeviceAbi, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const response = await fetch(RELEASES_API, {
          headers: { Accept: "application/vnd.github+json" },
        });
        if (!response.ok) throw new Error(`GitHub API ${response.status}`);
        const release = (await response.json()) as { assets?: ReleaseAsset[]; published_at?: string };
        if (cancelled) return;
        const parsed = (release.assets ?? [])
          .map((asset) => parseApk(asset, deviceAbi))
          .filter((file): file is ApkFile => file !== null);
        setFiles(parsed);
        setReleasePublished(release.published_at ? formatAge(release.published_at) : null);
        setStatus("ready");
      } catch {
        if (!cancelled) setStatus("error");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [deviceAbi]);

  const debugFiles = files.filter((file) => file.buildType === "debug");
  const releaseFiles = files.filter((file) => file.buildType === "release");
  const groups: { key: string; badge: string; title: string; note: string; files: ApkFile[] }[] = releaseFiles.length
    ? [
        { key: "release", badge: "RELEASE", title: "Release build", note: "Optimised, signed with the production key, ready for daily use.", files: releaseFiles },
        { key: "debug", badge: "DEBUG", title: "Debug build", note: "Fully installable, no signing keys needed — the easiest way to try Dorja.", files: debugFiles },
      ]
    : debugFiles.length
      ? [{ key: "debug", badge: "DEBUG", title: "Debug build", note: "Fully installable, no signing keys needed — the easiest way to try Dorja.", files: debugFiles }]
      : [];

  return (
    <section
      className={`apk-download-section section-pad ${revealClass("download")}`}
      data-reveal="download"
      id="download"
      aria-labelledby="download-heading"
    >
      <div className="section-rail"><span>05</span><span>GET THE APP</span></div>
      <div className="apk-download-head">
        <p className="section-kicker">Direct from the build machine</p>
        <h2 id="download-heading">Download <em>Dorja,</em><br />per phone size.</h2>
        <p className="apk-download-sub">
          Each build on this page is fetched live from the latest release on GitHub.
          Pick the APK that matches your phone — every link below is the exact file CI published, unmodified.
        </p>
        {status === "ready" && releasePublished && (
          <p className="apk-download-meta"><Hourglass size={14} /> Release published {releasePublished}</p>
        )}
      </div>

      {status === "loading" && (
        <p className="apk-download-status"><Hourglass size={15} /> Fetching the latest release…</p>
      )}

      {status === "error" && (
        <div className="apk-download-status apk-download-status--error">
          <AlertTriangle size={15} /> Could not reach the GitHub release right now.
          <a href={RELEASES_PAGE} target="_blank" rel="noopener noreferrer" className="inline-link">
            Open the releases page instead <ExternalLink size={13} />
          </a>
        </div>
      )}

      {status === "ready" && groups.length === 0 && (
        <div className="apk-download-status apk-download-status--error">
          <AlertTriangle size={15} /> No per-architecture APKs are on the latest release yet.
          <a href={RELEASES_PAGE} target="_blank" rel="noopener noreferrer" className="inline-link">
            Check the releases page <ExternalLink size={13} />
          </a>
        </div>
      )}

      {status === "ready" && groups.length > 0 && (
        <>
          {groups.map((group) => (
            <div className="apk-download-group" key={group.key}>
              <div className="apk-download-group-head">
                <span className="apk-download-badge">{group.badge}</span>
                <h3>{group.title}</h3>
                <p>{group.note}</p>
              </div>
              <div className="apk-download-grid">
                {group.files.map((file) => (
                  <a
                    className={`apk-card${file.recommended ? " apk-card--recommended" : ""}`}
                    key={file.name}
                    href={file.url}
                    download={file.name}
                  >
                    <div className="apk-card-top">
                      {file.buildType === "debug" ? <Wrench size={18} /> : <BadgeCheck size={18} />}
                      {file.recommended
                        ? <span className="apk-card-flag apk-card-flag--you"><Smartphone size={10} /> FOR YOUR DEVICE</span>
                        : file.abi === "arm64-v8a"
                          ? <span className="apk-card-flag">MOST COMMON</span>
                          : null}
                    </div>
                    <div className="apk-card-arch"><Cpu size={15} /> {file.abi}</div>
                    <span className="apk-card-name">{file.name}</span>
                    <span className="apk-card-abi-label">{file.abiLabel}</span>
                    <dl className="apk-card-facts">
                      <div><dt>Size</dt><dd>{file.sizeMb}</dd></div>
                      <div><dt>Published</dt><dd>{file.publishedAt ?? "—"}</dd></div>
                      {file.digest && (
                        <div className="apk-card-digest"><dt>SHA-256</dt><dd title={file.digest}>{file.digest.slice(0, 18)}…</dd></div>
                      )}
                    </dl>
                    <span className="apk-card-download"><Download size={15} /> Download APK</span>
                  </a>
                ))}
              </div>
            </div>
          ))}

          <div className="apk-install-guide">
            <div className="apk-install-guide-head"><ListOrdered size={18} /> Installing on your device</div>
            <ol className="apk-install-steps">
              {INSTALL_STEPS.map((step) => (
                <li key={step}><Check size={14} /> {step}</li>
              ))}
            </ol>
            <p className="apk-install-note">
              <Fingerprint size={14} /> The debug and release builds are signed with different keys —
              uninstall one before installing the other, and reinstall before grabbing a newer debug build.
            </p>
            <a className="apk-install-link" href={RELEASES_PAGE} target="_blank" rel="noopener noreferrer">
              View every build on GitHub <ArrowRight size={15} />
            </a>
          </div>
        </>
      )}
    </section>
  );
}
