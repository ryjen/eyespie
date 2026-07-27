const { test, expect } = require('@playwright/test');

const SITE_URL = process.env.PUBLIC_SITE_URL || 'http://127.0.0.1:4173/';

test('public page loads without runtime errors and resolves local anchors', async ({ page }) => {
  const runtimeErrors = [];
  page.on('pageerror', (error) => runtimeErrors.push(error.message));
  page.on('console', (message) => {
    if (message.type() === 'error') {
      runtimeErrors.push(message.text());
    }
  });

  await page.goto(SITE_URL, { waitUntil: 'networkidle' });

  await expect(page).toHaveTitle(/Eyespie/);
  await expect(page.getByRole('heading', { level: 1 })).toContainText('Look closer');
  await expect(page.getByRole('link', { name: 'View on GitHub' })).toHaveAttribute(
    'href',
    'https://github.com/ryjen/eyespie',
  );

  const missingTargets = await page.locator('a[href^="#"]').evaluateAll((links) =>
    links
      .map((link) => link.getAttribute('href'))
      .filter((href) => href && href.length > 1)
      .filter((href) => !document.querySelector(href)),
  );

  expect(missingTargets).toEqual([]);
  expect(runtimeErrors).toEqual([]);
});

test('mobile navigation is keyboard-operable and does not overflow', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 });
  await page.goto(SITE_URL, { waitUntil: 'networkidle' });

  const toggle = page.getByRole('button', { name: 'Toggle navigation' });
  const navigation = page.getByRole('navigation', { name: 'Primary navigation' });

  await expect(toggle).toBeVisible();
  await expect(toggle).toHaveAttribute('aria-expanded', 'false');
  await expect(navigation).toBeHidden();

  await toggle.click();
  await expect(toggle).toHaveAttribute('aria-expanded', 'true');
  await expect(navigation).toBeVisible();

  await page.keyboard.press('Escape');
  await expect(toggle).toHaveAttribute('aria-expanded', 'false');
  await expect(toggle).toBeFocused();
  await expect(navigation).toBeHidden();

  const overflows = await page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
  );
  expect(overflows).toBe(false);
});

test('primary navigation remains available without JavaScript', async ({ browser }) => {
  const context = await browser.newContext({
    javaScriptEnabled: false,
    viewport: { width: 375, height: 812 },
  });
  const page = await context.newPage();

  await page.goto(SITE_URL, { waitUntil: 'domcontentloaded' });

  await expect(page.getByRole('navigation', { name: 'Primary navigation' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Toggle navigation' })).toBeHidden();

  await context.close();
});
