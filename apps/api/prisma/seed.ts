import { PrismaClient } from '@prisma/client';
import crypto from 'node:crypto';

const prisma = new PrismaClient();

function hashPhone(phone: string): string {
  return crypto.createHash('sha256').update(phone).digest('hex');
}

async function main() {
  console.log('🌱 Seeding DORJA users...');

  // Buyer user (username: buyer, password: 12345678)
  const buyer = await prisma.user.upsert({
    where: { phoneHash: hashPhone('+8801700000001') },
    update: {},
    create: {
      phoneHash: hashPhone('+8801700000001'),
      phoneLast4: '0001',
      displayName: 'Demo Buyer',
      primaryRole: 'SEEKER',
      identityStatus: 'IDENTITY_CONFIRMED',
      identityVerifiedAt: new Date(),
    },
  });

  // Seller user (username: seller, password: 12345678)
  const seller = await prisma.user.upsert({
    where: { phoneHash: hashPhone('+8801700000002') },
    update: {},
    create: {
      phoneHash: hashPhone('+8801700000002'),
      phoneLast4: '0002',
      displayName: 'Demo Seller',
      primaryRole: 'OWNER',
      identityStatus: 'IDENTITY_CONFIRMED',
      identityVerifiedAt: new Date(),
    },
  });

  console.log('✅ Seed complete');
  console.log(`   buyer: ${buyer.id}`);
  console.log(`   seller: ${seller.id}`);
}

main()
  .catch((e) => {
    console.error('❌ Seed failed:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
