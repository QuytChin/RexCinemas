// @ts-check
import { test, expect } from '@playwright/test';
import path from 'node:path';

const RENDERER = path.resolve(process.cwd(), 'src/main/resources/static/js/chat-render.js');

/** Payloads that must never become markup and must never run. */
const XSS_PAYLOADS = [
  { name: 'raw script tag', text: '<script>window.__xss = "script"</script>' },
  { name: 'img onerror', text: '<img src=x onerror="window.__xss = \'img\'">' },
  { name: 'svg onload', text: '<svg onload="window.__xss = \'svg\'"></svg>' },
  { name: 'quote breakout in link path', text: '[bấm](https://rex.vn/phim" onmouseover="window.__xss = \'attr\')' },
  { name: 'javascript scheme', text: '[bấm](javascript:window.__xss = "js")' },
  { name: 'mixed-case javascript scheme', text: '[bấm](JaVaScRiPt:window.__xss = "js2")' },
  { name: 'vbscript scheme', text: '[bấm](vbscript:msgbox(1))' },
  { name: 'data url', text: '[bấm](data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==)' },
  { name: 'file url', text: '[bấm](file:///C:/Windows/win.ini)' },
  { name: 'html inside bold', text: '**<img src=x onerror="window.__xss = \'bold\'">**' },
  { name: 'html inside italic', text: '*<img src=x onerror="window.__xss = \'italic\'">*' },
  { name: 'html as link label', text: '[<img src=x onerror="window.__xss = \'label\'">](https://rexcinema.vn)' }
];

async function openHarness(page) {
  await page.setContent('<!doctype html><html lang="vi"><body><div id="host"></div></body></html>');
  await page.addScriptTag({ path: RENDERER });
}

async function renderIntoHost(page, text) {
  await page.evaluate((value) => {
    window.RexChatRender.renderInto(document.getElementById('host'), value);
  }, text);
}

/** Any attribute that starts with "on" would be an event handler. */
async function hostEventAttributes(page) {
  return page.evaluate(() => Array.from(document.querySelectorAll('#host *'))
    .flatMap((el) => Array.from(el.attributes).map((a) => a.name))
    .filter((name) => name.toLowerCase().startsWith('on')));
}

async function hostExecuted(page) {
  return page.evaluate(() => window.__xss ?? null);
}

