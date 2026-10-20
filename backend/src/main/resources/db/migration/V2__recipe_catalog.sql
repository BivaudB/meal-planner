-- =====================================================================
-- Catalogue de recettes fournies avec l'application : elles appartiennent
-- à un utilisateur technique (sans compte Keycloak) et sont publiques.
-- Les recettes elles-mêmes sont ajoutées au démarrage du backend
-- (resources/seed/recipes.json), une fois le référentiel Ciqual chargé.
-- =====================================================================

INSERT INTO app_user (external_id, username, display_name)
VALUES ('system:catalog', 'meal-planner', 'Meal Planner')
ON CONFLICT (external_id) DO NOTHING;
