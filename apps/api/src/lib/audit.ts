import crypto from 'node:crypto';
import type { PrismaClient, AuditActorType } from '@prisma/client';

export type AuditInput = {
  actorType: AuditActorType;
  actorUserId: string | null;
  action: string;
  subjectType: string;
  subjectId: string;
  requestId?: string;
  ipHash?: string;
  userAgentHash?: string;
  publicMetadataJson?: Record<string, unknown>;
  previousEventHash?: string | null;
};

function computeAuditHash(input: AuditInput & { occurredAtIso: string }): string {
  const canonical = JSON.stringify({
    previousEventHash: input.previousEventHash,
    actorType: input.actorType,
    actorUserId: input.actorUserId,
    action: input.action,
    subjectType: input.subjectType,
    subjectId: input.subjectId,
    occurredAtIso: input.occurredAtIso,
    publicMetadataJson: input.publicMetadataJson,
  });
  const auditKey = Buffer.from(process.env.AUDIT_LOG_HMAC_KEY_BASE64!, 'base64');
  return crypto
    .createHmac('sha256', auditKey)
    .update(canonical)
    .digest('hex');
}

export class AuditWriter {
  private lastHash: string | null;

  constructor(
    private readonly prisma: PrismaClient,
    private readonly requestId?: string,
  ) {
    this.lastHash = null;
  }

  async write(input: Omit<AuditInput, 'previousEventHash'>): Promise<string> {
    const now = new Date();
    const occurredAtIso = now.toISOString();

    // Get previous event hash for chain
    if (this.lastHash === null) {
      const lastEvent = await this.prisma.auditLog.findFirst({
        orderBy: { createdAt: 'desc' },
        select: { eventHash: true },
      });
      this.lastHash = lastEvent?.eventHash ?? null;
    }

    const fullInput: AuditInput = {
      ...input,
      previousEventHash: this.lastHash,
    };

    const eventHash = computeAuditHash({ ...fullInput, occurredAtIso });

    await this.prisma.auditLog.create({
      data: {
        actorType: input.actorType,
        actorUserId: input.actorUserId,
        action: input.action,
        subjectType: input.subjectType,
        subjectId: input.subjectId,
        requestId: input.requestId ?? this.requestId,
        ipHash: input.ipHash,
        userAgentHash: input.userAgentHash,
        publicMetadataJson: input.publicMetadataJson as any ?? undefined,
        eventHash,
        previousEventHash: this.lastHash,
      },
    });

    this.lastHash = eventHash;
    return eventHash;
  }
}
