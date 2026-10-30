/**
 * RexCinemas - Admin Real-time Support Chat Manager
 */
(function () {
    'use strict';

    let activeSessionId = null;
    let stompClient = null;
    let cinemaSubscription = null;

    // DOM Elements
    const convList = document.getElementById('adminConvList');
    const searchInput = document.getElementById('adminSearchConv');
    const placeholder = document.getElementById('adminChatPlaceholder');
    const activeHeader = document.getElementById('adminActiveConvHeader');
    const messagesArea = document.getElementById('adminChatMessagesArea');
    const quickReplies = document.getElementById('adminQuickReplies');
    const inputArea = document.getElementById('adminChatInputArea');
    const replyInput = document.getElementById('adminReplyInput');
    const sendBtn = document.getElementById('adminSendReplyBtn');

    const customerNameEl = document.getElementById('adminActiveCustomerName');
    const customerEmailEl = document.getElementById('adminActiveCustomerEmail');
    const cinemaNameEl = document.getElementById('adminActiveCinemaName');

    init();

    function init() {
        initWebSocket();
        bindEvents();
    }

    function initWebSocket() {
        try {
            const socketFactory = () => new SockJS('/ws');
            let client;

            if (window.StompJs && window.StompJs.Client) {
                client = new window.StompJs.Client({
                    webSocketFactory: socketFactory,
                    reconnectDelay: 5000,
                    onConnect: () => onStompConnected(client),
                    onStompError: () => {}
                });
                client.activate();
                stompClient = client;
            } else if (window.Stomp) {
                const rawSocket = socketFactory();
                client = window.Stomp.over(rawSocket);
                client.debug = () => {};
                client.connect({}, () => onStompConnected(client), () => {});
                stompClient = client;
            }
        } catch (e) {
            console.warn('[AdminSupport] WebSocket init skipped:', e);
        }
    }

    function onStompConnected(client) {
        if (cinemaSubscription) {
            try { cinemaSubscription.unsubscribe(); } catch (e) {}
            cinemaSubscription = null;
        }
        const topic = window.CURRENT_CINEMA_ID ? `/topic/support/cinema/${window.CURRENT_CINEMA_ID}` : '/topic/support/admin';
        cinemaSubscription = client.subscribe(topic, message => {
            try {
                const data = JSON.parse(message.body);
                handleIncomingMessage(data);
            } catch (err) {}
        });
    }

    function handleIncomingMessage(msg) {
        // 1. Nếu đang mở cuộc hội thoại này, render tin nhắn ngay
        if (activeSessionId && msg.sessionId === activeSessionId) {
            appendMessageUI(msg);
            scrollToBottom();
            markCurrentAsRead();
        }

        // 2. Cập nhật preview và badge ở cột trái
        refreshConversationList();
    }

    function bindEvents() {
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                const term = e.target.value.toLowerCase().trim();
                const items = convList.querySelectorAll('.admin-chat-item');
                items.forEach(item => {
                    const text = item.textContent.toLowerCase();
                    item.style.display = text.includes(term) ? 'flex' : 'none';
                });
            });
        }

        if (replyInput) {
            replyInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    window.sendAdminReply();
                }
            });
        }
    }

    window.selectConversation = function (el) {
        const sessionId = el.getAttribute('data-session-id');
        if (!sessionId) return;

        activeSessionId = sessionId;

        // Highlight item
        const items = convList.querySelectorAll('.admin-chat-item');
        items.forEach(i => i.classList.remove('active'));
        el.classList.add('active');

        // Xóa badge unread trên item này
        const badge = el.querySelector('.admin-chat-unread-badge');
        if (badge) badge.remove();

        // Nạp thông tin header
        const name = el.querySelector('.admin-chat-name span').textContent;
        const cinema = el.querySelector('.admin-chat-cinema-badge').textContent;
        customerNameEl.textContent = name;
        cinemaNameEl.textContent = cinema;
        customerEmailEl.textContent = 'Mã hội thoại: ' + sessionId;

        // Hiển thị khung chat
        placeholder.style.display = 'none';
        activeHeader.style.display = 'flex';
        messagesArea.style.display = 'flex';
        quickReplies.style.display = 'flex';
        inputArea.style.display = 'flex';

        // Tải lịch sử tin nhắn
        loadSessionHistory(sessionId);
        window.markCurrentAsRead();

        setTimeout(() => replyInput && replyInput.focus(), 150);
    };

    function loadSessionHistory(sessionId) {
        messagesArea.innerHTML = '<div class="text-center text-secondary p-4">Đang tải tin nhắn...</div>';
        fetch(`/admin/support/api/history/${encodeURIComponent(sessionId)}`)
            .then(res => res.json())
            .then(msgs => {
                messagesArea.innerHTML = '';
                if (Array.isArray(msgs)) {
                    msgs.forEach(appendMessageUI);
                }
                scrollToBottom();
            })
            .catch(() => {
                messagesArea.innerHTML = '<div class="text-danger p-3">Lỗi tải lịch sử tin nhắn.</div>';
            });
    }

    window.sendAdminReply = function () {
        if (!activeSessionId) return;
        const text = replyInput.value.trim();
        if (!text) return;

        replyInput.value = '';

        const csrf = getCsrfToken();
        const headers = { 'Content-Type': 'application/json' };
        if (csrf.token) headers[csrf.headerName] = csrf.token;

        fetch('/admin/support/api/reply', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({
                sessionId: activeSessionId,
                message: text
            })
        })
        .then(res => {
            if (!res.ok) throw new Error('Không thể gửi phản hồi. Vui lòng thử lại.');
            return res.json();
        })
        .then(saved => {
            if (saved && saved.id) {
                appendMessageUI(saved);
                scrollToBottom();
                refreshConversationList();
            }
        })
        .catch(() => {
            window.RexNotifications?.toast('Không thể gửi phản hồi. Vui lòng kiểm tra kết nối rồi thử lại.', { type: 'error' });
        });
    };

    window.insertQuickReply = function (text) {
        if (!replyInput) return;
        replyInput.value = text;
        replyInput.focus();
    };

    window.markCurrentAsRead = function () {
        if (!activeSessionId) return;
        const csrf = getCsrfToken();
        const headers = { 'Content-Type': 'application/json' };
        if (csrf.token) headers[csrf.headerName] = csrf.token;

        fetch('/admin/support/api/mark-read', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ sessionId: activeSessionId })
        }).catch(() => {});
    };

    function refreshConversationList() {
        const cinemaParam = window.CURRENT_CINEMA_ID ? `?cinemaId=${window.CURRENT_CINEMA_ID}` : '';
        fetch(`/admin/support/api/conversations${cinemaParam}`)
            .then(res => res.json())
            .then(convs => {
                if (!Array.isArray(convs) || convs.length === 0) return;
                convList.innerHTML = '';
                convs.forEach(c => {
                    const timeStr = c.lastMessageAt ? formatTime(c.lastMessageAt) : '';
                    convList.appendChild(buildConversationItem(c, {
                        active: c.sessionId === activeSessionId,
                        timeLabel: timeStr,
                        onSelect: selectConversation
                    }));
                });
            })
            .catch(() => {});
    }

    function appendMessageUI(msg) {
        if (!msg || !messagesArea) return;
        if (msg.id && messagesArea.querySelector(`[data-msg-id="${msg.id}"]`)) return;

        const group = document.createElement('div');
        const type = (msg.senderType || 'CUSTOMER').toLowerCase();
        // In admin view: staff replies are right-aligned (like my own messages), customer/ai are left-aligned
        const isMe = type === 'staff';
        group.className = `rex-msg-group ${isMe ? 'customer' : 'staff'}`;
        if (msg.id) group.setAttribute('data-msg-id', msg.id);

        let senderLabel = msg.senderName;
        if (isMe) senderLabel = 'Bạn (Rạp)';
        else if (type === 'ai') senderLabel = 'Trợ lý Rex AI 🤖';
        else senderLabel = msg.senderName || 'Khách hàng';

        const senderEl = document.createElement('div');
        senderEl.className = 'rex-msg-sender';
        senderEl.textContent = senderLabel;

        const bubbleEl = document.createElement('div');
        bubbleEl.className = 'rex-msg-bubble';
        renderMessage(bubbleEl, msg.message);

        const timeEl = document.createElement('div');
        timeEl.className = 'rex-msg-time';
        timeEl.textContent = msg.createdAt || '';

        group.appendChild(senderEl);
        group.appendChild(bubbleEl);
        group.appendChild(timeEl);

        messagesArea.appendChild(group);
    }

    function scrollToBottom() {
        setTimeout(() => {
            messagesArea.scrollTop = messagesArea.scrollHeight;
        }, 50);
    }

    function formatTime(iso) {
        if (!iso) return '';
        try {
            const d = new Date(iso);
            const hh = String(d.getHours()).padStart(2, '0');
            const mm = String(d.getMinutes()).padStart(2, '0');
            const dd = String(d.getDate()).padStart(2, '0');
            const mo = String(d.getMonth() + 1).padStart(2, '0');
            return `${hh}:${mm} ${dd}/${mo}`;
        } catch (e) {
            return '';
        }
    }

    // Chat text is untrusted (customer, staff, AI). It is always rendered as DOM
    // text nodes by the shared renderer so it can never become markup.
    function renderMessage(element, text) {
        if (window.RexChatRender) {
            window.RexChatRender.renderInto(element, text);
            return;
        }
        element.textContent = text || '';
    }

    function buildConversationItem(conversation, options) {
        if (window.RexChatRender) return window.RexChatRender.createConversationItem(conversation, options);
        // Fail closed: a minimal text-only row if the shared renderer is missing.
        const conv = conversation || {};
        const opts = options || {};
        const item = document.createElement('div');
        item.className = 'admin-chat-item' + (opts.active ? ' active' : '');
        if (conv.sessionId) item.setAttribute('data-session-id', conv.sessionId);
        if (conv.cinemaId) item.setAttribute('data-cinema-id', conv.cinemaId);
        if (typeof opts.onSelect === 'function') {
            item.addEventListener('click', function () { opts.onSelect(item); });
        }
        const nameRow = document.createElement('div');
        nameRow.className = 'admin-chat-name';
        const nameEl = document.createElement('span');
        nameEl.textContent = conv.customerName || 'Khách vãng lai';
        nameRow.appendChild(nameEl);
        const cinemaBadge = document.createElement('span');
        cinemaBadge.className = 'admin-chat-cinema-badge';
        cinemaBadge.textContent = conv.cinemaName || 'Rex Cinemas';
        item.appendChild(nameRow);
        item.appendChild(cinemaBadge);
        return item;
    }

    function getCsrfToken() {
        const metaToken = document.querySelector('meta[name="_csrf"]');
        const metaHeader = document.querySelector('meta[name="_csrf_header"]');
        if (metaToken && metaHeader) {
            return { token: metaToken.getAttribute('content'), headerName: metaHeader.getAttribute('content') };
        }
        const match = document.cookie.match(new RegExp('(^| )XSRF-TOKEN=([^;]+)'));
        if (match) {
            return { token: decodeURIComponent(match[2]), headerName: 'X-XSRF-TOKEN' };
        }
        return { token: '', headerName: 'X-CSRF-TOKEN' };
    }
})();
