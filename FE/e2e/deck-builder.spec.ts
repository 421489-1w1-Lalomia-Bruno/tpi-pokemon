import { test, expect } from '@playwright/test';
import { registerUser, createDeckViaRandom, equipDeck } from './helpers';

const TEST_USER = `ed-${Date.now()}`;
const TEST_PASS = 'Test1234';
let deckCounter = 0;

test.describe('Deck Builder', () => {
  test.beforeEach(async ({ page }) => {
    await registerUser(page, TEST_USER, TEST_PASS);
    await page.goto('/deck-builder');
    await page.waitForSelector('app-deck-builder-page');
  });

  test('crear un mazo con Al Azar y guardarlo', async ({ page }) => {
    const deckName = `Mazo-${Date.now()}-${deckCounter++}`;
    await createDeckViaRandom(page, deckName);
    await expect(page.locator('li.deck-list__item', { hasText: deckName })).toBeVisible();
  });

  test('asignar mazo para usar en partida', async ({ page }) => {
    const deckName = `Mazo-${Date.now()}-${deckCounter++}`;
    await createDeckViaRandom(page, deckName);
    await equipDeck(page, deckName);
  });

  test('botón Guardar deshabilitado con menos de 60 cartas', async ({ page }) => {
    const nuevoBtn = page.getByRole('button', { name: /Nuevo/ });
    if (await nuevoBtn.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await nuevoBtn.click();
    } else {
      await page.locator('.deck-list__header app-button').filter({ hasText: '+ Nuevo' }).click();
    }

    await page.waitForSelector('input.editor__name', { timeout: 5_000 });

    // Add 1 card so the save button renders (it only renders when cards > 0)
    const addBtn = page.locator('button.catalog__add-btn').first();
    await expect(addBtn).toBeVisible({ timeout: 5_000 });
    await addBtn.click();

    // Save button should be disabled (less than 60 cards)
    await expect(page.locator('button.editor__save-btn')).toBeDisabled();
  });

  test('eliminar un mazo', async ({ page }) => {
    const deckName = `Mazo-${Date.now()}-${deckCounter++}`;
    await createDeckViaRandom(page, deckName);

    const deckItem = page.locator('li.deck-list__item', { hasText: deckName });
    await deckItem.locator('button.deck-list__delete-btn').first().click();

    // Wait for modal backdrop (visible, not the hidden custom element)
    await page.waitForSelector('.modal-backdrop', { timeout: 5_000 });

    const apiDelete = page.waitForResponse(
      (resp) => resp.url().includes('/api/decks/') && resp.request().method() === 'DELETE',
      { timeout: 10_000 },
    );

    await page.locator('.modal-footer app-button').filter({ hasText: 'Eliminar' }).click();
    await apiDelete;

    await expect(page.locator('li.deck-list__item', { hasText: deckName })).not.toBeVisible({ timeout: 5_000 });
  });
});
