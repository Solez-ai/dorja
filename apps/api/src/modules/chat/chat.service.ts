import { prisma } from '../../lib/prisma.js';

export class ChatService {
  /**
   * Get or create a conversation between two users for a listing
   */
  async getOrCreateConversation(
    listingId: string,
    seekerUserId: string,
    hostUserId: string,
  ) {
    // Try to find existing
    const existing = await prisma.conversation.findUnique({
      where: {
        listingId_seekerUserId_hostUserId: {
          listingId,
          seekerUserId,
          hostUserId,
        },
      },
      include: { messages: { orderBy: { createdAt: 'desc' }, take: 1 } },
    });
    if (existing) return existing;

    // Create new
    return prisma.conversation.create({
      data: {
        listingId,
        seekerUserId,
        hostUserId,
        status: 'ACTIVE',
      },
      include: { messages: { orderBy: { createdAt: 'desc' }, take: 1 } },
    });
  }

  /**
   * List conversations for a user
   */
  async listConversations(userId: string) {
    return prisma.conversation.findMany({
      where: {
        OR: [{ seekerUserId: userId }, { hostUserId: userId }],
        status: 'ACTIVE',
      },
      include: {
        listing: { select: { id: true, title: true, slug: true, publicArea: true } },
        seeker: { select: { id: true, displayName: true, primaryRole: true } },
        host: { select: { id: true, displayName: true, primaryRole: true } },
        messages: {
          orderBy: { createdAt: 'desc' },
          take: 1,
        },
      },
      orderBy: { lastMessageAt: 'desc' },
    });
  }

  /**
   * Get messages in a conversation
   */
  async getMessages(conversationId: string, userId: string, limit = 50, cursor?: string) {
    const where: any = { conversationId };
    if (cursor) {
      where.createdAt = { lt: new Date(cursor) };
    }

    const messages = await prisma.message.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      take: limit,
      include: {
        sender: { select: { id: true, displayName: true, primaryRole: true } },
      },
    });

    // Mark as read
    await prisma.conversation.update({
      where: { id: conversationId },
      data: { lastMessageAt: new Date() },
    }).catch(() => {});

    return messages.reverse();
  }

  /**
   * Send a message
   */
  async sendMessage(
    conversationId: string,
    senderUserId: string,
    body: string,
    kind: string = 'TEXT',
    relatedEntityType?: string,
    relatedEntityId?: string,
  ) {
    const message = await prisma.message.create({
      data: {
        conversationId,
        senderUserId,
        kind: kind as any,
        bodyEncrypted: Buffer.from(body, 'utf-8'),
        safePreview: body.substring(0, 100),
        relatedEntityType,
        relatedEntityId,
      },
      include: {
        sender: { select: { id: true, displayName: true, primaryRole: true } },
      },
    });

    // Update conversation timestamp
    await prisma.conversation.update({
      where: { id: conversationId },
      data: { lastMessageAt: new Date() },
    }).catch(() => {});

    return message;
  }
}

export const chatService = new ChatService();
