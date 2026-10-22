import { test, expect } from '@playwright/test';
import { registerUser, loginUser, logoutUser } from './helpers';

const TEST_USER = `et-${Date.now()}`;
const TEST_PASS = 'Test1234';

test.describe('Auth - Registro y Login', () => {
  test('registrar un usuario nuevo y hacer login', async ({ page }) => {
    await registerUser(page, TEST_USER, TEST_PASS, 'MALE');

    // Verify redirect to pokedex (authenticated)
    expect(page.url()).toContain('/pokedex');

    // Navbar debe mostrar el username
    await expect(page.locator('span.auth-section__user')).toContainText(TEST_USER);
  });

  test('no permitir login con credenciales inválidas', async ({ page }) => {
    await page.goto('/login');

    await page.getByPlaceholder('Tu nombre de usuario').fill('usuario-que-no-existe-98765');
    await page.getByPlaceholder('Tu contraseña').fill('pass-que-no-existe');
    await page.getByRole('button', { name: 'Ingresar' }).click();

    // Debe mostrar error
    await expect(page.locator('.login__error')).toBeVisible({ timeout: 5_000 });
  });

  test('no permitir registro con contraseña corta', async ({ page }) => {
    await page.goto('/register');

    await page.getByPlaceholder('Elegí un nombre de usuario').fill('test-short-pass');
    await page.getByPlaceholder('Mínimo 8 caracteres').fill('123');
    await page.getByPlaceholder('Repetí tu contraseña').fill('123');

    await page.getByRole('button', { name: 'Crear cuenta' }).click();

    // No debe redirigir (sigue en register)
    expect(page.url()).toContain('/register');
  });

  test('hacer logout y redirigir a login', async ({ page }) => {
    await registerUser(page, `${TEST_USER}-2`, TEST_PASS);

    await logoutUser(page);

    expect(page.url()).toContain('/login');
  });

  test('rutas protegidas redirigen a login si no hay sesión', async ({ page }) => {
    await page.goto('/deck-builder');
    await page.waitForURL((url) => url.pathname === '/login', { timeout: 5_000 });
    expect(page.url()).toContain('/login');
  });
});
