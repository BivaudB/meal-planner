-- Le référentiel ne garde plus que les ingrédients de base (voir BasicIngredients) : plats
-- composés, desserts, boissons, sauces industrielles et versions cuites ne sont plus importés.
-- Cette migration applique le même filtre aux bases déjà chargées (règles figées à cette date).

-- Nom sans la mention « cru » (« Tomate ronde, crue » → « Tomate ronde »)
UPDATE food f
SET name = regexp_replace(f.name, '(?<!lait|jambon)(\s*,\s*|\s+)crue?s?(\s+(?=,))?(?=[\s,(]|$)', '', 'gi')
WHERE f.source = 'CIQUAL';

CREATE TEMPORARY TABLE excluded_food ON COMMIT DROP AS
SELECT f.id
FROM food f
WHERE f.source = 'CIQUAL'
  AND NOT (
    f.group_code IS NOT NULL
    AND f.group_code !~ '^(01|0303|040305|040306|040307|040308|0404|050203|050204|050205|06|0703|0705|0706|0707|0708|0709|08|1001|1008|1010|11)'
    AND f_normalize(f.name) !~ '^(accra|beignet|nuggets|croquette|tarama|terrine|rillettes|omelette)( |,|$)|faites? maison|faits? maison|(^|[ ,])panee?s?([ ,]|$)'
    AND NOT (f.group_code ~ '^(02|03|0401|0402|0403|0405|0406|0407|0408|0409|0410|10)'
      AND f_normalize(f.name) ~ '(^|[ ,(/])(cuite?s?|precuite?s?|bouillie?s?|braisee?s?|grillee?s?|rotie?s?|poelee?s?|sautee?s?|frite?s?|prefrite?s?|etouffee?s?|rissolee?s?|cuisinee?s?|mijotee?s?|pochee?s?|brouillee?s?|mollet|au four|vapeur|au plat|a la coque|oeufs? durs?|au grille-pain)([ ,)/]|$)'
      AND f_normalize(f.name) !~ '^jambon')
  );

-- Les ingrédients rattachés à un aliment retiré sont rattachés à nouveau parmi ceux qui restent
-- (même recherche que FoodRepository.search, seuil par défaut app.import.match-threshold = 0.45),
-- ou détachés si aucun n'est assez proche.
UPDATE recipe_ingredient ri
SET (food_id, match_score) = (
    SELECT f.id, word_similarity(f_normalize(ri.name), f_normalize(f.name))
    FROM food f
    WHERE f.id NOT IN (SELECT id FROM excluded_food)
      AND word_similarity(f_normalize(ri.name), f_normalize(f.name)) >= 0.45
    ORDER BY word_similarity(f_normalize(ri.name), f_normalize(f.name)) DESC,
             (f_normalize(f.name) LIKE f_normalize(ri.name) || '%') DESC,
             length(f.name)
    LIMIT 1)
WHERE ri.food_id IN (SELECT id FROM excluded_food);

UPDATE pantry_item pi
SET food_id = (
    SELECT f.id
    FROM food f
    WHERE f.id NOT IN (SELECT id FROM excluded_food)
      AND word_similarity(f_normalize(pi.name), f_normalize(f.name)) >= 0.45
    ORDER BY word_similarity(f_normalize(pi.name), f_normalize(f.name)) DESC,
             (f_normalize(f.name) LIKE f_normalize(pi.name) || '%') DESC,
             length(f.name)
    LIMIT 1)
WHERE pi.food_id IN (SELECT id FROM excluded_food);

-- Les valeurs nutritionnelles suivent (ON DELETE CASCADE)
DELETE FROM food WHERE id IN (SELECT id FROM excluded_food);
