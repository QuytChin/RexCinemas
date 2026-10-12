document.addEventListener('DOMContentLoaded', () => {
    const body = document.body;
    const toggle = document.querySelector('.admin-sidebar-toggle');
    const close = document.querySelector('.admin-sidebar-close');
    const backdrop = document.querySelector('.admin-sidebar-backdrop');
    const links = document.querySelectorAll('[data-admin-route]');

    if (!toggle) return;

    const setOpen = (open) => {
        body.classList.toggle('admin-sidebar-open', open);
        toggle.setAttribute('aria-expanded', String(open));
    };

    toggle.addEventListener('click', () => setOpen(!body.classList.contains('admin-sidebar-open')));
    close?.addEventListener('click', () => setOpen(false));
    backdrop?.addEventListener('click', () => setOpen(false));
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') setOpen(false);
    });

    const currentPath = window.location.pathname.replace(/\/$/, '') || '/';
    let activeLink = null;
    links.forEach((link) => {
        const route = link.dataset.adminRoute;
        const matches = route === '/admin'
            ? currentPath === route
            : currentPath === route || currentPath.startsWith(`${route}/`);
        if (matches && (!activeLink || route.length > activeLink.dataset.adminRoute.length)) {
            activeLink = link;
        }
    });
    if (activeLink) {
        activeLink.classList.add('active');
        activeLink.setAttribute('aria-current', 'page');
    }
});
