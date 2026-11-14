import { notifications } from '@mantine/notifications';
import { ERRORS } from './labels.js';
import { getErrorMessage } from './api/errorMessage.js';

export function notifyError(errorOrText, { raw = false } = {}) {
  notifications.show({
    color: 'red',
    title: ERRORS.title,
    message: raw ? String(errorOrText) : getErrorMessage(errorOrText),
    autoClose: 8000,
  });
}

export function notifySuccess(message) {
  notifications.show({ color: 'brand', message });
}
