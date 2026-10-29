/**
 * RexCinemas - Safe renderer for support chat content.
 *
 * Chat text is untrusted: it is written by customers, by staff and by an external
 * LLM. Every helper here therefore builds DOM nodes with textContent and never
 * turns message text into an HTML string. Markdown support is limited to the
 * inline syntax the chat actually uses: **bold**, *italic*, [label](url),
 * "- " bullets and line breaks.
 */
(function (global) {
    'use strict';

    const ALLOWED_PROTOCOLS = ['http:', 'https:'];

    /**
     * Resolves a Markdown link target and rejects every scheme that can execute
     * code or smuggle a document (javascript:, data:, vbscript:, file:, ...).
     * The WHATWG parser also normalises sneaky forms such as "java\nscript:".
     */
    function resolveSafeUrl(rawUrl) {
        if (rawUrl === null || rawUrl === undefined) return null;
        const value = String(rawUrl).trim();
        if (!value) return null;
        // A quote, angle bracket, backtick or whitespace inside a Markdown target means
        // the text is malformed or tried to break out of the old href attribute.
        if (/[\s"'`<>]/.test(value)) return null;
        const location = global.location;
        const base = location && /^https?:$/.test(location.protocol) ? location.href : 'https://rexcinema.vn/';
        let parsed;
        try {
            parsed = new URL(value, base);
        } catch (e) {
            return null;
        }
        return ALLOWED_PROTOCOLS.indexOf(parsed.protocol) >= 0 ? parsed.href : null;
    }

    function linkNode(label, rawUrl) {
        const href = resolveSafeUrl(rawUrl);
        if (!href) {
            // Unsafe or malformed targets stay visible as inert text.
            return document.createTextNode('[' + label + '](' + rawUrl + ')');
        }
        const anchor = document.createElement('a');
        anchor.className = 'rex-chat-link';
        anchor.href = href;
        anchor.target = '_blank';
        anchor.rel = 'noopener noreferrer';
        anchor.textContent = label || href;
        return anchor;
    }

    function appendInline(parent, text) {
        if (!text) return;
        // A fresh pattern per call keeps recursion from corrupting lastIndex.
        const pattern = /\*\*([\s\S]+?)\*\*|\*([^*\n]+?)\*|\[([^\]\n]*)\]\(([^)]*)\)/g;
        let lastIndex = 0;
        let match;
        while ((match = pattern.exec(text)) !== null) {
            if (match.index > lastIndex) {
                parent.appendChild(document.createTextNode(text.slice(lastIndex, match.index)));
            }
            if (match[1] !== undefined) {
                const strong = document.createElement('strong');
                appendInline(strong, match[1]);
                parent.appendChild(strong);
            } else if (match[2] !== undefined) {
                const em = document.createElement('em');
                appendInline(em, match[2]);
                parent.appendChild(em);
            } else {
                parent.appendChild(linkNode(match[3], match[4]));
            }
            lastIndex = pattern.lastIndex;
        }
        if (lastIndex < text.length) {
            parent.appendChild(document.createTextNode(text.slice(lastIndex)));
        }
    }

    /** Renders message text into an existing element, replacing its content. */
    function renderInto(element, text) {
        if (!element) return;
        element.textContent = '';
        if (text === null || text === undefined) return;
        const normalized = String(text).replace(/\r\n?/g, '\n');
        if (!normalized) return;
        normalized.split('\n').forEach(function (line, index) {
            if (index > 0) element.appendChild(document.createElement('br'));
            const bullet = /^\s*-\s+([\s\S]*)$/.exec(line);
            if (bullet) {
                element.appendChild(document.createTextNode('\u2022 '));
                appendInline(element, bullet[1]);
            } else {
                appendInline(element, line);
            }
        });
    }

    /** Builds one conversation row without concatenating any HTML. */
    function createConversationItem(conversation, options) {
        const conv = conversation || {};
        const opts = options || {};
        const item = document.createElement('div');
        item.className = 'admin-chat-item' + (opts.active ? ' active' : '');
        if (conv.sessionId) item.setAttribute('data-session-id', conv.sessionId);
        if (conv.cinemaId) item.setAttribute('data-cinema-id', conv.cinemaId);
        if (typeof opts.onSelect === 'function') {
            item.addEventListener('click', function () { opts.onSelect(item); });
        }

        const customerName = conv.customerName || 'Khách vãng lai';

        const avatar = document.createElement('div');
        avatar.className = 'admin-chat-avatar';
        avatar.textContent = String(customerName).substring(0, 1).toUpperCase();

        const meta = document.createElement('div');
        meta.className = 'admin-chat-meta';

        const nameRow = document.createElement('div');
        nameRow.className = 'admin-chat-name';
        const nameEl = document.createElement('span');
        nameEl.textContent = customerName;
        const timeEl = document.createElement('span');
        timeEl.className = 'admin-chat-time';
        timeEl.textContent = opts.timeLabel || '';
        nameRow.appendChild(nameEl);
        nameRow.appendChild(timeEl);

        const preview = document.createElement('div');
        preview.className = 'admin-chat-preview';
        preview.textContent = conv.lastMessage || '';

        const footer = document.createElement('div');
        footer.className = 'd-flex align-items-center justify-content-between';
        const cinemaBadge = document.createElement('span');
        cinemaBadge.className = 'admin-chat-cinema-badge';
        cinemaBadge.textContent = conv.cinemaName || 'Rex Cinemas';
        footer.appendChild(cinemaBadge);
        const unread = Number(conv.unreadCount);
        if (unread > 0) {
            const unreadBadge = document.createElement('span');
            unreadBadge.className = 'admin-chat-unread-badge';
            unreadBadge.textContent = String(unread);
            footer.appendChild(unreadBadge);
        }

        meta.appendChild(nameRow);
        meta.appendChild(preview);
        meta.appendChild(footer);
        item.appendChild(avatar);
        item.appendChild(meta);
        return item;
    }

    global.RexChatRender = {
        renderInto: renderInto,
        createConversationItem: createConversationItem,
        safeUrl: resolveSafeUrl
    };
})(window);