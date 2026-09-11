/** Public-folder URL that respects Vite's base (GitHub Pages is /dorja/). */
export function publicUrl(path: string): string {
  const base = import.meta.env.BASE_URL || "/";
  const clean = path.replace(/^\//, "");
  return `${base}${clean}`;
}

export const routerBase = (import.meta.env.BASE_URL || "/").replace(/\/$/, "") || undefined;
