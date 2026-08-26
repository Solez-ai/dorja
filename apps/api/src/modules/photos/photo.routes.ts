import type { FastifyInstance } from 'fastify';
import { requireAuth } from '../../lib/auth.js';
import crypto from 'node:crypto';

export async function photoRoutes(app: FastifyInstance): Promise<void> {
  // POST /v1/photos/upload - accept base64 data URL
  app.post('/v1/photos/upload', { preHandler: [requireAuth] }, async (request, reply) => {
    const { dataUrl, filename } = request.body as { dataUrl: string; filename: string };
    if (!dataUrl) return reply.code(400).send({ error: { code: 'NO_DATA', message: 'No image data' } });
    const id = crypto.randomUUID();
    const ext = (filename || 'photo.jpg').split('.').pop() || 'jpg';
    return reply.send({ data: { id, key: 'rooms/' + id + '.' + ext, url: dataUrl, filename: filename || 'photo.jpg', size: Math.floor(dataUrl.length * 0.75) } });
  });

  // POST /v1/photos/label - label a photo with room name
  app.post('/v1/photos/label', { preHandler: [requireAuth] }, async (request, reply) => {
    const { photoId, label } = request.body as { photoId: string; label: string };
    return reply.send({ data: { photoId, label, labeledAt: new Date().toISOString() } });
  });
}
