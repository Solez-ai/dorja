import { prisma } from '../../lib/prisma.js';
import { getEncryptionService } from '../../lib/encryption.js';
import { NotFoundError, ForbiddenError, ValidationError } from '../../lib/errors.js';
import { canTransition } from '@dorja/domain';
import { nanoid } from 'nanoid';
import type { Prisma, Listing } from '@prisma/client';

function generateSlug(title: string): string {
  const slugified = title
    .toLowerCase()
    .replace(/[^a-z0-9\u0980-\u09FF\s-]/g, '')
    .replace(/\s+/g, '-')
    .slice(0, 60);
  return `${slugified}-${nanoid(6)}`;
}

export class ListingService {
  /**
   * Create a draft listing with encrypted address
   */
  async create(
    ownerId: string,
    data: {
      title: string;
      intent: string;
      propertyType: string;
      publicArea: string;
      exactAddress: string;
      mapsLink?: string;
      approximateLat?: number;
      approximateLng?: number;
      priceAmount: number;
      currency?: string;
      rooms: Array<{ roomType: string; displayName: string; ordinal: number }>;
    },
  ): Promise<Listing> {
    const encryption = getEncryptionService();
    const slug = generateSlug(data.title);

    // Encrypt exact address
    const addressEncrypted = encryption.encryptString(
      data.exactAddress,
      'listing',
      slug, // Use slug as temp ID before create
      'exactAddress',
    );
    const serialized = encryption.serialize(addressEncrypted);

    // Build room create inputs
    const roomCreates: Prisma.RoomCreateNestedManyWithoutListingInput = {
      create: data.rooms.map((r) => ({
        roomType: r.roomType as any,
        displayName: r.displayName,
        ordinal: r.ordinal,
      })),
    };

    const listing = await prisma.listing.create({
      data: {
        slug,
        ownerId,
        title: data.title,
        intent: data.intent as any,
        propertyType: data.propertyType as any,
        status: 'DRAFT',
        publicArea: data.publicArea,
        exactAddressEncrypted: Buffer.from(JSON.stringify(serialized)),
        approximateLat: data.approximateLat,
        approximateLng: data.approximateLng,
        mapsLink: data.mapsLink,
        priceAmount: data.priceAmount,
        currency: data.currency || 'BDT',
        authorityStatus: 'NOT_STARTED',
        rooms: roomCreates,
      },
      include: { rooms: true },
    });

    // Auto-pulse: make listing immediately active
    const now = new Date();
    const expiresAt = new Date(now.getTime() + 48 * 60 * 60 * 1000);
    return prisma.listing.update({
      where: { id: listing.id },
      data: {
        status: 'ACTIVE',
        livePulseAt: now,
        livePulseExpiresAt: expiresAt,
      },
      include: { rooms: true },
    });
  }

  /**
   * Get public listing cards (no exact address)
   */
  async getPublicListings(filters: {
    intent?: string;
    propertyType?: string;
    minPrice?: number;
    maxPrice?: number;
    area?: string;
    page?: number;
    limit?: number;
  }): Promise<{ listings: any[]; total: number }> {
    const where: Prisma.ListingWhereInput = {
      status: { in: ['ACTIVE', 'VIEWING_HELD', 'UNCONFIRMED'] },
    };

    if (filters.intent) where.intent = filters.intent as any;
    if (filters.propertyType) where.propertyType = filters.propertyType as any;
    if (filters.minPrice) where.priceAmount = { ...where.priceAmount as any, gte: filters.minPrice };
    if (filters.maxPrice) where.priceAmount = { ...where.priceAmount as any, lte: filters.maxPrice };
    if (filters.area) where.publicArea = { contains: filters.area, mode: 'insensitive' };

    const page = filters.page || 1;
    const limit = filters.limit || 20;
    const skip = (page - 1) * limit;

    const [listings, total] = await Promise.all([
      prisma.listing.findMany({
        where,
        select: {
          id: true,
          slug: true,
          title: true,
          intent: true,
          propertyType: true,
          status: true,
          publicArea: true,
          approximateLat: true,
          approximateLng: true,
          priceAmount: true,
          currency: true,
          livePulseAt: true,
          livePulseExpiresAt: true,
          authorityStatus: true,
          createdAt: true,
          rooms: { select: { id: true, roomType: true, displayName: true } },
          realityPassports: {
            take: 1,
            orderBy: { publishedAt: 'desc' },
            select: { reviewLevel: true, publishedAt: true, coverageScore: true },
          },
        },
        orderBy: [
          { livePulseAt: 'desc' },
          { createdAt: 'desc' },
        ],
        skip,
        take: limit,
      }),
      prisma.listing.count({ where }),
    ]);

    return {
      listings: listings.map((l) => ({
        ...l,
        realityReviewLevel: l.realityPassports[0]?.reviewLevel ?? null,
        realityPublishedAt: l.realityPassports[0]?.publishedAt ?? null,
        realityPassports: undefined,
      })),
      total,
    };
  }

