/* Context-specific artwork when an uploaded image is absent or its URL returns an error. */
(() => {
    'use strict';

    const artwork = {
        movie: '/images/fallbacks/movie.webp',
        article: '/images/fallbacks/article.webp',
        event: '/images/fallbacks/event.webp',
        combo: '/images/fallbacks/combo.webp',
        media: '/images/fallbacks/media.webp',
        avatar: '/images/fallbacks/avatar.webp'
    };

    function categoryFor(img) {
        if (img.dataset.imageFallback && artwork[img.dataset.imageFallback]) {
            return img.dataset.imageFallback;
        }
        const src = img.getAttribute('src') || '';
        if (/\/images\/movies\//.test(src) || img.matches('.detail-poster') ||
            img.closest('.rex-movie-card, .movie-show-poster, .hero-poster-wrap, .booking-movie-bar, .sidebar-movie, .rex-ticket-movie')) return 'movie';
        if (/\/images\/articles\//.test(src) || img.closest('.article-card, .article-detail')) return 'article';
        if (/\/images\/events\//.test(src) || img.closest('.event-card')) return 'event';
        if (/\/images\/combos\//.test(src) || img.closest('.snack-card-v3, .combo-thumb, .combo-admin-card')) return 'combo';
        if (/\/images\/media\//.test(src) || img.closest('.media-card')) return 'media';
        if (img.closest('.avatar-wrap')) return 'avatar';
        return null;
    }

    function applyFallback(img) {
        if (img.dataset.fallbackApplied === 'true') return;
        const category = categoryFor(img);
        if (!category) return; // Never replace payment/check-in QR codes or site logos.
        img.dataset.fallbackApplied = 'true';
        img.classList.add('image-fallback-used');
        img.src = artwork[category];
    }

    document.addEventListener('error', (event) => {
        if (event.target instanceof HTMLImageElement) applyFallback(event.target);
    }, true);

    function inspect() {
        document.querySelectorAll('img').forEach((img) => {
            if (!categoryFor(img)) return;
            if (!img.getAttribute('src')?.trim() || (img.complete && img.naturalWidth === 0)) {
                applyFallback(img);
            }
        });
    }
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', inspect);
    else inspect();
})();
