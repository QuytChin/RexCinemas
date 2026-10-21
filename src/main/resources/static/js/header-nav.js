(() => {
  const links = document.querySelectorAll('.rex-main-nav [data-rex-nav]');
  if (!links.length) return;

  const updateActiveLink = () => {
    const path = window.location.pathname.replace(/\/+$/, '') || '/';
    let activeKey = null;

    if (path === '/schedule' || path.startsWith('/schedule/')) {
      activeKey = 'schedule';
    } else if (path === '/') {
      activeKey = window.location.hash.slice(1) || null;
    }

    links.forEach((link) => {
      const isActive = link.dataset.rexNav === activeKey;
      link.classList.toggle('active', isActive);
      if (isActive) link.setAttribute('aria-current', 'page');
      else link.removeAttribute('aria-current');
    });
  };

  updateActiveLink();
  window.addEventListener('hashchange', updateActiveLink);
})();
