import { ERRORS } from '../labels.js';

export function getErrorMessage(error) {
  if (!error) return ERRORS.unknown;
  if (error.isNetwork) return ERRORS.network;
  const { status, detail } = error;
  if (!status) return ERRORS.unknown;
  if (status >= 500) return error.errorId ? `${ERRORS.serverWithCode} ${error.errorId}` : ERRORS.server;
  if (status === 401) return ERRORS.unauthorized;
  if (status === 403) return error.type === 'access-denied' || !detail ? ERRORS.forbidden : detail;
  if (status === 404) return detail || ERRORS.notFound;
  if (status === 409) return detail || ERRORS.conflict;
  if (status === 400 || status === 422) return detail || ERRORS.validation;
  return detail || ERRORS.unknown;
}
