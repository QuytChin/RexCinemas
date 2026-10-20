(() => {
  const roots = [...document.querySelectorAll('[data-header-popover]')];
  const closeAll = except => roots.forEach(root => {
    if (root === except) return;
    const toggle = root.querySelector('[data-popover-toggle]');
    const panel = root.querySelector('.header-popover');
    if (toggle && panel) { toggle.setAttribute('aria-expanded', 'false'); panel.hidden = true; }
  });
  roots.forEach(root => {
    const toggle = root.querySelector('[data-popover-toggle]');
    const panel = root.querySelector('.header-popover');
    if (!toggle || !panel) return;
    toggle.addEventListener('click', () => {
      const willOpen = panel.hidden; closeAll(root); panel.hidden = !willOpen; toggle.setAttribute('aria-expanded', String(willOpen));
    });
  });
  document.addEventListener('click', event => { if (!roots.some(root => root.contains(event.target))) closeAll(); });
  document.addEventListener('keydown', event => { if (event.key === 'Escape') { closeAll(); document.querySelector('[data-popover-toggle][aria-expanded="true"]')?.focus(); } });
  const csrfToken = () => document.cookie.split('; ').find(value => value.startsWith('XSRF-TOKEN='))
    ?.split('=').slice(1).join('=');
  const updateUnreadCount = unreadCount => {
    const count = Math.max(0, Number(unreadCount) || 0);
    document.querySelectorAll('[data-notification-badge]').forEach(badge => {
      badge.textContent = count > 9 ? '9+' : String(count);
      badge.hidden = count === 0;
    });
    document.querySelectorAll('[data-notification-unread-summary]').forEach(summary => {
      summary.textContent = `${count} chưa đọc`;
      summary.hidden = count === 0;
    });
    document.querySelectorAll('[data-mark-all-notifications]').forEach(button => { button.hidden = count === 0; });
  };
  const markRead = async path => {
    const token = csrfToken();
    const response = await fetch(path, {
      method: 'POST', credentials: 'same-origin',
      headers: { Accept: 'application/json', ...(token ? { 'X-XSRF-TOKEN': decodeURIComponent(token) } : {}) }
    });
    if (!response.ok) throw new Error(`Notification update failed: ${response.status}`);
    const body = await response.json();
    if (!Number.isFinite(Number(body.unreadCount))) throw new Error('Notification response is invalid');
    updateUnreadCount(body.unreadCount);
  };
  document.querySelectorAll('[data-notification-id]').forEach(item => item.addEventListener('click', async event => {
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    if (item.dataset.reading) { event.preventDefault(); return; }
    event.preventDefault(); item.dataset.reading = 'true'; item.setAttribute('aria-busy', 'true');
    try {
      await markRead('/api/notifications/' + encodeURIComponent(item.dataset.notificationId) + '/read');
      item.classList.remove('is-unread');
      window.location.assign(item.href);
    } catch (_) {
      item.classList.add('notification-read-failed');
      item.removeAttribute('aria-busy');
      delete item.dataset.reading;
    }
  }));
  document.querySelectorAll('[data-mark-all-notifications]').forEach(button => button.addEventListener('click', async () => {
    if (button.dataset.reading) return;
    button.dataset.reading = 'true'; button.disabled = true;
    try {
      await markRead('/api/notifications/read-all');
      document.querySelectorAll('.notification-item.is-unread').forEach(item => item.classList.remove('is-unread'));
    } catch (_) {
      button.disabled = false;
    } finally {
      delete button.dataset.reading;
    }
  }));
})();
