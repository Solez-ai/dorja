import type { FastifyInstance } from 'fastify';
import { requireAuth } from '../../lib/auth.js';
import { prisma } from '../../lib/prisma.js';
import { NotFoundError, ForbiddenError, ConflictError } from '../../lib/errors.js';
import { getEncryptionService } from '../../lib/encryption.js';
import crypto from 'node:crypto';

export async function messageRoutes(app: FastifyInstance): Promise<void> {
  app.post('/v1/listings/:listingId/conversations', { preHandler: [requireAuth] }, async (request, reply) => {
    const { listingId } = request.params as { listingId: string };
    const userId = request.authUser!.id;
    const listing = await prisma.listing.findUnique({ where: { id: listingId } });
    if (!listing) throw new NotFoundError('Listing');
    if (listing.ownerId === userId) throw new ConflictError('Cannot message yourself.');
    const conversation = await prisma.conversation.upsert({ where: { listingId_seekerUserId_hostUserId: { listingId, seekerUserId: userId, hostUserId: listing.ownerId } }, create: { listingId, seekerUserId: userId, hostUserId: listing.ownerId }, update: {} });
    return reply.code(201).send({ data: conversation });
  });

  app.get('/v1/conversations', { preHandler: [requireAuth] }, async (request, reply) => {
    const userId = request.authUser!.id;
    const conversations = await prisma.conversation.findMany({ where: { OR: [{ seekerUserId: userId }, { hostUserId: userId }], status: 'ACTIVE' }, include: { listing: { select: { id: true, title: true, publicArea: true, slug: true } }, messages: { take: 1, orderBy: { createdAt: 'desc' } } }, orderBy: { lastMessageAt: 'desc' } });
    return reply.send({ data: conversations });
  });

  app.get('/v1/conversations/:id/messages', { preHandler: [requireAuth] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const userId = request.authUser!.id;
    const conversation = await prisma.conversation.findUnique({ where: { id } });
    if (!conversation) throw new NotFoundError('Conversation');
    if (conversation.seekerUserId !== userId && conversation.hostUserId !== userId) throw new ForbiddenError();
    const messages = await prisma.message.findMany({ where: { conversationId: id, deletedAt: null }, orderBy: { createdAt: 'asc' }, take: 50 });
    const encryption = getEncryptionService();
    const decrypted = messages.map((m) => ({ ...m, body: m.bodyEncrypted ? encryption.decryptString(Buffer.from(m.bodyEncrypted), 'message', m.id, 'body') : m.safePreview }));
    return reply.send({ data: decrypted });
  });

  app.post('/v1/conversations/:id/messages', { preHandler: [requireAuth] }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const userId = request.authUser!.id;
    const { kind, body, relatedEntityType, relatedEntityId } = request.body as any;
    const conversation = await prisma.conversation.findUnique({ where: { id } });
    if (!conversation) throw new NotFoundError('Conversation');
    let bodyEncrypted = null;
    let safePreview = null;
    if (body && kind === 'TEXT') {
      const tempId = crypto.randomUUID();
      bodyEncrypted = Buffer.from(JSON.stringify(getEncryptionService().serialize(getEncryptionService().encryptString(body, 'message', tempId, 'body'))));
      safePreview = body.slice(0, 80) + (body.length > 80 ? '...' : '');
    }
    const message = await prisma.message.create({ data: { id: crypto.randomUUID(), conversationId: id, senderUserId: userId, kind: kind || 'TEXT', bodyEncrypted: bodyEncrypted || undefined, safePreview, relatedEntityType, relatedEntityId } });
    await prisma.conversation.update({ where: { id }, data: { lastMessageAt: new Date() } });
    return reply.code(201).send({ data: message });
  });
}
