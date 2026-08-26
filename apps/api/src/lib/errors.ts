export class AppError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly statusCode: number = 400,
  ) {
    super(message);
    this.name = 'AppError';
  }
}

export class UnauthorizedError extends AppError {
  constructor(message = 'Authentication required.') {
    super('UNAUTHORIZED', message, 401);
  }
}

export class ForbiddenError extends AppError {
  constructor(message = 'You do not have permission to perform this action.') {
    super('FORBIDDEN', message, 403);
  }
}

export class NotFoundError extends AppError {
  constructor(resource: string) {
    super('NOT_FOUND', `${resource} not found.`, 404);
  }
}

export class ConflictError extends AppError {
  constructor(message: string) {
    super('CONFLICT', message, 409);
  }
}

export class ValidationError extends AppError {
  constructor(message: string) {
    super('VALIDATION_ERROR', message, 422);
  }
}

export class IdentityRequiredError extends ForbiddenError {
  constructor() {
    super('Identity confirmation is required before requesting a private viewing.');
  }
}

export class SlotCapacityError extends ConflictError {
  constructor() {
    super('This time slot no longer has available capacity.');
  }
}

export class PassInvalidError extends AppError {
  constructor(message = 'Viewing pass is invalid or has already been used.') {
    super('PASS_INVALID', message, 410);
  }
}

export class AddressRevealError extends ForbiddenError {
  constructor(reason: string) {
    super(reason);
  }
}
