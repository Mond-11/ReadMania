import { notifyError } from '../notify.js';

const toPath = (field) => field.replace(/\[(\d+)\]/g, '.$1').split('.');
const hasPath = (obj, field) => {
  let cur = obj;
  for (const key of toPath(field)) {
    if (cur === null || typeof cur !== 'object' || !(key in cur)) return false;
    cur = cur[key];
  }
  return true;
};

// Field errors for fields missing from the form are shown as a notification so they never vanish silently.
export function applyFormErrors(form, error) {
  const fields = error?.fieldErrors;
  if (!fields || Object.keys(fields).length === 0) return false;
  form.setErrors(fields);
  const values = form.getValues();
  const unmapped = Object.entries(fields).filter(([field]) => !hasPath(values, field));
  if (unmapped.length > 0) notifyError(unmapped.map(([, message]) => message).join(' '), { raw: true });
  return true;
}
