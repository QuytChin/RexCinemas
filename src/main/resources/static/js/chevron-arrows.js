document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('a, button').forEach((element) => {
        if (element.dataset.chevronsNormalized === 'true') return;

        if (element.classList.contains('js-trailer-trigger') && !element.classList.contains('media-card')) {
            element.querySelectorAll('.ui-icon use').forEach((use) => {
                use.setAttribute('href', '/images/icons.svg#play');
            });
        }

        const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT);
        const textNodes = [];
        let node;
        while ((node = walker.nextNode())) textNodes.push(node);

        textNodes.forEach((textNode) => {
            const value = textNode.nodeValue || '';
            const glyphPattern = element.classList.contains('js-trailer-trigger') && !element.classList.contains('media-card')
                ? /[←→▶]/g
                : /[←→]/g;
            if (!glyphPattern.test(value)) return;
            glyphPattern.lastIndex = 0;

            const fragment = document.createDocumentFragment();
            let cursor = 0;
            value.replace(glyphPattern, (arrow, index) => {
                fragment.append(document.createTextNode(value.slice(cursor, index)));

                const icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                icon.setAttribute('class', 'ui-icon ui-chevron');
                icon.setAttribute('aria-hidden', 'true');
                const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
                const iconName = arrow === '←' ? 'chevron-left' : (arrow === '→' ? 'chevron-right' : 'play');
                use.setAttribute('href', `/images/icons.svg#${iconName}`);
                icon.append(use);
                fragment.append(icon);
                cursor = index + 1;
                return arrow;
            });
            fragment.append(document.createTextNode(value.slice(cursor)));
            textNode.replaceWith(fragment);
        });

        element.dataset.chevronsNormalized = 'true';
    });
});
