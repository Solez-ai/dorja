import type { FastifyInstance } from 'fastify';
import { createListingSchema, updateListingSchema, listingQuerySchema } from '@dorja/contracts';
import { listingService } from './listing.service.js';
import { requireAuth } from '../../lib/auth.js';
import QRCode from 'qrcode';

export async function listingRoutes(app: FastifyInstance): Promise<void> {
  // GET /v1/listings — public discovery
  app.get('/v1/listings', async (request, reply) => {
    const query = listingQuerySchema.parse(request.query);
    const result = await listingService.getPublicListings({
      intent: query.intent,
      propertyType: query.propertyType,
      minPrice: query.minPrice,
      maxPrice: query.maxPrice,
      area: query.area,
      page: query.page,
      limit: query.limit,
    });
    return reply.send({
      data: result.listings,
      meta: { page: query.page, limit: query.limit, total: result.total },
    });
  });

  // GET /v1/listings/:slug — public Reality Passport
  app.get('/v1/listings/:slug', async (request, reply) => {
    const { slug } = request.params as { slug: string };
    const passport = await listingService.getPublicPassport(slug);
    return reply.send({ data: passport });
  });

  // POST /v1/listings — create draft
  app.post('/v1/listings', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const listing = await listingService.create(request.authUser!.id, request.body as any);
    return reply.send({ data: listing });
  });

  // POST /v1/listings/:listingId/live-pulse — reconfirm availability
  app.post('/v1/listings/:listingId/live-pulse', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const { listingId } = request.params as { listingId: string };
    const listing = await listingService.reconfirmPulse(listingId, request.authUser!.id);
    return reply.send({ data: { livePulseAt: listing.livePulseAt, livePulseExpiresAt: listing.livePulseExpiresAt } });
  });

  // POST /v1/listings/:listingId/mark-closed
  app.post('/v1/listings/:listingId/mark-closed', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const { listingId } = request.params as { listingId: string };
    const listing = await listingService.markClosed(listingId, request.authUser!.id);
    return reply.send({ data: { status: listing.status } });
  });

  // POST /v1/listings/:listingId/qr — generate QR
  app.post('/v1/listings/:listingId/qr', {
    preHandler: [requireAuth],
  }, async (request, reply) => {
    const { listingId } = request.params as { listingId: string };
    const listing = await listingService.getById(listingId);
    if (listing.ownerId !== request.authUser!.id) {
      return reply.code(403).send({ error: { code: 'FORBIDDEN', message: 'Not your listing.' } });
    }
    const webUrl = `${process.env.WEB_ORIGIN || 'http://localhost:3000'}/properties/${listing.slug}`;
    const qrSvg = await QRCode.toString(webUrl, { type: 'svg', width: 512, margin: 2 });
    return reply.send({ data: { svg: qrSvg, url: webUrl } });
  });
}
