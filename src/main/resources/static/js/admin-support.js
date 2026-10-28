/**
 * RexCinemas - Admin Real-time Support Chat Manager
 */
(function () {
    'use strict';

    let activeSessionId = null;
    let stompClient = null;

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
        // Lắng nghe topic của rạp hiện tại hoặc topic tổng admin
        const topic = window.CURRENT_CINEMA_ID ? `/topic/support/cinema/${window.CURRENT_CINEMA_ID}` : '/topic/support/admin';
        client.subscribe(topic, message => {
            try {
                const data = JSON.parse(message.body);
                handleIncomingMessage(data);
            } catch (err) {}
        });
    }

    function handleIncomingMessage(msg) {
        // 1. Nếu đang mở cuộc hội thoại này, render tin nhắn ngay
        if (activeSessionId && msg.sessionId === activeSessionId) {
            const existing = document.querySelector(`[data-msg-id="${msg.id}"]`);
            if (!existing) {
                appendMessageUI(msg);
                scrollToBottom();
                markCurrentAsRead();
            }
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
        .then(res => res.json())
        .then(saved => {
            if (saved && saved.id) {
                appendMessageUI(saved);
                scrollToBottom();
                refreshConversationList();
            }
        })
        .catch(() => {
            alert('Không thể gửi phản hồi. Vui lòng thử lại!');
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
                    const item = document.createElement('div');
                    item.className = 'admin-chat-item' + (c.sessionId === activeSessionId ? ' active' : '');
                    item.setAttribute('data-session-id', c.sessionId);
                    if (c.cinemaId) item.setAttribute('data-cinema-id', c.cinemaId);
                    item.onclick = function () { selectConversation(item); };

                    const initial = (c.customerName || 'K').substring(0, 1).toUpperCase();
                    const timeStr = c.lastMessageAt ? formatTime(c.lastMessageAt) : '';

                    item.innerHTML = `
                        <div class="admin-chat-avatar">${initial}</div>
                        <div class="admin-chat-meta">
                            <div class="admin-chat-name">
                                <span>${c.customerName || 'Khách vãng lai'}</span>
                                <span class="admin-chat-time">${timeStr}</span>
                            </div>
                            <div class="admin-chat-preview">${escapeHtml(c.lastMessage || '')}</div>
                            <div class="d-flex align-items-center justify-content-between">
                                <span class="admin-chat-cinema-badge">${c.cinemaName || 'Rex Cinemas'}</span>
                                ${c.unreadCount > 0 ? `<span class="admin-chat-unread-badge">${c.unreadCount}</span>` : ''}
                            </div>
                        </div>
                    `;
                    convList.appendChild(item);
                });
            })
            .catch(() => {});
    }

    function appendMessageUI(msg) {
        if (msg.id && document.querySelector(`[data-msg-id="${msg.id}"]`)) return;

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
        bubbleEl.innerHTML = formatMarkdown(msg.message);

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

    function formatMarkdown(text) {
        if (!text) return '';
        let escaped = escapeHtml(text);
        escaped = escaped.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        escaped = escaped.replace(/\*(.*?)\*/g, '<em>$1</em>');
        escaped = escaped.replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" class="rex-chat-link" target="_blank" rel="noopener">$1</a>');
        escaped = escaped.replace(/^\s*-\s+(.*)$/gm, '• $1');
        escaped = escaped.replace(/\n/g, '<br>');
        return escaped;
    }

    function escapeHtml(text) {
        if (!text) return '';
        return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
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