test.describe('REX-A03 — support chat XSS', () => {
  for (const payload of XSS_PAYLOADS) {
    test(`message with ${payload.name} stays inert`, async ({ page }) => {
      const dialogs = [];
      page.on('dialog', async (dialog) => { dialogs.push(dialog.message()); await dialog.dismiss(); });
      await openHarness(page);

      await renderIntoHost(page, payload.text);
      await page.waitForTimeout(150);

      expect(await hostExecuted(page)).toBeNull();
      expect(dialogs).toEqual([]);
      expect(await page.locator('#host script').count()).toBe(0);
      expect(await page.locator('#host img').count()).toBe(0);
      expect(await page.locator('#host svg').count()).toBe(0);
      expect(await hostEventAttributes(page)).toEqual([]);
    });
  }

  test('link with a quote breakout is refused instead of becoming a link', async ({ page }) => {
    await openHarness(page);
    await renderIntoHost(page, '[bấm](https://rex.vn/phim" onmouseover="window.__xss = \'attr\')');
    expect(await page.evaluate(() => window.__xss ?? null)).toBeNull();
    await expect(page.locator('#host a')).toHaveCount(0);
    await expect(page.locator('#host')).toContainText('onmouseover');
    expect(await hostEventAttributes(page)).toEqual([]);
  });

  test('unsafe schemes are shown as plain text instead of links', async ({ page }) => {
    await openHarness(page);
    await renderIntoHost(page, '[bấm](javascript:window.__xss = "js")');
    await expect(page.locator('#host a')).toHaveCount(0);
    await expect(page.locator('#host')).toContainText('javascript:');
  });

  test('safeUrl rejects every executable scheme and accepts http/https', async ({ page }) => {
    await openHarness(page);
    const result = await page.evaluate(() => {
      const rejected = [
        'javascript:alert(1)',
        'JaVaScRiPt:alert(1)',
        'java\nscript:alert(1)',
        'java\tscript:alert(1)',
        '  javascript:alert(1)  ',
        'data:text/html,<script>alert(1)</script>',
        'vbscript:msgbox(1)',
        'file:///C:/Windows/win.ini',
        'blob:https://rexcinema.vn/uuid',
        'https://rex.vn/phim" onmouseover="alert(1)',
        "https://rex.vn/phim' onmouseover='alert(1)",
        'https://rex.vn/phim onmouseover=alert(1)',
        '',
        '   '
      ].map((value) => window.RexChatRender.safeUrl(value));
      const accepted = [
        'https://rexcinema.vn/phim',
        'http://rexcinema.vn/ve.pdf',
        '/phim/dune'
      ].map((value) => window.RexChatRender.safeUrl(value));
      return { rejected, accepted };
    });
    expect(result.rejected).toEqual([null, null, null, null, null, null, null, null, null, null, null, null, null, null]);
    expect(result.accepted[0]).toBe('https://rexcinema.vn/phim');
    expect(result.accepted[1]).toBe('http://rexcinema.vn/ve.pdf');
    expect(result.accepted[2]).toBe('https://rexcinema.vn/phim/dune');
  });

  test('http and https links still work', async ({ page }) => {
    await openHarness(page);
    await renderIntoHost(page, '[Rex](https://rexcinema.vn/phim) và [Tải](http://rexcinema.vn/ve.pdf)');
    const links = page.locator('#host a');
    await expect(links).toHaveCount(2);
    await expect(links.nth(0)).toHaveAttribute('href', 'https://rexcinema.vn/phim');
    await expect(links.nth(1)).toHaveAttribute('href', 'http://rexcinema.vn/ve.pdf');
    await expect(links.nth(0)).toHaveAttribute('rel', 'noopener noreferrer');
    await expect(links.nth(0)).toHaveAttribute('target', '_blank');
    await expect(links.nth(0)).toHaveClass('rex-chat-link');
  });

  test('Vietnamese text and chat markdown formatting are preserved', async ({ page }) => {
    await openHarness(page);
    await renderIntoHost(page, 'Xin chào **Rex** — phim *Hay* lắm!\n- Dòng một\n- Dòng hai');

    await expect(page.locator('#host strong')).toHaveText('Rex');
    await expect(page.locator('#host em')).toHaveText('Hay');
    await expect(page.locator('#host br')).toHaveCount(2);
    expect(await page.locator('#host').innerText()).toBe(
      'Xin chào Rex — phim Hay lắm!\n• Dòng một\n• Dòng hai'
    );
  });

  test('re-rendering a bubble replaces content instead of appending', async ({ page }) => {
    await openHarness(page);
    await renderIntoHost(page, 'tin nhắn cũ');
    await renderIntoHost(page, 'tin nhắn mới');
    await expect(page.locator('#host')).toHaveText('tin nhắn mới');
  });

  test('history payload from MySQL renders as text', async ({ page }) => {
    await openHarness(page);
    await page.evaluate(() => {
      const history = [
        { id: 1, message: 'Chào shop, mình muốn đổi vé', senderType: 'CUSTOMER' },
        { id: 2, message: '<img src=x onerror="window.__xss = \'history\'">', senderType: 'CUSTOMER' },
        { id: 3, message: 'Dạ em hỗ trợ ngay ạ', senderType: 'STAFF' }
      ];
      const host = document.getElementById('host');
      history.forEach((msg) => {
        const bubble = document.createElement('div');
        window.RexChatRender.renderInto(bubble, msg.message);
        host.appendChild(bubble);
      });
    });
    await page.waitForTimeout(150);
    expect(await hostExecuted(page)).toBeNull();
    expect(await page.locator('#host img').count()).toBe(0);
    expect(await page.locator('#host').innerText()).toContain('Chào shop, mình muốn đổi vé');
    expect(await page.locator('#host').innerText()).toContain('Dạ em hỗ trợ ngay ạ');
  });

  test('WebSocket message frame is rendered safely', async ({ page }) => {
    await openHarness(page);
    await page.evaluate(() => {
      const frame = {
        id: 7,
        sessionId: 'user_1',
        senderType: 'CUSTOMER',
        senderName: '<img src=x onerror="window.__xss = \'sender\'">',
        message: '<img src=x onerror="window.__xss = \'body\'"> [x](javascript:window.__xss="link")',
        createdAt: '10:00 30/10/2026'
      };
      const bubble = document.createElement('div');
      window.RexChatRender.renderInto(bubble, frame.message);
      const sender = document.createElement('div');
      sender.textContent = frame.senderName;
      document.getElementById('host').append(sender, bubble);
    });
    await page.waitForTimeout(150);
    expect(await hostExecuted(page)).toBeNull();
    expect(await page.locator('#host img').count()).toBe(0);
    expect(await page.locator('#host a').count()).toBe(0);
  });

  test('AI markdown answer with links renders safely', async ({ page }) => {
    await openHarness(page);
    await renderIntoHost(page,
      'Gợi ý cho bạn: **[Dune: Hành Tinh Cát](https://rexcinema.vn/phim/dune)**\n- Suất 19:30\n- Suất 21:45');
    await expect(page.locator('#host strong a')).toHaveCount(1);
    await expect(page.locator('#host strong')).toContainText('Dune: Hành Tinh Cát');
    expect(await page.locator('#host').innerText()).toContain('• Suất 19:30');
  });

  test('admin conversation row escapes every customer supplied field', async ({ page }) => {
    await openHarness(page);
    await page.evaluate(() => {
      const item = window.RexChatRender.createConversationItem({
        sessionId: 'user_1',
        customerName: '<img src=x onerror="window.__xss = \'conv\'">',
        lastMessage: '<img src=x onerror="window.__xss = \'preview\'">',
        cinemaName: '<svg onload="window.__xss = \'cinema\'"></svg>',
        unreadCount: 3
      }, { timeLabel: '10:00' });
      document.getElementById('host').appendChild(item);
    });
    await page.waitForTimeout(150);

    expect(await hostExecuted(page)).toBeNull();
    expect(await page.locator('#host img').count()).toBe(0);
    expect(await page.locator('#host svg').count()).toBe(0);
    await expect(page.locator('#host .admin-chat-name span').first())
      .toHaveText('<img src=x onerror="window.__xss = \'conv\'">');
    await expect(page.locator('#host .admin-chat-preview'))
      .toHaveText('<img src=x onerror="window.__xss = \'preview\'">');
    await expect(page.locator('#host .admin-chat-cinema-badge'))
      .toHaveText('<svg onload="window.__xss = \'cinema\'"></svg>');
    await expect(page.locator('#host .admin-chat-unread-badge')).toHaveText('3');
  });

  test('admin conversation row keeps its selection contract', async ({ page }) => {
    await openHarness(page);
    await page.evaluate(() => {
      let selected = null;
      const item = window.RexChatRender.createConversationItem(
        { sessionId: 'user_42', cinemaId: 9, customerName: 'Nguyễn Văn A', lastMessage: 'Xin chào', cinemaName: 'Rex Q1', unreadCount: 1 },
        { active: true, timeLabel: '10:00', onSelect: (el) => { selected = el.dataset.sessionId; } }
      );
      document.getElementById('host').appendChild(item);
      item.click();
      window.Selected = selected;
    });
    await expect(page.locator('#host .admin-chat-item')).toHaveClass(/active/);
    await expect(page.locator('#host .admin-chat-item')).toHaveAttribute('data-session-id', 'user_42');
    await expect(page.locator('#host .admin-chat-item')).toHaveAttribute('data-cinema-id', '9');
    await expect(page.locator('#host .admin-chat-avatar')).toHaveText('N');
    await expect(page.locator('#host .admin-chat-time')).toHaveText('10:00');
    await expect(page.locator('#host .admin-chat-name span').first()).toHaveText('Nguyễn Văn A');
    expect(await page.evaluate(() => window.Selected)).toBe('user_42');
  });
});