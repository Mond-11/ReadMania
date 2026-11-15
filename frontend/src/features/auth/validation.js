import { AUTH } from '../../shared/labels.js';

// Front-end checks (U2) mirror the backend rules; every message names the field, the reason and an example (U3).
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const MIN_PASSWORD_LENGTH = 6;
export const MAX_REASON_LENGTH = 1000;

export function validateEmail(value) {
  const v = value.trim();
  if (!v) return AUTH.emailRequired;
  return EMAIL_RE.test(v) ? null : AUTH.emailInvalid;
}

export const validatePassword = (value) => (value ? null : AUTH.passwordRequired);

export function validateNewPassword(value) {
  if (!value) return AUTH.passwordRequired;
  return value.length < MIN_PASSWORD_LENGTH ? AUTH.passwordTooShort : null;
}

export const validateConfirmation = (field) => (value, values) =>
  value && value === values[field] ? null : AUTH.confirmMismatch;

export function validateReason(value) {
  const v = value.trim();
  if (!v) return AUTH.reasonRequired;
  return v.length > MAX_REASON_LENGTH ? AUTH.reasonTooLong : null;
}
