import { z } from 'zod';
import { readFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';

// Simple .env parser that DOES override existing env vars
function loadDotEnv(filePath: string) {
  if (!existsSync(filePath)) return;
  const content = readFileSync(filePath, 'utf-8');
  for (const line of content.split('\n')) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eqIdx = trimmed.indexOf('=');
    if (eqIdx === -1) continue;
    const key = trimmed.slice(0, eqIdx).trim();
    let value = trimmed.slice(eqIdx + 1).trim();
    // Remove surrounding quotes
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    process.env[key] = value;
  }
}

// Load .env from api directory first, then root
const apiDir = import.meta.dirname || process.cwd();
loadDotEnv(resolve(apiDir, '.env'));
loadDotEnv(resolve(apiDir, '..', '.env'));

const envSchema = z.object({
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  PORT: z.coerce.number().int().min(0).default(4000),
  DATABASE_URL: z.string().min(1),
  REDIS_URL: z.string().min(1),
  MINIO_ENDPOINT: z.string().default('localhost'),
  MINIO_PORT: z.coerce.number().int().default(9000),
  MINIO_ACCESS_KEY: z.string().min(1),
  MINIO_SECRET_KEY: z.string().min(1),
  MINIO_BUCKET: z.string().default('dorja-media'),
  JWT_ACCESS_SECRET: z.string().min(16),
  JWT_REFRESH_SECRET: z.string().min(16),
  WEB_ORIGIN: z.string().default('http://localhost:3000'),
  MAPS_MODE: z.enum(['deep-link', 'sdk']).default('deep-link'),
  IDENTITY_PROVIDER: z.enum(['manual-review', 'authorised-nid', 'disabled']).default('manual-review'),
  SMS_PROVIDER: z.enum(['console', 'twilio', 'disabled']).default('console'),
  SAFETY_ALERT_PROVIDER: z.enum(['console', 'sms', 'disabled']).default('console'),
  FIELD_ENCRYPTION_ROOT_KEY_BASE64: z.string().min(1),
  FIELD_ENCRYPTION_KEY_VERSION: z.coerce.number().int().min(1).default(1),
  AUDIT_LOG_HMAC_KEY_BASE64: z.string().min(1),
});

function loadConfig() {
  // Fix PORT=0 issue on Windows
  if (process.env.PORT === '0') delete process.env.PORT;

  const parsed = envSchema.safeParse(process.env);
  if (!parsed.success) {
    console.error('❌ Invalid environment variables:');
    console.error(parsed.error.flatten().fieldErrors);
    process.exit(1);
  }
  return parsed.data;
}

export const config = loadConfig();
export type Config = z.infer<typeof envSchema>;
