const { test, expect } = require('@playwright/test');

const USERNAME = process.env.E2E_USERNAME || 'admin';
const PASSWORD = process.env.E2E_PASSWORD || '123456';

test('portal login and core pages should work', async ({ page, baseURL }) => {
  await page.goto(`${baseURL}/#/home`);

  await expect(page.getByText('欢迎访问高校大型仪器共享平台')).toBeVisible();
  await page.getByText('用户登录').click();

  await expect(page).toHaveURL(/#\/login$/);
  await page.getByRole('textbox', { name: '*用户名' }).fill(USERNAME);
  await page.getByRole('textbox', { name: '*密码' }).fill(PASSWORD);
  await page.getByRole('button', { name: '登录系统' }).click();

  await expect(page).toHaveURL(/#\/home$/);
  await expect(page.getByText('登录成功')).toBeVisible();

  await page.getByText('预约服务', { exact: true }).click();
  await expect(page).toHaveURL(/#\/instruments/);
  await expect(page.getByText('高效液相色谱仪')).toBeVisible();

  await page.getByRole('button', { name: '立即预约' }).first().click();
  await expect(page).toHaveURL(/#\/instruments\/\d+/);
  await expect(page.getByText('开放与预约说明')).toBeVisible();
  await expect(page.getByText('预约办理')).toBeVisible();

  await page.getByText('管理平台').click();
  await expect(page).toHaveURL(/#\/admin$/);
  await expect(page.getByText('管理平台')).toBeVisible();

  await page.getByText('开放规则').click();
  await expect(page).toHaveURL(/#\/admin\/open-rules$/);
  await expect(page.getByText('新增规则')).toBeVisible();
});

