// Types des objets échangés avec l'API (miroir des records Java du backend).

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Me {
  id: number;
  username: string;
  email: string | null;
  displayName: string | null;
  admin: boolean;
}

export type Visibility = 'PRIVATE' | 'PUBLIC';
export type RecipeSource = 'MANUAL' | 'IMPORT' | 'CATALOG' | 'INSTAGRAM' | 'YOUTUBE';

export interface RecipeSummary {
  id: number;
  name: string;
  servings: number | null;
  prepMinutes: number | null;
  cookMinutes: number | null;
  visibility: Visibility;
  source: RecipeSource;
  ingredientCount: number;
  hasSteps: boolean;
  mine: boolean;
  nutrition: Nutrition;
  updatedAt: string;
}

export interface IngredientLine {
  id: number;
  position: number;
  name: string;
  quantity: number | null;
  unit: string | null;
  rawQuantity: string | null;
  foodId: number | null;
  foodName: string | null;
  matchScore: number | null;
}

/** Calories, protéines, glucides, lipides : les valeurs mises en avant partout. */
export interface Macros {
  energyKcal: number | null;
  proteinG: number | null;
  carbohydrateG: number | null;
  fatG: number | null;
}

export interface Nutrition {
  total: Macros;
  /** Null si le nombre de personnes n'est pas renseigné. */
  perServing: Macros | null;
  ingredientsCounted: number;
  ingredientsTotal: number;
}

export interface RecipeDetail {
  id: number;
  name: string;
  description: string | null;
  servings: number | null;
  prepMinutes: number | null;
  cookMinutes: number | null;
  visibility: Visibility;
  source: RecipeSource;
  sourceUrl: string | null;
  mine: boolean;
  ingredients: IngredientLine[];
  steps: string[];
  nutrition: Nutrition;
  createdAt: string;
  updatedAt: string;
}

export interface IngredientRequest {
  name: string;
  quantity: number | null;
  unit: string | null;
  foodId: number | null;
}

export interface RecipeRequest {
  name: string;
  description: string | null;
  servings: number | null;
  prepMinutes: number | null;
  cookMinutes: number | null;
  visibility: Visibility;
  sourceUrl: string | null;
  ingredients: IngredientRequest[];
  steps: string[];
}

export interface FoodSummary {
  id: number;
  name: string;
  groupName: string | null;
  energyKcal: number | null;
  proteinG: number | null;
  carbohydrateG: number | null;
  fatG: number | null;
  score: number | null;
}

export interface NutrientValue {
  id: number;
  name: string;
  unit: string | null;
  value: number | null;
  qualifier: string | null;
  rawValue: string | null;
}

export interface FoodDetail extends Omit<FoodSummary, 'score'> {
  scientificName: string | null;
  energyKj: number | null;
  sugarsG: number | null;
  saturatedFatG: number | null;
  fiberG: number | null;
  saltG: number | null;
  nutrients: NutrientValue[];
}

export type ImportStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'COMPLETED_WITH_ERRORS' | 'FAILED';

export interface ImportJob {
  id: number;
  type: 'RECIPES' | 'CIQUAL';
  filename: string;
  status: ImportStatus;
  totalItems: number;
  importedItems: number;
  errorItems: number;
  report: string | null;
  createdAt: string;
  finishedAt: string | null;
}

export interface PantryItem {
  id: number;
  name: string;
  foodId: number | null;
  quantity: number | null;
  unit: string | null;
}

export interface Recommendation {
  recipeId: number;
  name: string;
  servings: number | null;
  prepMinutes: number | null;
  cookMinutes: number | null;
  mine: boolean;
  totalIngredients: number;
  availableIngredients: number;
  coverage: number;
  missingIngredients: string[];
  nutrition: Nutrition | null;
}

/** Unités reconnues par le backend (valeur stockée -> libellé affiché). */
export const UNITS: { value: string; label: string }[] = [
  { value: 'g', label: 'g' },
  { value: 'kg', label: 'kg' },
  { value: 'ml', label: 'ml' },
  { value: 'cl', label: 'cl' },
  { value: 'l', label: 'l' },
  { value: 'cas', label: 'c. à soupe' },
  { value: 'cac', label: 'c. à café' },
  { value: 'pincee', label: 'pincée' },
  { value: 'piece', label: 'pièce(s)' },
  { value: 'gousse', label: 'gousse(s)' },
  { value: 'tranche', label: 'tranche(s)' },
  { value: 'sachet', label: 'sachet(s)' },
  { value: 'botte', label: 'botte(s)' },
  { value: 'verre', label: 'verre(s)' },
  { value: 'boite', label: 'boîte(s)' },
];

export function unitLabel(unit: string | null): string {
  if (!unit) {
    return '';
  }
  return UNITS.find((u) => u.value === unit)?.label ?? unit;
}
