import crypto from 'node:crypto';

const ALGORITHM = 'aes-256-gcm';

export type EncryptedPayload = {
  keyVersion: number;
  encryptedDek: Buffer;
  iv: Buffer;
  ciphertext: Buffer;
  authTag: Buffer;
};

export type SerializedEncryptedPayload = {
  keyVersion: number;
  encryptedDek: string; // base64
  iv: string; // base64
  ciphertext: string; // base64
  authTag: string; // base64
};

export class FieldEncryptionService {
  constructor(
    private readonly rootKey: Buffer,
    private readonly keyVersion: number,
  ) {
    if (rootKey.byteLength !== 32) {
      throw new Error('FIELD_ENCRYPTION_ROOT_KEY must be 32 bytes');
    }
  }

  encrypt(plaintext: Buffer, aad: string): EncryptedPayload {
    const dek = crypto.randomBytes(32);
    const iv = crypto.randomBytes(12);
    const cipher = crypto.createCipheriv(ALGORITHM, dek, iv);
    cipher.setAAD(Buffer.from(aad, 'utf8'));
    const ciphertext = Buffer.concat([cipher.update(plaintext), cipher.final()]);
    const authTag = cipher.getAuthTag();

    // Wrap DEK with root key
    const kekIv = crypto.randomBytes(12);
    const kekCipher = crypto.createCipheriv(ALGORITHM, this.rootKey, kekIv);
    kekCipher.setAAD(Buffer.from(`dek:${aad}`, 'utf8'));
    const encryptedDekBody = Buffer.concat([kekCipher.update(dek), kekCipher.final()]);
    const encryptedDek = Buffer.concat([kekIv, kekCipher.getAuthTag(), encryptedDekBody]);

    return {
      keyVersion: this.keyVersion,
      encryptedDek,
      iv,
      ciphertext,
      authTag,
    };
  }

  decrypt(payload: EncryptedPayload, aad: string): Buffer {
    // Unwrap DEK
    const kekIv = payload.encryptedDek.subarray(0, 12);
    const kekAuthTag = payload.encryptedDek.subarray(12, 28);
    const encryptedDekBody = payload.encryptedDek.subarray(28);
    const kekDecipher = crypto.createDecipheriv(ALGORITHM, this.rootKey, kekIv);
    kekDecipher.setAAD(Buffer.from(`dek:${aad}`, 'utf8'));
    kekDecipher.setAuthTag(kekAuthTag);
    const dek = Buffer.concat([kekDecipher.update(encryptedDekBody), kekDecipher.final()]);

    // Decrypt data
    const decipher = crypto.createDecipheriv(ALGORITHM, dek, payload.iv);
    decipher.setAAD(Buffer.from(aad, 'utf8'));
    decipher.setAuthTag(payload.authTag);
    return Buffer.concat([decipher.update(payload.ciphertext), decipher.final()]);
  }

  encryptString(plaintext: string, subjectType: string, subjectId: string, fieldName: string): EncryptedPayload {
    const aad = `dorja:v1:${subjectType}:${subjectId}:${fieldName}`;
    return this.encrypt(Buffer.from(plaintext, 'utf8'), aad);
  }

  decryptString(payload: EncryptedPayload, subjectType: string, subjectId: string, fieldName: string): string {
    const aad = `dorja:v1:${subjectType}:${subjectId}:${fieldName}`;
    return this.decrypt(payload, aad).toString('utf8');
  }

  serialize(payload: EncryptedPayload): SerializedEncryptedPayload {
    return {
      keyVersion: payload.keyVersion,
      encryptedDek: payload.encryptedDek.toString('base64'),
      iv: payload.iv.toString('base64'),
      ciphertext: payload.ciphertext.toString('base64'),
      authTag: payload.authTag.toString('base64'),
    };
  }

  deserialize(serialized: SerializedEncryptedPayload): EncryptedPayload {
    return {
      keyVersion: serialized.keyVersion,
      encryptedDek: Buffer.from(serialized.encryptedDek, 'base64'),
      iv: Buffer.from(serialized.iv, 'base64'),
      ciphertext: Buffer.from(serialized.ciphertext, 'base64'),
      authTag: Buffer.from(serialized.authTag, 'base64'),
    };
  }
}

// Singleton for server use
let _encryptionService: FieldEncryptionService | null = null;

export function getEncryptionService(): FieldEncryptionService {
  if (!_encryptionService) {
    const keyB64 = process.env.FIELD_ENCRYPTION_ROOT_KEY_BASE64;
    if (!keyB64) throw new Error('FIELD_ENCRYPTION_ROOT_KEY_BASE64 is required');
    const rootKey = Buffer.from(keyB64, 'base64');
    const keyVersion = parseInt(process.env.FIELD_ENCRYPTION_KEY_VERSION || '1', 10);
    _encryptionService = new FieldEncryptionService(rootKey, keyVersion);
  }
  return _encryptionService;
}
