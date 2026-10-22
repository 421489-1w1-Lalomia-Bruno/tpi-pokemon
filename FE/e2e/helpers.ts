import { Page, expect } from '@playwright/test';

export async function registerUser(
  page: Page,
  username: string,
  password: string,
  gender?: 'MALE' | 'FEMALE' | 'OTHER',
): Promise<void> {
  await page.goto('/register');
  await page.waitForSelector('app-register-page');

  await page.getByPlaceholder('Elegí un nombre de usuario').fill(username);
  await page.getByPlaceholder('Mínimo 8 caracteres').fill(password);
  await page.getByPlaceholder('Repetí tu contraseña').fill(password);

  if (gender) {
    await page.locator('select[formControlName="gender"]').selectOption(gender);
  }

  await page.getByRole('button', { name: 'Crear cuenta' }).click();

  try {
    await page.waitForURL('**/pokedex', { timeout: 8_000 });
  } catch {
    const errText = await page.locator('.register__error').textContent().catch(() => null);
    if (errText && errText.trim().length > 0) {
      if (errText.toLowerCase().includes('already taken') || errText.toLowerCase().includes('ya existe')) {
        await loginUser(page, username, password);
        return;
      }
      throw new Error(`Registration failed: ${errText}`);
    }
    throw new Error('Registration: no redirect to pokedex and no error shown');
  }
}

export async function loginUser(
  page: Page,
  username: string,
  password: string,
): Promise<void> {
  await page.goto('/login');
  await page.waitForSelector('app-login-page');

  await page.getByPlaceholder('Tu nombre de usuario').fill(username);
  await page.getByPlaceholder('Tu contraseña').fill(password);

  await page.getByRole('button', { name: 'Ingresar' }).click();

  try {
    await page.waitForURL('**/pokedex', { timeout: 8_000 });
  } catch {
    const errText = await page.locator('.login__error').textContent().catch(() => null);
    throw new Error(`Login failed: ${errText || 'No error message shown'}`);
  }
}

export async function logoutUser(page: Page): Promise<void> {
  await page.locator('button.auth-section__logout').click();
  await page.waitForURL((url) => url.pathname === '/login', { timeout: 10_000 });
}

export async function createDeckViaRandom(page: Page, deckName: string): Promise<void> {
  await page.goto('/deck-builder');
  await page.waitForSelector('app-deck-builder-page');

  // Try empty-state button first, then header button
  const nuevoBtn = page.getByRole('button', { name: /Nuevo mazo/ });
  if (await nuevoBtn.isVisible({ timeout: 2_000 }).catch(() => false)) {
    await nuevoBtn.click();
  } else {
    await page.locator('.deck-list__header app-button').filter({ hasText: '+ Nuevo' }).click();
  }

  // Wait for editor
  await page.waitForSelector('input.editor__name', { timeout: 5_000 });

  await page.locator('input.editor__name').clear();
  await page.locator('input.editor__name').fill(deckName);

  // Click "Al Azar" to auto-fill 60 cards
  await page.locator('button.editor__random-btn').click();

  // Wait briefly for random fill
  await page.waitForTimeout(2_000);

  // Click "Guardar mazo"
  const saveBtn = page.locator('button.editor__save-btn');
  await expect(saveBtn).not.toBeDisabled({ timeout: 5_000 });
  await saveBtn.click();

  // Should go back to list mode
  await page.waitForSelector('.deck-list__item', { timeout: 10_000 });
}

export async function equipDeck(page: Page, deckName: string): Promise<void> {
  const deckItem = page.locator('li.deck-list__item', { hasText: deckName });
  await expect(deckItem).toBeVisible({ timeout: 5_000 });

  const equipBtn = deckItem.locator('button.deck-list__equip-btn').first();
  await equipBtn.click();

  // onUseInMatch calls onSelectDeck which navigates to edit mode,
  // so the button disappears. Verify via localStorage instead.
  await page.waitForTimeout(800);

  const playerId = await page.evaluate(() => localStorage.getItem('playerId'));
  expect(playerId).toBeTruthy();

  const storedDeckId = await page.evaluate(
    (key) => localStorage.getItem(key),
    `deck-for-${playerId}`,
  );
  expect(storedDeckId).toBeTruthy();
}

export async function createAndJoinMatch(
  pageCreator: Page,
  pageJoiner: Page,
): Promise<string> {
  // Creator goes to lobby and creates match
  await pageCreator.goto('/lobby');
  await pageCreator.waitForSelector('app-lobby-page');

  await pageCreator.getByRole('button', { name: /Crear partida/ }).click();
  await pageCreator.waitForSelector('.dialog-overlay', { timeout: 10_000 });

  // Select first deck in dialog
  const deckOption = pageCreator.locator('button.deck-option').first();
  await expect(deckOption).toBeVisible({ timeout: 5_000 });
  await deckOption.click();

  await pageCreator.locator('.dialog-overlay').getByRole('button', { name: 'Crear Partida' }).click();

  // Dialog shows success and redirects to /game/:matchId after 1.5s
  await pageCreator.waitForURL('**/game/**', { timeout: 15_000 });
  const matchId = pageCreator.url().split('/').pop() ?? '';

  // Joiner goes to lobby, sees match, joins
  await pageJoiner.goto('/lobby');
  await pageJoiner.waitForSelector('section[aria-label="Partidas disponibles"]');

  const joinBtn = pageJoiner.locator('article.match app-button').filter({ hasText: 'Unirse' }).first();
  await expect(joinBtn).toBeVisible({ timeout: 10_000 });
  await joinBtn.click();

  // Wait for join - page may redirect to /game/:matchId
  await pageJoiner.waitForURL('**/game/**', { timeout: 10_000 });

  return matchId;
}
