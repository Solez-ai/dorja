import type { FastifyRequest, FastifyReply } from 'fastify';
import crypto from 'node:crypto';
import type { UserRole, IdentityStatus } from '@prisma/client';

export type AuthenticatedUser = {
  id: string;
  phoneHash: string;
  phoneLast4: string;
  displayName: string;
  primaryRole: UserRole;
  identityStatus: IdentityStatus;
};

// Augment FastifyRequest with a custom authenticated user field
 declare module 'fastify' {
  interface FastifyRequest {
    authUser?: AuthenticatedUser;
  }
}

export function generateOtp(): string {
  return crypto.randomInt(100000, 999999).toString();
}

export function hashPhone(phone: string): string {
  return crypto.createHash('sha256').update(phone).digest('hex');
}

export function hashOtp(otp: string): string {
  return crypto.createHmac('sha256', process.env.JWT_ACCESS_SECRET || 'dev').update(otp).digest('hex');
}

export function hashToken(token: string): string {
  return crypto.createHash('sha256').update(token).digest('hex');
}

export function generateRefreshToken(): string {
  return crypto.randomBytes(32).toString('hex');
}

export async function requireAuth(request: FastifyRequest, reply: FastifyReply): Promise<void> {
  try {
    const decoded = await request.jwtVerify<{ sub: string }>();
    if (!decoded || !decoded.sub) {
      return reply.code(401).send({ error: { code: 'UNAUTHORIZED', message: 'Invalid token.' } });
    }

    const { prisma } = await import('./prisma.js');
    const user = await prisma.user.findUnique({ where: { id: decoded.sub } });
    if (!user) {
      return reply.code(401).send({ error: { code: 'UNAUTHORIZED', message: 'User not found.' } });
    }

    request.authUser = {
      id: user.id,
      phoneHash: user.phoneHash,
      phoneLast4: user.phoneLast4,
      displayName: user.displayName,
      primaryRole: user.primaryRole,
      identityStatus: user.identityStatus,
    };
  } catch {
    return reply.code(401).send({ error: { code: 'UNAUTHORIZED', message: 'Authentication required.' } });
  }
}

export function requireRole(...roles: UserRole[]) {
  return async (request: FastifyRequest, reply: FastifyReply): Promise<void> => {
    await requireAuth(request, reply);
    if (reply.sent) return;
    if (!request.authUser || !roles.includes(request.authUser.primaryRole)) {
      return reply.code(403).send({
        error: { code: 'FORBIDDEN', message: 'Insufficient role permissions.' },
      });
    }
  };
}

export function requireIdentityConfirmed() {
  return async (request: FastifyRequest, reply: FastifyReply): Promise<void> => {
    await requireAuth(request, reply);
    if (reply.sent) return;
    if (!request.authUser || request.authUser.identityStatus !== 'IDENTITY_CONFIRMED') {
      return reply.code(403).send({
        error: { code: 'VIEWING_IDENTITY_REQUIRED', message: 'Identity confirmation is required before requesting a private viewing.' },
      });
    }
  };
}
