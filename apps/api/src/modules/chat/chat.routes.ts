import { FastifyInstance } from 'fastify';
import { requireAuth } from '../../lib/auth.js';
import { chatService } from './chat.service.js';
import { prisma } from '../../lib/prisma.js';

export async function chatRoutes(app: FastifyInstance) {
  // List conversations for current user
  app.get('/v1/chat/conversations', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const userId = request.authUser!.id;
    const conversations = await chatService.listConversations(userId);
    return { data: conversations };
  });

  // Get or create a conversation for a listing
  app.post('/v1/chat/conversations', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const userId = request.authUser!.id;
    const { listingId, hostUserId } = request.body as any;
    if (!listingId || !hostUserId) {
      return reply.status(400).send({ error: { message: 'listingId and hostUserId required' } });
    }

    const conversation = await chatService.getOrCreateConversation(listingId, userId, hostUserId);
    return { data: conversation };
  });

  // Get messages in a conversation
  app.get('/v1/chat/conversations/:id/messages', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const userId = request.authUser!.id;
    const { id } = request.params as any;
    const { limit, cursor } = request.query as any;

    const messages = await chatService.getMessages(id, userId, limit ? parseInt(limit) : 50, cursor);
    return { data: messages };
  });

  // Send a message
  app.post('/v1/chat/conversations/:id/messages', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const userId = request.authUser!.id;
    const { id } = request.params as any;
    const { body, kind, relatedEntityType, relatedEntityId } = request.body as any;
    if (!body) {
      return reply.status(400).send({ error: { message: 'Message body required' } });
    }

    const message = await chatService.sendMessage(id, userId, body, kind, relatedEntityType, relatedEntityId);
    return { data: message };
  });

  // Create a room scan record
  app.post('/v1/scans', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const userId = request.authUser!.id;
    const { listingId, roomType, roomName, frameCount, coveragePercent, scanData, durationMs } = request.body as any;

    const scan = await prisma.roomScan.create({
      data: {
        listingId: listingId || null,
        userId,
        roomType: roomType || null,
        roomName: roomName || null,
        frameCount: frameCount || 0,
        coveragePercent: coveragePercent || 0,
        scanData: scanData || null,
        durationMs: durationMs || 0,
        status: 'COMPLETED',
      },
    });

    return { data: scan };
  });

  // List scans for a listing
  app.get('/v1/scans/:listingId', async (request, reply) => {
    const { listingId } = request.params as any;

    const scans = await prisma.roomScan.findMany({
      where: { listingId },
      orderBy: { createdAt: 'desc' },
    });

    return { data: scans };
  });
}
