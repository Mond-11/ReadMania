-- V10__institutions_unique_name.sql
-- Унікальний індекс для назв закладів (без урахування регістру)

CREATE UNIQUE INDEX uk_institutions_lower_name ON institutions (LOWER(name));
