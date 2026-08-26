import type { FastifyInstance } from 'fastify';
import { requireAuth, requireRole } from '../../lib/auth.js';
import { prisma } from '../../lib/prisma.js';
import { NotFoundError } from '../../lib/errors.js';

export async function identityRoutes(app: FastifyInstance): Promise<void> {
  app.post('/v1/identity/start', { preHandler: [requireAuth] }, async (request, reply) => {
    const userId = request.authUser!.id;
    const { consentVersion } = request.body as { consentVersion: string };
    const verification = await prisma.identityVerification.create({
      data: { userId, providerType: 'MANUAL_REVIEW', status: 'PENDING', consentVersion, consentedAt: new Date() },
    });
    console.log(`Identity verification started for user ${userId}`);
    return reply.code(201).send({ data: { verificationId: verification.id, status: 'PENDING' } });
  });

  app.get('/v1/identity/status', { preHandler: [requireAuth] }, async (request, reply) => {
    const verification = await prisma.identityVerification.findFirst({
      where: { userId: request.authUser!.id }, orderBy: { createdAt: 'desc' },
      select: { id: true, status: true, providerType: true, verifiedAt: true, expiresAt: true, createdAt: true },
    });
    return reply.send({ data: { identityStatus: request.authUser!.identityStatus, verification } });
  });

  app.post('/v1/reviewer/authority/:reviewId/approve', { preHandler: [requireRole('REVIEWER', 'ADMIN')] }, async (request, reply) => {
    const { reviewId } = request.params as { reviewId: string };
    const review = await prisma.authorityReview.findUnique({ where: { id: reviewId } });
    if (!review) throw new NotFoundError('Authority review');
    const updated = await prisma.authorityReview.update({ where: { id: reviewId }, data: { status: 'REVIEWED', reviewerId: request.authUser!.id, reviewedAt: new Date() } });
    await prisma.listing.update({ where: { id: review.listingId }, data: { authorityStatus: 'REVIEWED' } });
    return reply.send({ data: updated });
  });

  app.post('/v1/reviewer/authority/:reviewId/reject', { preHandler: [requireRole('REVIEWER', 'ADMIN')] }, async (request, reply) => {
    const { reviewId } = request.params as { reviewId: string };
    const review = await prisma.authorityReview.findUnique({ where: { id: reviewId } });
    if (!review) throw new NotFoundError('Authority review');
    const updated = await prisma.authorityReview.update({ where: { id: reviewId }, data: { status: 'REJECTED', reviewerId: request.authUser!.id, reviewedAt: new Date() } });
    return reply.send({ data: updated });
  });
}
