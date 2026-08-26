import type { FastifyInstance } from 'fastify';
import { phoneStartSchema, otpVerifySchema, refreshSchema } from '@dorja/contracts';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { authService } from './auth.service.js';
import { requireAuth } from '../../lib/auth.js';
import { prisma } from '../../lib/prisma.js';
import { AppError } from '../../lib/errors.js';

export async function authRoutes(app: FastifyInstance): Promise<void> {

  // POST /v1/auth/login — username/password login (demo)
  // seller / 12345678 = Seller account
  // buyer / 12345678 = Buyer account
  app.post('/v1/auth/login', {}, async (request, reply) => {
    const { username, password } = request.body as { username: string; password: string };
    if (!username || !password) {
      return reply.status(400).send({ error: { message: 'Username and password required' } });
    }
    if (password !== '12345678') {
      return reply.status(401).send({ error: { message: 'Invalid password' } });
    }
    const role = username.toLowerCase() === 'seller' ? 'OWNER' : 'SEEKER';
    const phone = role === 'OWNER' ? '+8801700000002' : '+8801700000001';
    const displayName = role === 'OWNER' ? 'Demo Seller' : 'Demo Buyer';

    // Find or create user
    const { hashPhone } = await import('../../lib/auth.js');
    const phoneHashed = hashPhone(phone);
    let user = await prisma.user.findUnique({ where: { phoneHash: phoneHashed } });
    if (!user) {
      user = await prisma.user.create({
        data: {
          phoneHash: phoneHashed,
          phoneLast4: phone.slice(-4),
          displayName,
          primaryRole: role as any,
          identityStatus: 'IDENTITY_CONFIRMED',
        },
      });
    }

    const accessToken = await app.jwt.sign({ sub: user.id }, { expiresIn: '24h' });
    return reply.send({
      data: {
        accessToken,
        user: {
          id: user.id,
          displayName: user.displayName,
          primaryRole: user.primaryRole,
          phone,
          username,
        },
      },
    });
  });

  // POST /v1/auth/otp/start — fake OTP for demo
  app.post('/v1/auth/otp/start', {
  }, async (request, reply) => {
    const { phone } = request.body as { phone: string };
    // In demo mode, always succeed — no real SMS
    return reply.send({ data: { message: 'OTP sent', expiresIn: 300, phone } });
  });

  // POST /v1/auth/otp/verify
  app.post('/v1/auth/otp/verify', {
  }, async (request, reply) => {
    const { phone, code } = request.body as { phone: string; code: string };
    const result = await authService.verifyOtp(phone, code, {
      jwtSign: async (payload, opts) => app.jwt.sign(payload, opts),
    });
    return reply.send({ data: result });
  });

  // POST /v1/auth/refresh
  app.post('/v1/auth/refresh', {
  }, async (request, reply) => {
    const { refreshToken } = request.body as { refreshToken: string };
    // Decode refresh token to get userId — in production, use a proper session store
    // For demo, we accept userId from a signed refresh token
    let decoded: { sub: string } | null = null;
    try { decoded = await app.jwt.verify<{ sub: string }>(refreshToken); } catch { /* invalid token */ }
    const userId = decoded?.sub;
    if (!userId) {
      throw new AppError('REFRESH_INVALID', 'Invalid refresh token.', 401);
    }
    const { getRedis } = await import('../../lib/redis.js');
    const { hashToken } = await import('../../lib/auth.js');
    const redis = getRedis();
    const tokenHash = hashToken(refreshToken);
    const valid = await redis.get(`refresh:${tokenHash}:${userId}`);
    if (!valid) {
      throw new AppError('REFRESH_INVALID', 'Invalid or expired refresh token.', 401);
    }
    await redis.del(`refresh:${tokenHash}:${userId}`);

    const accessToken = await app.jwt.sign({ sub: userId }, { expiresIn: '1h' });
    const newRefreshToken = await app.jwt.sign({ sub: userId }, { expiresIn: '30d' });
    const { hashToken: ht } = await import('../../lib/auth.js');
    const newRefreshHash = ht(newRefreshToken);
    await redis.setex(`refresh:${newRefreshHash}:${userId}`, 30 * 24 * 60 * 60, '1');

    return reply.send({ data: { accessToken, refreshToken: newRefreshToken } });
  });

  // POST /v1/auth/logout
  app.post('/v1/auth/logout', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const { refreshToken } = request.body as { refreshToken: string };
    if (refreshToken && request.authUser) {
      await authService.logout(refreshToken, request.authUser.id);
    }
    return reply.send({ data: { success: true } });
  });

  // GET /v1/me
  app.get('/v1/me', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const user = await prisma.user.findUnique({
      where: { id: request.authUser!.id },
      select: {
        id: true,
        displayName: true,
        avatarUrl: true,
        primaryRole: true,
        identityStatus: true,
        identityVerifiedAt: true,
        createdAt: true,
      },
    });
    return reply.send({ data: user });
  });
}
