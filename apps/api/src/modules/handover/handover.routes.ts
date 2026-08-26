import type { FastifyInstance } from 'fastify';
import { requireAuth } from '../../lib/auth.js';
import { handoverService } from './handover.service.js';

export async function handoverRoutes(app: FastifyInstance): Promise<void> {
  // ── Passport CRUD ───────────────────────────────────────────────

  // GET /v1/listings/:listingId/handover
  app.get('/v1/listings/:listingId/handover', { preHandler: [requireAuth] }, async (request, reply) => {
    const { listingId } = request.params as { listingId: string };
    const passport = await handoverService.getPassportForListing(listingId);
    return reply.send({ data: passport });
  });

  // GET /v1/handover/by-listing/:listingId (alias)
  app.get('/v1/handover/by-listing/:listingId', { preHandler: [requireAuth] }, async (request, reply) => {
    const { listingId } = request.params as { listingId: string };
    const passport = await handoverService.getPassportForListing(listingId);
    return reply.send({ data: passport });
  });

  // POST /v1/listings/:listingId/handover
  app.post('/v1/listings/:listingId/handover', { preHandler: [requireAuth] }, async (request, reply) => {
    const { listingId } = request.params as { listingId: string };
    const passport = await handoverService.createPassport(listingId, request.authUser!.id, request.body as any);
    return reply.send({ data: passport });
  });

  // PATCH /v1/handover/:passportId
  app.patch('/v1/handover/:passportId', { preHandler: [requireAuth] }, async (request, reply) => {
    const { passportId } = request.params as { passportId: string };
    const passport = await handoverService.updatePassport(passportId, request.authUser!.id, request.body as any);
    return reply.send({ data: passport });
  });

  // ── Promises ────────────────────────────────────────────────────

  // GET /v1/handover/:passportId/promises
  app.get('/v1/handover/:passportId/promises', { preHandler: [requireAuth] }, async (request, reply) => {
    const { passportId } = request.params as { passportId: string };
    const promises = await handoverService.getPromises(passportId);
    return reply.send({ data: promises });
  });

  // POST /v1/handover/:passportId/promises
  app.post('/v1/handover/:passportId/promises', { preHandler: [requireAuth] }, async (request, reply) => {
    const { passportId } = request.params as { passportId: string };
    const promise = await handoverService.createPromise(passportId, request.authUser!.id, request.body as any);
    return reply.send({ data: promise });
  });

  // POST /v1/handover/promises/:promiseId/acknowledge
  app.post('/v1/handover/promises/:promiseId/acknowledge', { preHandler: [requireAuth] }, async (request, reply) => {
    const { promiseId } = request.params as { promiseId: string };
    const promise = await handoverService.acknowledgePromise(promiseId, request.authUser!.id);
    return reply.send({ data: promise });
  });

  // POST /v1/handover/promises/:promiseId/contest
  app.post('/v1/handover/promises/:promiseId/contest', { preHandler: [requireAuth] }, async (request, reply) => {
    const { promiseId } = request.params as { promiseId: string };
    const { note } = request.body as { note: string };
    const promise = await handoverService.contestPromise(promiseId, request.authUser!.id, note || '');
    return reply.send({ data: promise });
  });

  // POST /v1/handover/promises/:promiseId/revisions
  app.post('/v1/handover/promises/:promiseId/revisions', { preHandler: [requireAuth] }, async (request, reply) => {
    const { promiseId } = request.params as { promiseId: string };
    const revision = await handoverService.proposeRevision(promiseId, request.authUser!.id, request.body as any);
    return reply.send({ data: revision });
  });

  // ── Evidence ────────────────────────────────────────────────────

  // GET /v1/handover/:passportId/evidence
  app.get('/v1/handover/:passportId/evidence', { preHandler: [requireAuth] }, async (request, reply) => {
    const { passportId } = request.params as { passportId: string };
    const evidence = await handoverService.getEvidence(passportId);
    return reply.send({ data: evidence });
  });

  // POST /v1/handover/:passportId/evidence
  app.post('/v1/handover/:passportId/evidence', { preHandler: [requireAuth] }, async (request, reply) => {
    const { passportId } = request.params as { passportId: string };
    const evidence = await handoverService.addEvidence(passportId, request.authUser!.id, request.body as any);
    return reply.send({ data: evidence });
  });

  // ── Remedies ────────────────────────────────────────────────────

  // GET /v1/handover/:passportId/remedies
  app.get('/v1/handover/:passportId/remedies', { preHandler: [requireAuth] }, async (request, reply) => {
    const { passportId } = request.params as { passportId: string };
    const remedies = await handoverService.getRemedies(passportId);
    return reply.send({ data: remedies });
  });

  // POST /v1/handover/:passportId/remedies
  app.post('/v1/handover/:passportId/remedies', { preHandler: [requireAuth] }, async (request, reply) => {
    const { passportId } = request.params as { passportId: string };
    const remedy = await handoverService.createRemedy(passportId, request.authUser!.id, request.body as any);
    return reply.send({ data: remedy });
  });

  // POST /v1/handover/remedies/:remedyId/propose-date
  app.post('/v1/handover/remedies/:remedyId/propose-date', { preHandler: [requireAuth] }, async (request, reply) => {
    const { remedyId } = request.params as { remedyId: string };
    const { date } = request.body as { date: string };
    const remedy = await handoverService.proposeRemedyDate(remedyId, request.authUser!.id, date);
    return reply.send({ data: remedy });
  });

  // POST /v1/handover/remedies/:remedyId/accept-date
  app.post('/v1/handover/remedies/:remedyId/accept-date', { preHandler: [requireAuth] }, async (request, reply) => {
    const { remedyId } = request.params as { remedyId: string };
    const remedy = await handoverService.acceptRemedyDate(remedyId, request.authUser!.id);
    return reply.send({ data: remedy });
  });

  // POST /v1/handover/remedies/:remedyId/mark-ready
  app.post('/v1/handover/remedies/:remedyId/mark-ready', { preHandler: [requireAuth] }, async (request, reply) => {
    const { remedyId } = request.params as { remedyId: string };
    const remedy = await handoverService.markRemedyReady(remedyId, request.authUser!.id);
    return reply.send({ data: remedy });
  });

  // POST /v1/handover/remedies/:remedyId/resolve
  app.post('/v1/handover/remedies/:remedyId/resolve', { preHandler: [requireAuth] }, async (request, reply) => {
    const { remedyId } = request.params as { remedyId: string };
    const remedy = await handoverService.resolveRemedy(remedyId, request.authUser!.id);
    return reply.send({ data: remedy });
  });

  // POST /v1/handover/remedies/:remedyId/contest
  app.post('/v1/handover/remedies/:remedyId/contest', { preHandler: [requireAuth] }, async (request, reply) => {
    const { remedyId } = request.params as { remedyId: string };
    const remedy = await handoverService.contestRemedy(remedyId, request.authUser!.id);
    return reply.send({ data: remedy });
  });

  // ── Timeline ────────────────────────────────────────────────────

  // GET /v1/handover/:passportId/timeline
  app.get('/v1/handover/:passportId/timeline', { preHandler: [requireAuth] }, async (request, reply) => {
    const { passportId } = request.params as { passportId: string };
    const timeline = await handoverService.getTimeline(passportId);
    return reply.send({ data: timeline });
  });

  // ── Evidence Pack ───────────────────────────────────────────────

  // GET /v1/handover/:passportId/evidence-pack
  app.get('/v1/handover/:passportId/evidence-pack', { preHandler: [requireAuth] }, async (request, reply) => {
    const { passportId } = request.params as { passportId: string };
    const pack = await handoverService.generateEvidencePack(passportId);
    return reply.send({ data: pack });
  });
}
