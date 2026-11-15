-- V10__institutions_unique_name.sql
-- Унікальний індекс для назв закладів (без урахування регістру)

-- 1. Переприв'язка замовлень та журналу подарунків до первинного (найменшого) id закладу за наявності дублікатів
UPDATE orders o
SET institution_id = canonical.min_id
FROM (
    SELECT id, MIN(id) OVER (PARTITION BY LOWER(name)) AS min_id
    FROM institutions
) canonical
WHERE o.institution_id = canonical.id AND canonical.id != canonical.min_id;

UPDATE gift_log g
SET institution_id = canonical.min_id
FROM (
    SELECT id, MIN(id) OVER (PARTITION BY LOWER(name)) AS min_id
    FROM institutions
) canonical
WHERE g.institution_id = canonical.id AND canonical.id != canonical.min_id;

-- 2. Видалення дублікатів закладів (залишаємо заклад із найменшим id)
DELETE FROM institutions
WHERE id NOT IN (
    SELECT MIN(id)
    FROM institutions
    GROUP BY LOWER(name)
);

-- 3. Створення унікального індексу без урахування регістру
CREATE UNIQUE INDEX IF NOT EXISTS uk_institutions_lower_name ON institutions (LOWER(name));
