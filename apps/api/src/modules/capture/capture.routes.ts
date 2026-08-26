import type { FastifyInstance } from 'fastify';
import { requireAuth } from '../../lib/auth.js';
import { prisma } from '../../lib/prisma.js';
import { NotFoundError, ForbiddenError } from '../../lib/errors.js';
import { getCheckpointsForRoomType } from '@dorja/domain';
import { nanoid } from 'nanoid';

export async function captureRoutes(app: FastifyInstance): Promise<void> {
  // POST /v1/listings/:listingId/capture-sessions — create capture session
  app.post('/v1/listings/:listingId/capture-sessions', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const { listingId } = request.params as { listingId: string };
    const { routeVersion } = request.body as { routeVersion: number };

    const listing = await prisma.listing.findUnique({ where: { id: listingId } });
    if (!listing) throw new NotFoundError('Listing');
    if (listing.ownerId !== request.authUser!.id) throw new ForbiddenError('Only the assigned lister can create capture sessions.');
    if (listing.status === 'ARCHIVED' || listing.status === 'RESTRICTED') {
      throw new ForbiddenError('Cannot capture for archived or restricted listings.');
    }

    const session = await prisma.captureSession.create({
      data: {
        listingId,
        capturedByUserId: request.authUser!.id,
        routeVersion,
        status: 'IN_PROGRESS',
      },
    });

    // Create room progress for each room
    const rooms = await prisma.room.findMany({
      where: { listingId },
      orderBy: { ordinal: 'asc' },
    });

    for (const room of rooms) {
      const checkpoints = getCheckpointsForRoomType(room.roomType);
      await prisma.captureRoomProgress.create({
        data: {
          captureSessionId: session.id,
          roomId: room.id,
          ordinal: room.ordinal,
          requiredCheckpointCount: checkpoints.length,
          acceptedCheckpointCount: 0,
          status: 'NOT_STARTED',
        },
      });
    }

    return reply.code(201).send({ data: session });
  });

  // POST /v1/capture-sessions/:id/upload-url — get signed upload URL
  app.post('/v1/capture-sessions/:id/upload-url', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const { filename, mimeType: _mimeType } = request.body as { filename: string; mimeType: string };

    const session = await prisma.captureSession.findUnique({ where: { id } });
    if (!session) throw new NotFoundError('Capture session');
    if (session.capturedByUserId !== request.authUser!.id) throw new ForbiddenError();

    const storageKey = `capture/${session.listingId}/${session.id}/${nanoid()}-${filename}`;

    // In production, generate MinIO presigned URL
    // For demo, return the key for direct upload
    return reply.send({
      data: {
        uploadUrl: `http://localhost:9000/${process.env.MINIO_BUCKET || 'dorja-media'}/${storageKey}`,
        storageKey,
        expiresAt: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
      },
    });
  });

  // POST /v1/capture-sessions/:id/media — confirm uploaded asset
  app.post('/v1/capture-sessions/:id/media', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const { storageKey, sha256, mimeType, width, height } = request.body as {
      storageKey: string; sha256: string; mimeType: string; width?: number; height?: number;
    };

    const session = await prisma.captureSession.findUnique({ where: { id } });
    if (!session) throw new NotFoundError('Capture session');
    if (session.capturedByUserId !== request.authUser!.id) throw new ForbiddenError();

    const asset = await prisma.mediaAsset.create({
      data: {
        captureSessionId: id,
        storageKey,
        sha256,
        mimeType,
        width,
        height,
        qualityStatus: 'PENDING',
        sourceType: 'HOLD_TO_CAPTURE',
      },
    });

    return reply.code(201).send({ data: asset });
  });

  // POST /v1/capture-sessions/:id/submit — submit for processing
  app.post('/v1/capture-sessions/:id/submit', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const { id } = request.params as { id: string };

    const session = await prisma.captureSession.findUnique({ where: { id } });
    if (!session) throw new NotFoundError('Capture session');
    if (session.capturedByUserId !== request.authUser!.id) throw new ForbiddenError();

    await prisma.captureSession.update({
      where: { id },
      data: {
        status: 'READY_FOR_REVIEW',
        submittedAt: new Date(),
        captureTimestamp: new Date(),
      },
    });

    // Queue media processing job (BullMQ) — in demo, process synchronously
    return reply.send({ data: { status: 'READY_FOR_REVIEW' } });
  });

  // GET /v1/capture-sessions/:id — capture progress
  app.get('/v1/capture-sessions/:id', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const { id } = request.params as { id: string };

    const session = await prisma.captureSession.findUnique({
      where: { id },
      include: {
        captureRoomProgresses: { orderBy: { ordinal: 'asc' } },
        mediaAssets: true,
        rooms: true,
      },
    });
    if (!session) throw new NotFoundError('Capture session');

    return reply.send({ data: session });
  });

  // POST /v1/capture-sessions/:id/publish — publish reviewed capture
  app.post('/v1/capture-sessions/:id/publish', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const { id } = request.params as { id: string };

    const session = await prisma.captureSession.findUnique({ where: { id } });
    if (!session) throw new NotFoundError('Capture session');
    if (session.capturedByUserId !== request.authUser!.id) throw new ForbiddenError();

    // Update session to published
    await prisma.captureSession.update({
      where: { id },
      data: { status: 'PUBLISHED_SELLER_CAPTURED' },
    });

    // Create Reality Passport
    const acceptedCount = await prisma.captureRoomProgress.count({
      where: { captureSessionId: id, status: 'ACCEPTED' },
    });
    const totalRooms = await prisma.captureRoomProgress.count({
      where: { captureSessionId: id },
    });
    const coverageScore = totalRooms > 0 ? Math.round((acceptedCount / totalRooms) * 100) : 0;

    const passport = await prisma.realityPassport.create({
      data: {
        listingId: session.listingId,
        captureSessionId: id,
        reviewLevel: 'SELLER_CAPTURED',
        coverageScore,
        publishedAt: new Date(),
        publicStatus: 'PUBLISHED_SELLER_CAPTURED',
      },
    });

    // Create TourNodes for each room
    const rooms = await prisma.room.findMany({ where: { captureSessionId: id } });
    let x = 0;
    for (const room of rooms) {
      await prisma.tourNode.upsert({
        where: { roomId: room.id },
        create: {
          roomId: room.id,
          previewAssetId: `/media/preview-${room.id}.jpg`,
          mapX: x,
          mapY: 0,
        },
        update: {},
      });
      x += 2;
    }

    return reply.send({ data: passport });
  });
}
