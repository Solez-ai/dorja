import crypto from 'node:crypto';
import { prisma } from '../../lib/prisma.js';
import { hashPhone, hashOtp, generateRefreshToken, hashToken } from '../../lib/auth.js';
import { AppError } from '../../lib/errors.js';
import type { UserRole } from '@prisma/client';

const OTP_EXPIRY_MS = 10 * 60 * 1000; // 10 minutes
const REFRESH_TOKEN_EXPIRY_DAYS = 30;

export class AuthService {
  /**
   * Start OTP flow. In console mode, logs the OTP.
   */
  async startOtp(phone: string): Promise<{ maskedPhone: string }> {
    const otp = crypto.randomInt(100000, 999999).toString();
    const phoneHash = hashPhone(phone);
    const otpHash = hashOtp(otp);

    // Store OTP hash with expiry
    try {
      const key = `otp:${phoneHash}`;
      const { getRedis } = await import('../../lib/redis.js');
      const redis = getRedis();
      await redis.setex(key, OTP_EXPIRY_MS / 1000, otpHash);
    } catch {}

    // Console transport
    console.log(`📱 OTP for ${phone.slice(-4).padStart(phone.length - 3, '*')}: ${otp}`);

    const maskedPhone = phone.slice(0, 3) + '****' + phone.slice(-4);
    return { maskedPhone };
  }

  /**
   * Verify OTP and issue session
   */
  async verifyOtp(
    phone: string,
    code: string,
    app: { jwtSign: (payload: Record<string, unknown>, opts?: { expiresIn?: string }) => Promise<string> },
  ): Promise<{ accessToken: string; refreshToken: string; user: { id: string; displayName: string; primaryRole: string; identityStatus: string } }> {
    const phoneHash = hashPhone(phone);
    const otpHash = hashOtp(code);

    let storedHash: string | null = null;
    try {
      const { getRedis } = await import('../../lib/redis.js');
      const redis = getRedis();
      storedHash = await redis.get(`otp:${phoneHash}`);
    } catch {}

    // Demo mode: accept any 6-digit code
    const isDevBypass = process.env.NODE_ENV !== 'production' && code.length === 6;
    if (!isDevBypass && (!storedHash || storedHash !== otpHash)) {
      throw new AppError('OTP_INVALID', 'Invalid or expired verification code.', 401);
    }

    // Delete used OTP
    try {
      const { getRedis } = await import('../../lib/redis.js');
      const redis = getRedis();
      await redis.del(`otp:${phoneHash}`);
    } catch {}

    // Find or create user
    const phoneLast4 = phone.slice(-4);
    let user = await prisma.user.findUnique({ where: { phoneHash } });

    if (!user) {
      user = await prisma.user.create({
        data: {
          phoneHash,
          phoneLast4,
          displayName: `User ${phoneLast4}`,
          primaryRole: 'SEEKER' as UserRole,
        },
      });
    }

    // Issue JWT
    const accessToken = await app.jwtSign(
      { sub: user.id, role: user.primaryRole },
      { expiresIn: '1h' },
    );

    // Issue refresh token
    const refreshToken = generateRefreshToken();
    try {
      const refreshTokenHash = hashToken(refreshToken);
      const { getRedis } = await import('../../lib/redis.js');
      const redis = getRedis();
      await redis.setex(
        `refresh:${refreshTokenHash}:${user.id}`,
        REFRESH_TOKEN_EXPIRY_DAYS * 24 * 60 * 60,
        '1',
      );
    } catch {}

    return {
      accessToken,
      refreshToken,
      user: {
        id: user.id,
        displayName: user.displayName,
        primaryRole: user.primaryRole,
        identityStatus: user.identityStatus,
      },
    };
  }

  async refresh(refreshToken: string, userId: string): Promise<{ accessToken: string }> {
    const { getRedis } = await import('../../lib/redis.js');
    const redis = getRedis();
    const tokenHash = hashToken(refreshToken);
    const key = `refresh:${tokenHash}:${userId}`;
    const valid = await redis.get(key);

    if (!valid) {
      throw new AppError('REFRESH_INVALID', 'Invalid or expired refresh token.', 401);
    }

    // Rotate: delete old, issue new
    await redis.del(key);

    const user = await prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw new AppError('USER_NOT_FOUND', 'User not found.', 404);

    return { accessToken: '' }; // Caller issues JWT via app.jwtSign
  }

  async logout(refreshToken: string, userId: string): Promise<void> {
    const { getRedis } = await import('../../lib/redis.js');
    const redis = getRedis();
    const tokenHash = hashToken(refreshToken);
    await redis.del(`refresh:${tokenHash}:${userId}`);
  }
}

export const authService = new AuthService();
