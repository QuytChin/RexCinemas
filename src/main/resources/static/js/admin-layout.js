document.addEventListener('DOMContentLoaded', () => {
    const body = document.body;
    const toggle = document.querySelector('.admin-sidebar-toggle');
    const close = document.querySelector('.admin-sidebar-close');
    const backdrop = document.querySelector('.admin-sidebar-backdrop');
    const links = document.querySelectorAll('[data-admin-route]');

    const iconForAction = (label) => {
        const text = label.trim().toLocaleLowerCase('vi');
        if (/^(thêm|tạo)/.test(text)) return 'plus';
        if (/^sửa/.test(text)) return 'pencil';
        if (/^lưu/.test(text)) return 'save';
        if (/^(xóa)/.test(text)) return 'trash-2';
        if (/^(ẩn|khóa)/.test(text)) return 'eye-off';
        if (/^(hiện|bật|mở)/.test(text)) return 'eye';
        if (/^(hủy|quay lại|reset)/.test(text)) return 'x';
        return null;
    };

    const addActionIcons = () => {
        document.querySelectorAll('main .btn, main .admin-sidebar-footer button').forEach((element) => {
            if (element.querySelector('svg') || element.dataset.iconified === 'true') return;
            const iconName = iconForAction(element.textContent || '');
            if (!iconName) return;
            const icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            icon.setAttribute('class', 'ui-icon');
            icon.setAttribute('aria-hidden', 'true');
            const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
            use.setAttribute('href', `/images/icons.svg#${iconName}`);
            icon.append(use);
            element.prepend(icon);
            element.classList.add('ui-btn');
            element.dataset.iconified = 'true';
        });
    };

    addActionIcons();
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
        link.classList.remove('active');
        link.removeAttribute('aria-current');
        link.addEventListener('click', () => setOpen(false));
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
