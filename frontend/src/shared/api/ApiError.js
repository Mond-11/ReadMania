const TYPE_PREFIX = 'urn:readmania:error:';

// `type` is shortened: 'urn:readmania:error:account-blocked' -> 'account-blocked'.
export class ApiError extends Error {
  constructor({ status = 0, type = null, title = null, detail = null, errors = [], extra = {}, network = false } = {}) {
    super(detail || title || (network ? 'Network error' : `HTTP ${status}`));
    this.name = 'ApiError';
    this.status = status;
    this.type = type;
    this.title = title;
    this.detail = detail;
    this.isNetwork = network;
    this.errors = Array.isArray(errors) ? errors : [];
    this.fieldErrors = fieldErrorsFrom(this.errors);
    this.retryAfter = extra.retryAfter ?? null;
    this.blockReason = extra.blockReason ?? null;
    this.blockedAt = extra.blockedAt ?? null;
    this.currentStatus = extra.currentStatus ?? null;
    this.errorId = extra.errorId ?? null;
  }

  is(type) {
    return this.type === type;
  }

  static fromResponse(status, data) {
    if (data && typeof data === 'object') {
      const rawType = typeof data.type === 'string' ? data.type : null;
      const type = rawType && rawType.startsWith(TYPE_PREFIX) ? rawType.slice(TYPE_PREFIX.length) : null;
      return new ApiError({
        status,
        type,
        title: data.title ?? null,
        detail: data.detail ?? null,
        errors: data.errors,
        extra: data,
      });
    }
    return new ApiError({ status });
  }

  static network() {
    return new ApiError({ status: 0, network: true });
  }
}

function fieldErrorsFrom(errors) {
  const out = {};
  for (const e of errors) {
    if (!e || !e.field || !e.message) continue;
    if (!(e.field in out)) out[e.field] = e.message;
  }
  return out;
}
