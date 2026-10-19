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
  document.querySelectorAll('[data-notification-id]').forEach(item => item.addEventListener('click', () => {
    const token = document.querySelector('input[name="_csrf"]')?.value;
    fetch('/api/notifications/' + item.dataset.notificationId + '/read', { method: 'POST', headers: token ? { 'X-CSRF-TOKEN': token } : {} }).catch(() => {});
  }));
})();
