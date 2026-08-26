import Fastify from 'fastify';
import cors from '@fastify/cors';
import jwt from '@fastify/jwt';
import rateLimit from '@fastify/rate-limit';
import { config } from './config.js';
import { prisma } from './lib/prisma.js';
import { authRoutes } from './modules/auth/auth.routes.js';
import { listingRoutes } from './modules/listings/listing.routes.js';
import { captureRoutes } from './modules/capture/capture.routes.js';
import { messageRoutes } from './modules/messaging/message.routes.js';
import { offerRoutes } from './modules/offers/offer.routes.js';
import { viewingRoutes } from './modules/viewings/viewing.routes.js';
import { photoRoutes } from './modules/photos/photo.routes.js';
import { identityRoutes } from './modules/identity/identity.routes.js';
import { handoverRoutes } from './modules/handover/handover.routes.js';
import { chatRoutes } from './modules/chat/chat.routes.js';
import { AppError } from './lib/errors.js';

const app = Fastify({
  logger: {
    level: config.NODE_ENV === 'development' ? 'info' : 'warn',
  },
});

// --- Plugins ---
await app.register(cors, {
  origin: config.WEB_ORIGIN,
  credentials: true,
});

await app.register(jwt, {
  secret: config.JWT_ACCESS_SECRET,
  sign: { expiresIn: '1h' },
});

await app.register(rateLimit, {
  max: 100,
  timeWindow: '1 minute',
});

// --- Global error handler ---
app.setErrorHandler((error: any, request, reply) => {
  if (error instanceof AppError) {
    return reply.code(error.statusCode).send({
      error: {
        code: error.code,
        message: error.message,
        requestId: request.id,
      },
    });
  }

  if (error.validation) {
    return reply.code(422).send({
      error: {
        code: 'VALIDATION_ERROR',
        message: error.message,
        requestId: request.id,
      },
    });
  }

  app.log.error(error);
  return reply.code(500).send({
    error: {
      code: 'INTERNAL_ERROR',
      message: 'An unexpected error occurred.',
      requestId: request.id,
    },
  });
});

// --- Health ---
app.get('/v1/health', async () => {
  try {
    await prisma.$queryRaw`SELECT 1`;
    return { status: 'ok', timestamp: new Date().toISOString() };
  } catch (error) {
    return { status: 'error', message: 'Database connection failed' };
  }
});

// --- Register routes ---
await app.register(authRoutes);
await app.register(listingRoutes);
await app.register(captureRoutes);
await app.register(messageRoutes);
await app.register(offerRoutes);
await app.register(viewingRoutes);
  await app.register(photoRoutes);
await app.register(identityRoutes);
await app.register(handoverRoutes);
await app.register(chatRoutes);

// --- Start ---
async function start() {
  try {
    await prisma.$connect();
    console.log('✅ Database connected');

    await app.listen({ port: config.PORT, host: '0.0.0.0' });
    console.log(`🚀 DORJA API running at http://localhost:${config.PORT}`);
  } catch (err) {
    app.log.error(err);
    process.exit(1);
  }
}

// Graceful shutdown
process.on('SIGTERM', async () => {
  await app.close();
  await prisma.$disconnect();
  process.exit(0);
});

start();
