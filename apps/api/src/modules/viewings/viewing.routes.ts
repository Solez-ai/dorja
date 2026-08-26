import type { FastifyInstance } from 'fastify';
import { requireAuth, requireIdentityConfirmed } from '../../lib/auth.js';
import { prisma } from '../../lib/prisma.js';
import { NotFoundError, ForbiddenError, ConflictError, SlotCapacityError, PassInvalidError } from '../../lib/errors.js';
import { hashToken } from '../../lib/auth.js';
import crypto from 'node:crypto';
import { getEncryptionService } from '../../lib/encryption.js';

const REVEAL_WINDOW_MINUTES = 30;
const PASS_EXPIRY_MINUTES_AFTER_END = 15;

export async function viewingRoutes(app: FastifyInstance): Promise<void> {
  app.get('/v1/listings/:listingId/slots', { preHandler: [requireAuth] }, async (request, reply) => {
    const { listingId } = request.params as { listingId: string };
    const slots = await prisma.availabilitySlot.findMany({ where: { listingId, isActive: true, startsAt: { gt: new Date() } }, orderBy: { startsAt: 'asc' } });
    return reply.send({ data: slots });
  });

  app.post('/v1/listings/:listingId/viewings', { preHandler: [requireIdentityConfirmed()] }, async (request, reply) => {
    const { listingId } = request.params as { listingId: string };
    const userId = request.authUser!.id;
    const { preferredSlotId, attendeeCount, companionName } = request.body as any;
    const viewing = await prisma.$transaction(async (tx) => {
      const listing = await tx.listing.findUnique({ where: { id: listingId } });
      if (!listing) throw new NotFoundError('Listing');
      if (!['ACTIVE', 'VIEWING_HELD'].includes(listing.status)) throw new ConflictError('Not available');
      if (listing.authorityStatus !== 'REVIEWED') throw new ConflictError('Authority not reviewed');
      const slot = await tx.availabilitySlot.findUnique({ where: { id: preferredSlotId } });
      if (!slot) throw new NotFoundError('Time slot');
      if (slot.confirmedCount >= slot.capacity) throw new SlotCapacityError();
      return tx.viewing.create({ data: { listingId, seekerId: userId, hostId: listing.ownerId, status: 'REQUESTED', attendeeCount, companionName } });
    });
    return reply.code(201).send({ data: viewing });
  });

  app.post('/v1/viewings/:id/confirm', { preHandler: [requireAuth] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const userId = request.authUser!.id;
    const result = await prisma.$transaction(async (tx) => {
      const viewing = await tx.viewing.findUnique({ where: { id } });
      if (!viewing) throw new NotFoundError('Viewing');
      if (viewing.hostId !== userId) throw new ForbiddenError();
      const startsAt = new Date(Date.now() + 3600000);
      const endsAt = new Date(startsAt.getTime() + 1800000);
      const addressRevealAt = new Date(startsAt.getTime() - REVEAL_WINDOW_MINUTES * 60000);
      const passExpiresAt = new Date(endsAt.getTime() + PASS_EXPIRY_MINUTES_AFTER_END * 60000);
      const rawToken = crypto.randomBytes(32).toString('hex');
      const tokenHash = hashToken(rawToken);
      await tx.viewingPass.create({ data: { viewingId: id, tokenHash, status: 'ISSUED', expiresAt: passExpiresAt } });
      const updated = await tx.viewing.update({ where: { id }, data: { status: 'CONFIRMED', startsAt, endsAt, addressRevealAt }, include: { pass: true } });
      return { viewing: updated, passToken: rawToken };
    });
    return reply.send({ data: { ...result.viewing, passToken: result.passToken } });
  });

  app.get('/v1/viewings/:id/pass', { preHandler: [requireAuth] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const userId = request.authUser!.id;
    const viewing = await prisma.viewing.findUnique({ where: { id }, include: { pass: true } });
    if (!viewing) throw new NotFoundError('Viewing');
    if (viewing.seekerId !== userId && viewing.hostId !== userId) throw new ForbiddenError();
    if (!viewing.pass) throw new NotFoundError('Pass');
    if (viewing.pass.status !== 'ISSUED' && viewing.pass.status !== 'VIEWED') throw new PassInvalidError();
    if (viewing.pass.status === 'ISSUED') await prisma.viewingPass.update({ where: { id: viewing.pass.id }, data: { status: 'VIEWED', viewedAt: new Date() } });
    return reply.send({ data: { viewingId: viewing.id, expiresAt: viewing.pass.expiresAt.toISOString() } });
  });

  app.post('/v1/viewings/:id/check-in', { preHandler: [requireAuth] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const { token } = request.body as { token: string };
    const userId = request.authUser!.id;
    const viewing = await prisma.viewing.findUnique({ where: { id }, include: { pass: true } });
    if (!viewing || !viewing.pass) throw new NotFoundError('Viewing/Pass');
    if (hashToken(token) !== viewing.pass.tokenHash) throw new PassInvalidError('Invalid token');
    if (viewing.pass.expiresAt < new Date()) throw new PassInvalidError('Expired');
    await prisma.viewingPass.update({ where: { id: viewing.pass.id }, data: { status: 'CHECKED_IN', checkedInAt: new Date(), scanCount: { increment: 1 }, lastScannedByUserId: userId } });
    await prisma.viewing.update({ where: { id }, data: { status: 'CHECKED_IN' } });
    return reply.send({ data: { checkedIn: true } });
  });

  app.post('/v1/viewings/:id/check-out', { preHandler: [requireAuth] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const userId = request.authUser!.id;
    const viewing = await prisma.viewing.findUnique({ where: { id } });
    if (!viewing) throw new NotFoundError('Viewing');
    const isSeeker = viewing.seekerId === userId;
    const updated = await prisma.viewing.update({ where: { id }, data: isSeeker ? { seekerCheckedOutAt: new Date() } : { hostCheckedOutAt: new Date() } });
    if ((isSeeker || !!updated.seekerCheckedOutAt) && (!isSeeker || !!updated.hostCheckedOutAt)) await prisma.viewing.update({ where: { id }, data: { status: 'COMPLETED' } });
    return reply.send({ data: { checkedOut: true } });
  });

  app.post('/v1/viewings/:id/cancel', { preHandler: [requireAuth] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const userId = request.authUser!.id;
    await prisma.$transaction(async (tx) => {
      const v = await tx.viewing.findUnique({ where: { id }, include: { pass: true } });
      if (!v) throw new NotFoundError('Viewing');
      if (v.pass) await tx.viewingPass.update({ where: { id: v.pass.id }, data: { status: 'INVALIDATED', invalidatedAt: new Date() } });
      await tx.viewing.update({ where: { id }, data: { status: 'CANCELLED' } });
      await tx.safetyEvent.create({ data: { viewingId: id, actorUserId: userId, eventType: 'APPOINTMENT_CANCELLED' } });
    });
    return reply.send({ data: { cancelled: true } });
  });

  app.post('/v1/viewings/:id/safety-events', { preHandler: [requireAuth] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const { category, description } = request.body as { category: string; description: string };
    const userId = request.authUser!.id;
    const viewing = await prisma.viewing.findUnique({ where: { id } });
    if (!viewing) throw new NotFoundError('Viewing');
    const enc = getEncryptionService();
    const descEnc = enc.encryptString(description, 'safety_report', id, 'description');
    const report = await prisma.safetyReport.create({ data: { viewingId: id, listingId: viewing.listingId, reporterUserId: userId, reportedUserId: viewing.seekerId === userId ? viewing.hostId : viewing.seekerId, category: category as any, descriptionEnc: Buffer.from(JSON.stringify(enc.serialize(descEnc))) } });
    return reply.code(201).send({ data: { reportId: report.id, status: 'OPEN' } });
  });
}
