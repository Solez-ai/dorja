import { Queue } from 'bullmq';
import { getRedis } from '../../lib/redis.js';

function getQueue(name: string) {
  return new Queue(name, {
    connection: getRedis(),
    defaultJobOptions: {
      attempts: 3,
      backoff: { type: 'exponential', delay: 2000 },
      removeOnComplete: { count: 100 },
      removeOnFail: { count: 50 },
    },
  });
}

export const queues = {
  mediaProcessing: getQueue('media-processing'),
  captureExpiry: getQueue('capture-expiry'),
  livePulseExpiry: getQueue('live-pulse-expiry'),
  offerExpiry: getQueue('offer-expiry'),
  viewingReminders: getQueue('viewing-reminders'),
  viewingPassExpiry: getQueue('viewing-pass-expiry'),
  safetyFollowUp: getQueue('safety-follow-up'),
  notificationDelivery: getQueue('notification-delivery'),
};

/**
 * Schedule recurring jobs
 */
export async function scheduleRecurringJobs() {
  // Live Pulse expiry: check every 15 minutes
  await queues.livePulseExpiry.add('check-expiry', {}, {
    repeat: { every: 15 * 60 * 1000 },
  });

  // Viewing reminders: check every 5 minutes
  await queues.viewingReminders.add('check-reminders', {}, {
    repeat: { every: 5 * 60 * 1000 },
  });

  // Safety follow-up (missed check-outs): every 5 minutes
  await queues.safetyFollowUp.add('check-missed-checkout', {}, {
    repeat: { every: 5 * 60 * 1000 },
  });

  // Offer expiry: check every 10 minutes
  await queues.offerExpiry.add('check-expiry', {}, {
    repeat: { every: 10 * 60 * 1000 },
  });

  // Retention purge: daily
  await queues.captureExpiry.add('daily-purge', {}, {
    repeat: { every: 24 * 60 * 60 * 1000 },
  });

  console.log('✅ Recurring jobs scheduled');
}
