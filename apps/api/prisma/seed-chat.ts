import { PrismaClient } from '@prisma/client';
import crypto from 'node:crypto';

const prisma = new PrismaClient();

function hashPhone(phone: string): string {
  return crypto.createHash('sha256').update(phone).digest('hex');
}

async function main() {
  // Find users
  const buyer = await prisma.user.findUnique({ where: { phoneHash: hashPhone('+8801700000001') } });
  const seller = await prisma.user.findUnique({ where: { phoneHash: hashPhone('+8801700000002') } });

  if (!buyer || !seller) {
    console.log('Users not found. Run seed.ts first.');
    return;
  }

  console.log('Buyer:', buyer.id, buyer.displayName);
  console.log('Seller:', seller.id, seller.displayName);

  // Find listings
  const listings = await prisma.listing.findMany();
  console.log('Listings:', listings.length);

  // Create conversation 1: buyer asks about Mirpur listing
  const listing1 = listings.find(l => l.slug.includes('mirpur'));
  if (listing1) {
    const conv1 = await prisma.conversation.upsert({
      where: {
        listingId_seekerUserId_hostUserId: {
          listingId: listing1.id,
          seekerUserId: buyer.id,
          hostUserId: seller.id,
        },
      },
      update: {},
      create: {
        listingId: listing1.id,
        seekerUserId: buyer.id,
        hostUserId: seller.id,
        status: 'ACTIVE',
      },
    });
    console.log('Conversation 1:', conv1.id);

    // Seed messages
    const msgs = [
      { senderId: buyer.id, body: 'Hello! I saw your apartment in Mirpur 11. Is it still available?' },
      { senderId: seller.id, body: 'Yes, it is available! The apartment has 6 rooms including 3 bedrooms, kitchen, and bathroom.' },
      { senderId: buyer.id, body: 'Great! Can I schedule a SafeView visit this weekend?' },
      { senderId: seller.id, body: 'Sure! I can make Saturday afternoon work. What time suits you?' },
      { senderId: buyer.id, body: 'Saturday at 3 PM would be perfect. Looking forward to seeing the place!' },
      { senderId: seller.id, body: 'Confirmed. I will send you the viewing pass details soon. The address will unlock 1 hour before the visit.' },
    ];

    for (const msg of msgs) {
      await prisma.message.create({
        data: {
          conversationId: conv1.id,
          senderUserId: msg.senderId,
          kind: 'TEXT',
          bodyEncrypted: Buffer.from(msg.body, 'utf-8'),
          safePreview: msg.body,
        },
      });
    }
    console.log('Messages seeded:', msgs.length);
  }

  // Create conversation 2: buyer asks about Uttara listing
  const listing2 = listings.find(l => l.slug.includes('uttara'));
  if (listing2) {
    const conv2 = await prisma.conversation.upsert({
      where: {
        listingId_seekerUserId_hostUserId: {
          listingId: listing2.id,
          seekerUserId: buyer.id,
          hostUserId: seller.id,
        },
      },
      update: {},
      create: {
        listingId: listing2.id,
        seekerUserId: buyer.id,
        hostUserId: seller.id,
        status: 'ACTIVE',
      },
    });
    console.log('Conversation 2:', conv2.id);

    const msgs2 = [
      { senderId: buyer.id, body: 'Is the Uttara apartment suitable for a family of 4?' },
      { senderId: seller.id, body: 'Absolutely! It has 7 rooms, 3 bedrooms, a spacious living room, and is near Uttara Sector 7 market.' },
      { senderId: buyer.id, body: 'What about parking? We have two cars.' },
      { senderId: seller.id, body: 'There is dedicated parking for 2 vehicles in the building basement. Security guard 24/7.' },
    ];

    for (const msg of msgs2) {
      await prisma.message.create({
        data: {
          conversationId: conv2.id,
          senderUserId: msg.senderId,
          kind: 'TEXT',
          bodyEncrypted: Buffer.from(msg.body, 'utf-8'),
          safePreview: msg.body,
        },
      });
    }
    console.log('Messages seeded:', msgs2.length);
  }

  console.log('Done!');
}

main()
  .catch(console.error)
  .finally(() => prisma.$disconnect());