  /**
   * Get public Reality Passport by slug
   */
  async getPublicPassport(slug: string): Promise<any> {
    const listing = await prisma.listing.findUnique({
      where: { slug },
      include: {
        rooms: {
          orderBy: { ordinal: 'asc' },
          include: {
            tourNode: {
              include: {
                fromEdges: { include: { toNode: true } },
              },
            },
          },
        },
        realityPassports: {
          take: 1,
          orderBy: { publishedAt: 'desc' },
          include: { captureSession: true },
        },
      },
    });

    if (!listing) throw new NotFoundError('Listing');

    const passport = listing.realityPassports[0];
    const isLive = listing.livePulseAt && (!listing.livePulseExpiresAt || listing.livePulseExpiresAt > new Date());

    // Build room edges from tour graph
    const edges: Array<{ fromRoomId: string; toRoomId: string; doorwayLabel: string }> = [];
    for (const room of listing.rooms) {
      if (room.tourNode) {
        for (const edge of room.tourNode.fromEdges) {
          edges.push({
            fromRoomId: room.id,
            toRoomId: edge.toNode.roomId,
            doorwayLabel: edge.doorwayLabel,
          });
        }
      }
    }

    // Missing rooms
    const capturedRoomIds = new Set(
      passport?.captureSession
        ? (await prisma.room.findMany({
            where: { captureSessionId: passport.captureSessionId },
            select: { id: true },
          })).map((r) => r.id)
        : []
    );
    const missingRoomLabels = listing.rooms
      .filter((r) => !capturedRoomIds.has(r.id))
      .map((r) => r.displayName);

    return {
      listing: {
        id: listing.id,
        slug: listing.slug,
        title: listing.title,
        publicArea: listing.publicArea,
        intent: listing.intent,
        propertyType: listing.propertyType,
        priceAmount: listing.priceAmount,
        currency: listing.currency as 'BDT',
        livePulse: {
          status: isLive ? 'AVAILABLE' : 'UNCONFIRMED',
          confirmedAt: listing.livePulseAt?.toISOString(),
        },
      },
      reality: {
        reviewLevel: passport?.reviewLevel ?? 'INCOMPLETE',
        capturedAt: passport?.captureSession?.captureTimestamp?.toISOString(),
        coverageScore: passport?.coverageScore ?? 0,
        missingRoomLabels,
        sourceSummary: 'HOLD_TO_CAPTURE',
      },
      rooms: listing.rooms.map((r) => ({
        id: r.id,
        roomType: r.roomType,
        displayName: r.displayName,
        previewUrl: r.tourNode?.previewAssetId ?? '/placeholder-room.jpg',
        panoramaUrl: r.tourNode?.panoramaAssetId ?? undefined,
        sourceType: 'HOLD_TO_CAPTURE',
      })),
      edges,
    };
  }

  /**
   * Live Pulse: reconfirm availability
   */
  async reconfirmPulse(listingId: string, userId: string): Promise<Listing> {
    const listing = await prisma.listing.findUnique({ where: { id: listingId } });
    if (!listing) throw new NotFoundError('Listing');
    if (listing.ownerId !== userId) throw new ForbiddenError('Only the listing owner can reconfirm availability.');

    const now = new Date();
    const expiresAt = new Date(now.getTime() + 48 * 60 * 60 * 1000); // 48h

    return prisma.listing.update({
      where: { id: listingId },
      data: {
        livePulseAt: now,
        livePulseExpiresAt: expiresAt,
        status: listing.status === 'UNCONFIRMED' ? 'ACTIVE' : listing.status,
      },
    });
  }

  /**
   * Mark listing as rented/sold
   */
  async markClosed(listingId: string, userId: string): Promise<Listing> {
    const listing = await prisma.listing.findUnique({ where: { id: listingId } });
    if (!listing) throw new NotFoundError('Listing');
    if (listing.ownerId !== userId) throw new ForbiddenError('Only the listing owner can mark as closed.');

    const result = canTransition(listing.status, 'mark_closed');
    if (!result.allowed) {
      throw new ValidationError(`Cannot close listing in ${listing.status} status.`);
    }

    return prisma.listing.update({
      where: { id: listingId },
      data: { status: result.next },
    });
  }

  /**
   * Get listing by ID (for owner)
   */
  async getById(id: string): Promise<Listing> {
    const listing = await prisma.listing.findUnique({
      where: { id },
      include: { rooms: { orderBy: { ordinal: 'asc' } } },
    });
    if (!listing) throw new NotFoundError('Listing');
    return listing;
  }

  /**
   * Update listing
   */
  async update(id: string, userId: string, data: Partial<{
    title: string;
    publicArea: string;
    priceAmount: number;
    mapsLink: string;
  }>): Promise<Listing> {
    const listing = await prisma.listing.findUnique({ where: { id } });
    if (!listing) throw new NotFoundError('Listing');
    if (listing.ownerId !== userId) throw new ForbiddenError('Only the listing owner can update.');

    return prisma.listing.update({
      where: { id },
      data,
    });
  }
}

export const listingService = new ListingService();
