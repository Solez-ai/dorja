import type { FastifyInstance } from 'fastify';
import { requireAuth } from '../../lib/auth.js';
import { prisma } from '../../lib/prisma.js';
import { NotFoundError, ForbiddenError, ConflictError } from '../../lib/errors.js';
import { canTransitionOffer } from '@dorja/domain';
import { getEncryptionService } from '../../lib/encryption.js';

function encNote(note: string, sub: string, field: string) {
  const e = getEncryptionService();
  return Buffer.from(JSON.stringify(e.serialize(e.encryptString(note, sub, 'id', field))));
}

export async function offerRoutes(app: FastifyInstance): Promise<void> {
  app.post('/v1/listings/:listingId/offers', { preHandler: [requireAuth] }, async (request, reply) => {
    const { listingId } = request.params as { listingId: string };
    const userId = request.authUser!.id;
    const { conversationId, terms, note } = request.body as any;
    const conversation = await prisma.conversation.findUnique({ where: { id: conversationId } });
    if (!conversation) throw new NotFoundError('Conversation');
    const recipientId = conversation.seekerUserId === userId ? conversation.hostUserId : conversation.seekerUserId;
    const offer = await prisma.offer.create({
      data: {
        listingId, conversationId, senderUserId: userId, recipientUserId: recipientId, status: 'SENT',
        expiresAt: terms.expiresAt ? new Date(terms.expiresAt) : undefined,
        versions: { create: { version: 1, createdByUserId: userId, termsJson: terms, noteEncrypted: note ? encNote(note, 'offer', 'note') : undefined } }
      },
      include: { versions: true }
    });
    return reply.code(201).send({ data: offer });
  });

  app.get('/v1/offers/:id', { preHandler: [requireAuth] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const offer = await prisma.offer.findUnique({ where: { id }, include: { versions: { orderBy: { version: 'desc' } } } });
    if (!offer) throw new NotFoundError('Offer');
    return reply.send({ data: offer });
  });

  app.post('/v1/offers/:id/counter', { preHandler: [requireAuth] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const userId = request.authUser!.id;
    const { terms, note } = request.body as any;
    const offer = await prisma.offer.findUnique({ where: { id } });
    if (!offer) throw new NotFoundError('Offer');
    if (offer.recipientUserId !== userId) throw new ForbiddenError();
    const result = canTransitionOffer(offer.status, 'counter', 'recipient');
    if (!result.allowed) throw new ConflictError('Cannot counter in ' + offer.status);
    const updated = await prisma.offer.update({
      where: { id },
      data: { status: 'COUNTERED', currentVersion: offer.currentVersion + 1, versions: { create: { version: offer.currentVersion + 1, createdByUserId: userId, termsJson: terms, noteEncrypted: note ? encNote(note, 'offer', 'note') : undefined } } },
      include: { versions: true }
    });
    return reply.send({ data: updated });
  });

  app.post('/v1/offers/:id/accept', { preHandler: [requireAuth] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const userId = request.authUser!.id;
    const offer = await prisma.offer.findUnique({ where: { id } });
    if (!offer) throw new NotFoundError('Offer');
    if (offer.recipientUserId !== userId) throw new ForbiddenError();
    const updated = await prisma.offer.update({ where: { id }, data: { status: 'ACCEPTED', acceptedAt: new Date() } });
    return reply.send({ data: updated });
  });

  app.post('/v1/offers/:id/decline', { preHandler: [requireAuth] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const userId = request.authUser!.id;
    const offer = await prisma.offer.findUnique({ where: { id } });
    if (!offer) throw new NotFoundError('Offer');
    if (offer.recipientUserId !== userId) throw new ForbiddenError();
    const updated = await prisma.offer.update({ where: { id }, data: { status: 'DECLINED', declinedAt: new Date() } });
    return reply.send({ data: updated });
  });

  app.post('/v1/offers/:id/withdraw', { preHandler: [requireAuth] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const userId = request.authUser!.id;
    const offer = await prisma.offer.findUnique({ where: { id } });
    if (!offer) throw new NotFoundError('Offer');
    if (offer.senderUserId !== userId) throw new ForbiddenError();
    const updated = await prisma.offer.update({ where: { id }, data: { status: 'WITHDRAWN', withdrawnAt: new Date() } });
    return reply.send({ data: updated });
  });
}
