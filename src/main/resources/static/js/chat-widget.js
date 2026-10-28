/**
 * RexCinemas - Real-time Support Chat & AI Assistant Widget
 */
(function () {
    'use strict';

    let sessionId = localStorage.getItem('rex_chat_session_id');
    if (!sessionId) {
        sessionId = 'rex_' + Date.now() + '_' + Math.random().toString(36).substring(2, 10);
        localStorage.setItem('rex_chat_session_id', sessionId);
    }

    let currentTab = 'ai'; // 'ai' or 'cinema'
    let selectedCinemaId = localStorage.getItem('rex_chat_cinema_id') || '';
    let currentUser = null;
    let stompClient = null;
    let unreadCount = 0;
    let isOpen = false;

    // DOM Elements
    const launcher = document.getElementById('rexChatLauncher');
    const openBtn = document.getElementById('rexChatOpenBtn');
    const closeBtn = document.getElementById('rexChatCloseBtn');
    const chatBox = document.getElementById('rexChatBox');
    const unreadBadge = document.getElementById('rexChatUnreadBadge');
    const tabAi = document.getElementById('rexTabAi');
    const tabCinema = document.getElementById('rexTabCinema');
    const cinemaSelectBar = document.getElementById('rexCinemaSelectBar');
    const cinemaSelect = document.getElementById('rexCinemaSelect');
    const messagesContainer = document.getElementById('rexChatMessages');
    const quickChips = document.getElementById('rexQuickChips');
    const chatInput = document.getElementById('rexChatInput');
    const sendBtn = document.getElementById('rexChatSendBtn');

    if (!openBtn || !chatBox) return;

    // 1. Khởi tạo & nạp dữ liệu ban đầu
    init();

    function init() {
        fetchCurrentUser();
        loadCinemas();
        loadHistory();
        initWebSocket();
        bindEvents();
    }

    function fetchCurrentUser() {
        fetch('/api/support/me')
            .then(res => res.json())
            .then(data => {
                currentUser = data;
                if (data.authenticated && data.favoriteCinemaId && !selectedCinemaId) {
                    selectedCinemaId = data.favoriteCinemaId;
                    localStorage.setItem('rex_chat_cinema_id', selectedCinemaId);
                }
            })
            .catch(() => {});
    }

    function loadCinemas() {
        fetch('/api/support/cinemas')
            .then(res => res.json())
            .then(cinemas => {
                if (!cinemaSelect) return;
                cinemaSelect.innerHTML = '<option value="">-- Chọn cụm rạp để kết nối --</option>';
                cinemas.forEach(c => {
                    const opt = document.createElement('option');
                    opt.value = c.id;
                    opt.textContent = `${c.name} (${c.city})`;
                    if (String(c.id) === String(selectedCinemaId)) {
                        opt.selected = true;
                    }
                    cinemaSelect.appendChild(opt);
                });
                if (!selectedCinemaId && cinemas.length > 0) {
                    selectedCinemaId = cinemas[0].id;
                    cinemaSelect.value = cinemas[0].id;
                    localStorage.setItem('rex_chat_cinema_id', selectedCinemaId);
                }
            })
            .catch(() => {});
    }

    function loadHistory() {
        fetch(`/api/support/history?sessionId=${encodeURIComponent(sessionId)}`)
            .then(res => res.json())
            .then(msgs => {
                if (Array.isArray(msgs) && msgs.length > 0) {
                    messagesContainer.innerHTML = '';
                    msgs.forEach(appendMessageUI);
                    scrollToBottom();
                }
            })
            .catch(() => {});
    }

    // 2. Kết nối WebSocket STOMP
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
            console.warn('[RexChat] WebSocket init skipped:', e);
        }
    }

    function onStompConnected(client) {
        client.subscribe('/topic/support/session/' + sessionId, message => {
            try {
                const data = JSON.parse(message.body);
                // Tránh duplicate nếu tin nhắn của chính mình vừa gửi xong
                const existing = document.querySelector(`[data-msg-id="${data.id}"]`);
                if (!existing) {
                    appendMessageUI(data);
                    scrollToBottom();
                    if (!isOpen && data.senderType !== 'CUSTOMER') {
                        unreadCount++;
                        updateUnreadBadge();
                    }
                }
            } catch (err) {}
        });
    }

    // 3. Sự kiện giao diện
    function bindEvents() {
        openBtn.addEventListener('click', () => {
            isOpen = true;
            chatBox.style.display = 'flex';
            unreadCount = 0;
            updateUnreadBadge();
            scrollToBottom();
            setTimeout(() => chatInput && chatInput.focus(), 150);
        });

        closeBtn.addEventListener('click', () => {
            isOpen = false;
            chatBox.style.display = 'none';
        });

        tabAi.addEventListener('click', () => switchTab('ai'));
        tabCinema.addEventListener('click', () => switchTab('cinema'));

        if (cinemaSelect) {
            cinemaSelect.addEventListener('change', (e) => {
                selectedCinemaId = e.target.value;
                localStorage.setItem('rex_chat_cinema_id', selectedCinemaId);
            });
        }

        sendBtn.addEventListener('click', handleSendMessage);
        chatInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSendMessage();
            }
        });

        // Quick suggestion chips
        if (quickChips) {
            quickChips.addEventListener('click', (e) => {
                const chip = e.target.closest('.rex-chip');
                if (chip) {
                    const prompt = chip.getAttribute('data-prompt');
                    if (prompt) {
                        chatInput.value = prompt;
                        handleSendMessage();
                    }
                }
            });
        }
    }

    function switchTab(tab) {
        currentTab = tab;
        if (tab === 'ai') {
            tabAi.classList.add('active');
            tabCinema.classList.remove('active');
            cinemaSelectBar.style.display = 'none';
            quickChips.style.display = 'flex';
            chatInput.placeholder = 'Hỏi AI: Thể loại, độ tuổi, gợi ý phim...';
        } else {
            tabCinema.classList.add('active');
            tabAi.classList.remove('active');
            cinemaSelectBar.style.display = 'flex';
            quickChips.style.display = 'none';
            chatInput.placeholder = 'Nhập tin nhắn gửi đến nhân viên rạp...';
        }
        scrollToBottom();
    }

    // 4. Gửi tin nhắn
    function handleSendMessage() {
        const text = chatInput.value.trim();
        if (!text) return;
        chatInput.value = '';

        if (currentTab === 'ai') {
            sendToAi(text);
        } else {
            sendToCinema(text);
        }
    }

    function sendToAi(prompt) {
        // Render tin nhắn khách trước
        const tempMsg = {
            id: 'temp_' + Date.now(),
            senderType: 'CUSTOMER',
            senderName: (currentUser && currentUser.name) ? currentUser.name : 'Bạn',
            message: prompt,
            createdAt: 'Vừa xong'
        };
        appendMessageUI(tempMsg);
        showTypingIndicator('Rex AI đang soạn gợi ý...');
        scrollToBottom();

        const csrf = getCsrfToken();
        const headers = { 'Content-Type': 'application/json' };
        if (csrf.token) headers[csrf.headerName] = csrf.token;

        fetch('/api/support/ai-consult', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({
                sessionId: sessionId,
                prompt: prompt,
                guestName: currentUser ? currentUser.name : null
            })
        })
        .then(res => res.json())
        .then(data => {
            hideTypingIndicator();
            if (data && data.message) {
                appendMessageUI(data);
                scrollToBottom();
            }
        })
        .catch(() => {
            hideTypingIndicator();
            appendMessageUI({
                id: 'err_' + Date.now(),
                senderType: 'AI',
                senderName: 'Trợ lý Rex AI 🤖',
                message: 'Xin lỗi, kết nối tới máy chủ AI tạm thời gián đoạn. Bạn thử lại sau giây lát nhé!',
                createdAt: 'Vừa xong'
            });
            scrollToBottom();
        });
    }

    function sendToCinema(text) {
        if (!selectedCinemaId) {
            alert('Vui lòng chọn cụm rạp để kết nối với Ban Quản lý rạp!');
            cinemaSelect && cinemaSelect.focus();
            return;
        }

        const tempMsg = {
            id: 'temp_' + Date.now(),
            senderType: 'CUSTOMER',
            senderName: (currentUser && currentUser.name) ? currentUser.name : 'Bạn',
            message: text,
            createdAt: 'Vừa xong'
        };
        appendMessageUI(tempMsg);
        scrollToBottom();

        const csrf = getCsrfToken();
        const headers = { 'Content-Type': 'application/json' };
        if (csrf.token) headers[csrf.headerName] = csrf.token;

        fetch('/api/support/send', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({
                sessionId: sessionId,
                cinemaId: selectedCinemaId,
                message: text,
                guestName: currentUser ? currentUser.name : null,
                guestEmail: currentUser ? currentUser.email : null
            })
        })
        .then(res => res.json())
        .then(saved => {
            // Replace temp id with real id
            const el = document.querySelector(`[data-msg-id="${tempMsg.id}"]`);
            if (el && saved && saved.id) {
                el.setAttribute('data-msg-id', saved.id);
            }
        })
        .catch(() => {
            alert('Không thể gửi tin nhắn. Vui lòng kiểm tra lại kết nối mạng!');
        });
    }

    // 5. Hiển thị tin nhắn lên UI
    function appendMessageUI(msg) {
        // Kiểm tra tránh trùng lặp
        if (msg.id && document.querySelector(`[data-msg-id="${msg.id}"]`)) return;

        const group = document.createElement('div');
        const type = (msg.senderType || 'CUSTOMER').toLowerCase();
        group.className = `rex-msg-group ${type}`;
        if (msg.id) group.setAttribute('data-msg-id', msg.id);

        let senderLabel = msg.senderName;
        if (type === 'customer') senderLabel = 'Bạn';
        else if (type === 'ai') senderLabel = 'Trợ lý Rex AI 🤖';
        else if (type === 'staff') senderLabel = 'Quản lý Rạp 👨‍💼';

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
        if (msg.createdAt) group.appendChild(timeEl);

        messagesContainer.appendChild(group);
    }

    function showTypingIndicator(text) {
        hideTypingIndicator();
        const indicator = document.createElement('div');
        indicator.id = 'rexTypingIndicator';
        indicator.className = 'rex-msg-group ai rex-typing';
        indicator.innerHTML = `
            <div class="rex-msg-sender">Trợ lý Rex AI 🤖</div>
            <div class="rex-msg-bubble">
                <span class="rex-dots"><span>.</span><span>.</span><span>.</span></span> ${text || 'Đang phản hồi...'}
            </div>
        `;
        messagesContainer.appendChild(indicator);
    }

    function hideTypingIndicator() {
        const el = document.getElementById('rexTypingIndicator');
        if (el) el.remove();
    }

    function scrollToBottom() {
        setTimeout(() => {
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        }, 50);
    }

    function updateUnreadBadge() {
        if (!unreadBadge) return;
        if (unreadCount > 0) {
            unreadBadge.textContent = unreadCount > 9 ? '9+' : unreadCount;
            unreadBadge.style.display = 'inline-block';
        } else {
            unreadBadge.style.display = 'none';
        }
    }

    // 6. Xử lý Markdown an toàn
    function formatMarkdown(text) {
        if (!text) return '';
        // Escape HTML
        let escaped = text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');

        // **in đậm**
        escaped = escaped.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        // *in nghiêng*
        escaped = escaped.replace(/\*(.*?)\*/g, '<em>$1</em>');
        // [title](url)
        escaped = escaped.replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" class="rex-chat-link" target="_blank" rel="noopener">$1</a>');
        // Gạch đầu dòng
        escaped = escaped.replace(/^\s*-\s+(.*)$/gm, '• $1');
        // Xuống dòng
        escaped = escaped.replace(/\n/g, '<br>');
        return escaped;
    }

    function getCsrfToken() {
        const metaToken = document.querySelector('meta[name="_csrf"]');
        const metaHeader = document.querySelector('meta[name="_csrf_header"]');
        if (metaToken && metaHeader) {
            return { token: metaToken.getAttribute('content'), headerName: metaHeader.getAttribute('content') };
        }
        // Fallback đọc từ cookie XSRF-TOKEN
        const match = document.cookie.match(new RegExp('(^| )XSRF-TOKEN=([^;]+)'));
        if (match) {
            return { token: decodeURIComponent(match[2]), headerName: 'X-XSRF-TOKEN' };
        }
        return { token: '', headerName: 'X-CSRF-TOKEN' };
    }
})();
