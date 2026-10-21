-- Les libellés du classeur Ciqual contiennent des retours à la ligne (cellules à la ligne
-- automatique) : on les remplace par des espaces. Les imports suivants les aplatissent déjà.
UPDATE food
SET name            = btrim(regexp_replace(name, '\s+', ' ', 'g')),
    scientific_name = btrim(regexp_replace(scientific_name, '\s+', ' ', 'g'))
WHERE name ~ '\s{2,}|[\r\n\t]' OR scientific_name ~ '\s{2,}|[\r\n\t]';

UPDATE food_group
SET name = btrim(regexp_replace(name, '\s+', ' ', 'g'))
WHERE name ~ '\s{2,}|[\r\n\t]';
