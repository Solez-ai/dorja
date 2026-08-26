import { prisma } from '../../lib/prisma.js';
import { AppError } from '../../lib/errors.js';

export const handoverService = {
  // ── Passports ───────────────────────────────────────────────────
  async getPassportForListing(listingId: string) {
    return prisma.handoverPassport.findFirst({
      where: { listingId },
      include: {
        participants: true,
        promises: { orderBy: { createdAt: 'asc' } },
        remedies: { orderBy: { createdAt: 'desc' } },
        events: { orderBy: { createdAt: 'desc' }, take: 50 },
        _count: { select: { promises: true, remedies: true, evidence: true } },
      },
    });
  },

  async createPassport(listingId: string, userId: string, data: { agreementDate?: string }) {
    const existing = await prisma.handoverPassport.findFirst({ where: { listingId } });
    if (existing) throw new AppError('PASSPORT_EXISTS', 'A handover passport already exists for this listing.', 409);

    return prisma.handoverPassport.create({
      data: {
        listingId,
        status: 'PREPARING',
        agreementDate: data.agreementDate ? new Date(data.agreementDate) : null,
        latestActivityAt: new Date(),
        participants: { create: { userId, role: 'OWNER_REPRESENTATIVE' } },
        events: { create: { actorUserId: userId, eventType: 'PASSPORT_CREATED', entityType: 'PASSPORT' } },
      },
      include: { participants: true },
    });
  },

  async updatePassport(passportId: string, _userId: string, data: { status?: string }) {
    const passport = await prisma.handoverPassport.findUnique({ where: { id: passportId } });
    if (!passport) throw new AppError('NOT_FOUND', 'Passport not found.', 404);
    return prisma.handoverPassport.update({
      where: { id: passportId },
      data: { ...(data.status ? { status: data.status as any } : {}), latestActivityAt: new Date() },
    });
  },

  // ── Promises ────────────────────────────────────────────────────
  async getPromises(passportId: string) {
    return prisma.promise.findMany({
      where: { passportId },
      include: { revisions: { orderBy: { revisionNumber: 'desc' } }, evidence: true, remedies: true },
      orderBy: { createdAt: 'asc' },
    });
  },

  async createPromise(passportId: string, userId: string, data: {
    category: string; title: string; originalPromiseText: string;
    sourceReferenceLabel?: string; promisedDate?: string; promisedAmount?: number;
  }) {
    const promise = await prisma.promise.create({
      data: {
        passportId, category: data.category as any, title: data.title,
        originalPromiseText: data.originalPromiseText,
        sourceReferenceLabel: data.sourceReferenceLabel,
        promisedDate: data.promisedDate ? new Date(data.promisedDate) : null,
        promisedAmount: data.promisedAmount,
        originallyCreatedByUserId: userId,
        currentStatus: 'PENDING_ACKNOWLEDGEMENT',
        acknowledgementStatus: 'PENDING_ACKNOWLEDGEMENT',
      },
    });
    await prisma.handoverPassport.update({ where: { id: passportId }, data: { latestActivityAt: new Date(), promiseTotalCount: { increment: 1 } } });
    await prisma.handoverEvent.create({ data: { passportId, actorUserId: userId, eventType: 'PROMISE_CREATED', entityType: 'PROMISE', entityId: promise.id } });
    return promise;
  },

  async acknowledgePromise(promiseId: string, userId: string) {
    const promise = await prisma.promise.findUnique({ where: { id: promiseId } });
    if (!promise) throw new AppError('NOT_FOUND', 'Promise not found.', 404);
    const updated = await prisma.promise.update({ where: { id: promiseId }, data: { currentStatus: 'ACKNOWLEDGED', acknowledgementStatus: 'ACKNOWLEDGED', acknowledgedByUserId: userId, acknowledgedAt: new Date() } });
    await prisma.handoverEvent.create({ data: { passportId: promise.passportId, actorUserId: userId, eventType: 'PROMISE_ACKNOWLEDGED', entityType: 'PROMISE', entityId: promiseId } });
    return updated;
  },

  async contestPromise(promiseId: string, userId: string, note: string) {
    const promise = await prisma.promise.findUnique({ where: { id: promiseId } });
    if (!promise) throw new AppError('NOT_FOUND', 'Promise not found.', 404);
    const updated = await prisma.promise.update({ where: { id: promiseId }, data: { currentStatus: 'CONTESTED' } });
    await prisma.handoverEvent.create({ data: { passportId: promise.passportId, actorUserId: userId, eventType: 'PROMISE_CONTESTED', entityType: 'PROMISE', entityId: promiseId, publicMetadataJson: { note } } });
    return updated;
  },

  async proposeRevision(promiseId: string, userId: string, data: { proposedText?: string; proposedDate?: string; proposedAmount?: number; note?: string }) {
    const promise = await prisma.promise.findUnique({ where: { id: promiseId }, include: { revisions: true } });
    if (!promise) throw new AppError('NOT_FOUND', 'Promise not found.', 404);
    const nextRevision = (promise.revisions.length || 0) + 1;
    const revision = await prisma.promiseRevision.create({
      data: { promiseId, revisionNumber: nextRevision, proposedText: data.proposedText, proposedDate: data.proposedDate ? new Date(data.proposedDate) : null, proposedAmount: data.proposedAmount, proposerUserId: userId, note: data.note },
    });
    await prisma.promise.update({ where: { id: promiseId }, data: { currentStatus: 'CHANGE_PROPOSED' } });
    await prisma.handoverEvent.create({ data: { passportId: promise.passportId, actorUserId: userId, eventType: 'PROMISE_CHANGE_PROPOSED', entityType: 'PROMISE', entityId: promiseId } });
    return revision;
  },

  // ── Evidence ────────────────────────────────────────────────────
  async addEvidence(passportId: string, userId: string, data: { promiseId?: string; remedyId?: string; evidenceType: string; label: string; visibleNote?: string }) {
    const evidence = await prisma.handoverEvidence.create({
      data: { passportId, promiseId: data.promiseId || null, remedyId: data.remedyId || null, submitterUserId: userId, evidenceType: data.evidenceType as any, label: data.label, visibleNote: data.visibleNote },
    });
    await prisma.handoverEvent.create({ data: { passportId, actorUserId: userId, eventType: 'EVIDENCE_ADDED', entityType: 'EVIDENCE', entityId: evidence.id } });
    return evidence;
  },

  async getEvidence(passportId: string) {
    return prisma.handoverEvidence.findMany({ where: { passportId }, orderBy: { createdAt: 'desc' } });
  },

  // ── Remedies ────────────────────────────────────────────────────
  async getRemedies(passportId: string) {
    return prisma.remedyIssue.findMany({ where: { passportId }, include: { evidence: true }, orderBy: { createdAt: 'desc' } });
  },

  async createRemedy(passportId: string, userId: string, data: { title: string; description?: string; linkedPromiseId?: string; priority?: string }) {
    const remedy = await prisma.remedyIssue.create({
      data: { passportId, linkedPromiseId: data.linkedPromiseId || null, title: data.title, description: data.description, createdByUserId: userId, priority: (data.priority as any) || 'NORMAL', status: 'OPEN' },
    });
    await prisma.handoverEvent.create({ data: { passportId, actorUserId: userId, eventType: 'REMEDY_OPENED', entityType: 'REMEDY', entityId: remedy.id } });
    return remedy;
  },

  async proposeRemedyDate(remedyId: string, userId: string, date: string) {
    const updated = await prisma.remedyIssue.update({ where: { id: remedyId }, data: { proposedCompletionAt: new Date(date), status: 'REMEDY_PROPOSED', lastActorUserId: userId } });
    await prisma.handoverEvent.create({ data: { passportId: updated.passportId, actorUserId: userId, eventType: 'REMEDY_DATE_PROPOSED', entityType: 'REMEDY', entityId: remedyId, publicMetadataJson: { proposedDate: date } } });
    return updated;
  },

  async acceptRemedyDate(remedyId: string, userId: string) {
    const remedy = await prisma.remedyIssue.findUnique({ where: { id: remedyId } });
    if (!remedy) throw new AppError('NOT_FOUND', 'Remedy not found.', 404);
    const updated = await prisma.remedyIssue.update({ where: { id: remedyId }, data: { acceptedCompletionAt: remedy.proposedCompletionAt, status: 'IN_PROGRESS', lastActorUserId: userId } });
    await prisma.handoverEvent.create({ data: { passportId: remedy.passportId, actorUserId: userId, eventType: 'REMEDY_DATE_ACCEPTED', entityType: 'REMEDY', entityId: remedyId } });
    return updated;
  },

  async markRemedyReady(remedyId: string, userId: string) {
    const updated = await prisma.remedyIssue.update({ where: { id: remedyId }, data: { status: 'READY_FOR_REVIEW', lastActorUserId: userId } });
    await prisma.handoverEvent.create({ data: { passportId: updated.passportId, actorUserId: userId, eventType: 'REMEDY_READY_FOR_REVIEW', entityType: 'REMEDY', entityId: remedyId } });
    return updated;
  },

  async resolveRemedy(remedyId: string, userId: string) {
    const updated = await prisma.remedyIssue.update({ where: { id: remedyId }, data: { status: 'RESOLVED', closedAt: new Date(), lastActorUserId: userId } });
    const resolvedCount = await prisma.remedyIssue.count({ where: { passportId: updated.passportId, status: 'RESOLVED' } });
    await prisma.handoverPassport.update({ where: { id: updated.passportId }, data: { promiseResolvedCount: resolvedCount } });
    await prisma.handoverEvent.create({ data: { passportId: updated.passportId, actorUserId: userId, eventType: 'REMEDY_RESOLVED', entityType: 'REMEDY', entityId: remedyId } });
    return updated;
  },

  async contestRemedy(remedyId: string, userId: string) {
    const updated = await prisma.remedyIssue.update({ where: { id: remedyId }, data: { status: 'CONTESTED', lastActorUserId: userId } });
    await prisma.handoverEvent.create({ data: { passportId: updated.passportId, actorUserId: userId, eventType: 'REMEDY_CONTESTED', entityType: 'REMEDY', entityId: remedyId } });
    return updated;
  },

  // ── Timeline ────────────────────────────────────────────────────
  async getTimeline(passportId: string) {
    return prisma.handoverEvent.findMany({ where: { passportId }, orderBy: { createdAt: 'desc' } });
  },

  // ── Evidence Pack ───────────────────────────────────────────────
  async generateEvidencePack(passportId: string) {
    const passport = await prisma.handoverPassport.findUnique({
      where: { id: passportId },
      include: {
        listing: { select: { title: true, slug: true, publicArea: true } },
        promises: { include: { revisions: { orderBy: { revisionNumber: 'asc' } }, evidence: true }, orderBy: { createdAt: 'asc' } },
        evidence: { orderBy: { createdAt: 'asc' } },
        remedies: { include: { evidence: true }, orderBy: { createdAt: 'asc' } },
        events: { orderBy: { createdAt: 'asc' } },
        participants: true,
      },
    });
    if (!passport) throw new AppError('NOT_FOUND', 'Passport not found.', 404);
    await prisma.handoverEvent.create({ data: { passportId, eventType: 'EVIDENCE_PACK_EXPORTED', entityType: 'PASSPORT' } });
    return passport;
  },
};
