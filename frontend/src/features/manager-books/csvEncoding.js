// The backend reads the file as UTF-8 and does not strip a BOM (the first header would not match).
// Excel saves "CSV" as windows-1251 or as UTF-8 with BOM, so the file is re-encoded to plain UTF-8 first.
export async function normalizeCsvEncoding(file) {
  const buffer = await file.arrayBuffer();
  let text;
  try {
    text = new TextDecoder('utf-8', { fatal: true }).decode(buffer);
  } catch {
    text = new TextDecoder('windows-1251').decode(buffer);
  }
  return new File([text.replace(/^﻿/, '')], file.name, { type: 'text/csv' });
}
