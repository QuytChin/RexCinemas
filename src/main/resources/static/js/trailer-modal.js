(function () {
    'use strict';

    const VIDEO_ID_PATTERN = /^[A-Za-z0-9_-]{11}$/;
    const YOUTUBE_HOSTS = new Set(['youtube.com', 'www.youtube.com', 'm.youtube.com', 'youtu.be', 'www.youtu.be']);
    const modal = document.getElementById('trailerModal');
    if (!modal) return;

    const frame = document.getElementById('trailerModalFrame');
    const title = document.getElementById('trailerModalTitle');
    const message = document.getElementById('trailerModalMessage');
    const closeButton = modal.querySelector('.trailer-modal-close');
    let lastFocusedElement = null;
    let lockedScrollY = 0;

    function getYouTubeVideoId(value) {
        if (!value || typeof value !== 'string') return null;
        let url;
        try {
            url = new URL(value.trim());
        } catch (error) {
            return null;
        }
        const host = url.hostname.toLowerCase();
        if (!['http:', 'https:'].includes(url.protocol) || !YOUTUBE_HOSTS.has(host)) return null;

        let id = null;
        if (host.includes('youtu.be')) {
            id = url.pathname.split('/').filter(Boolean)[0];
        } else if (url.pathname === '/watch') {
            id = url.searchParams.get('v');
        } else if (/^\/(shorts|embed)\//.test(url.pathname)) {
            id = url.pathname.split('/').filter(Boolean)[1];
        }
        return id && VIDEO_ID_PATTERN.test(id) ? id : null;
    }

    function lockPageScroll() {
        lockedScrollY = window.scrollY;
        document.body.style.top = `-${lockedScrollY}px`;
        document.body.classList.add('trailer-modal-open');
    }

    function unlockPageScroll() {
        document.body.classList.remove('trailer-modal-open');
        document.body.style.top = '';
        window.scrollTo(0, lockedScrollY);
    }

    function openModal(trigger) {
        lastFocusedElement = document.activeElement;
        const videoId = getYouTubeVideoId(trigger.getAttribute('data-trailer-url'));
        title.textContent = trigger.getAttribute('data-trailer-title') || 'Xem Trailer';
        frame.hidden = !videoId;
        message.hidden = Boolean(videoId);
        if (videoId) {
            const embedUrl = `https://www.youtube.com/embed/${encodeURIComponent(videoId)}?autoplay=1&rel=0&playsinline=1`;
            frame.src = embedUrl;
            frame.title = `${title.textContent} - Trailer`;
        } else {
            frame.src = 'about:blank';
        }
        modal.hidden = false;
        modal.setAttribute('aria-hidden', 'false');
        lockPageScroll();
        closeButton.focus();
    }

    function closeModal() {
        if (modal.hidden) return;
        frame.src = 'about:blank';
        modal.hidden = true;
        modal.setAttribute('aria-hidden', 'true');
        unlockPageScroll();
        if (lastFocusedElement && typeof lastFocusedElement.focus === 'function') lastFocusedElement.focus();
    }

    function trapFocus(event) {
        if (modal.hidden || event.key !== 'Tab') return;
        const focusable = [closeButton, frame].filter((element) => !element.hidden && !element.disabled);
        if (!focusable.length) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    }

    document.addEventListener('click', function (event) {
        const trigger = event.target.closest('.js-trailer-trigger');
        if (trigger) {
            event.preventDefault();
            openModal(trigger);
            return;
        }
        if (event.target.closest('[data-trailer-close="true"]')) closeModal();
    });
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') closeModal();
        trapFocus(event);
    });
}());
