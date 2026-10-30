/**
 * RexCinemas - Real-time Support Chat & AI Assistant Widget
 * Fixed: Deduplication & Strict User Session Isolation on Logout
 */
(function () {
    'use strict';

    // Xóa sạch storage cũ để tránh rò rỉ dữ liệu phiên trước
    try {
        localStorage.removeItem('rex_chat_session_id');
        sessionStorage.removeItem('rex_chat_session_id');
        localStorage.removeItem('rex_chat_messages');
        sessionStorage.removeItem('rex_chat_messages');
    } catch (e) {}

    let currentUser = null;
    let sessionId = null; // Chỉ giữ trong RAM biến JS, không lưu vào Storage!
    let currentTab = 'ai'; // 'ai' hoặc 'cinema'
    let selectedCinemaId = '';
    let stompClient = null;
    let currentSubscription = null;
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
    const inputBar = document.querySelector('.rex-chat-input-bar');

    if (!openBtn || !chatBox) return;

    init();

    function init() {
        bindEvents();
        initLauncherCollisionGuard();
        loadCinemas();
        checkAuthAndInit();
    }

    // Keep the floating launcher from covering the control a customer needs to use.
    function initLauncherCollisionGuard() {
        if (!launcher || !openBtn) return;

        let pendingFrame = 0;
        const interactiveSelector = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [role="button"]';

        const update = () => {
            if (pendingFrame) return;
            pendingFrame = window.requestAnimationFrame(() => {
                pendingFrame = 0;
                const launcherRect = openBtn.getBoundingClientRect();
                let obscuresControl = window.innerWidth <= 767 && isOpen !== true && document.activeElement !== openBtn;

                if (obscuresControl && launcherRect.width > 0 && launcherRect.height > 0) {
                    obscuresControl = Array.from(document.querySelectorAll(interactiveSelector)).some(control => {
                        if (launcher.contains(control) || control.closest('#rexChatBox')) return false;
                        if (!control.getClientRects().length) return false;

                        const style = window.getComputedStyle(control);
                        if (style.visibility === 'hidden' || style.display === 'none' || Number(style.opacity) === 0) return false;

                        const rect = control.getBoundingClientRect();
                        return rect.width > 0 && rect.height > 0 &&
                            launcherRect.left < rect.right && launcherRect.right > rect.left &&
                            launcherRect.top < rect.bottom && launcherRect.bottom > rect.top;
                    });
                }

                launcher.classList.toggle('is-obscured', obscuresControl);
                if (obscuresControl) {
                    openBtn.setAttribute('aria-hidden', 'true');
                    openBtn.tabIndex = -1;
                } else {
                    openBtn.removeAttribute('aria-hidden');
                    openBtn.removeAttribute('tabindex');
                }
            });
        };

        window.addEventListener('scroll', update, { passive: true, capture: true });
        window.addEventListener('resize', update, { passive: true });
        document.addEventListener('focusin', update);
        document.addEventListener('focusout', update);
        window.addEventListener('load', update, { once: true });
        update();
    }

    // 1. Kiểm tra trạng thái xác thực từ Server
    function checkAuthAndInit() {
        fetch('/api/support/me')
            .then(res => res.json())
            .then(data => {
                currentUser = data;
                if (data && data.authenticated) {
                    // Khách đã đăng nhập: session cố định theo ID tài khoản
                    sessionId = 'user_' + data.id;
                    if (data.favoriteCinemaId && !selectedCinemaId) {
                        selectedCinemaId = data.favoriteCinemaId;
                        if (cinemaSelect) cinemaSelect.value = selectedCinemaId;
                    }
                    initWebSocket();
                } else {
                    // Chưa đăng nhập: session tạm cho AI trong bộ nhớ RAM
                    sessionId = 'guest_' + Math.random().toString(36).substring(2, 9);
                    disconnectWebSocket();
                }
                updateUiForCurrentTab();
            })
            .catch(() => {
                currentUser = { authenticated: false };
                sessionId = 'guest_' + Math.random().toString(36).substring(2, 9);
                disconnectWebSocket();
                updateUiForCurrentTab();
            });
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
                    cinemaSelect.appendChild(opt);
                });
                if (cinemas.length > 0 && !selectedCinemaId) {
                    selectedCinemaId = cinemas[0].id;
                    cinemaSelect.value = cinemas[0].id;
                }
            })
            .catch(() => {});
    }

    function loadHistory() {
        if (!currentUser || !currentUser.authenticated) return;

        fetch('/api/support/history')
            .then(res => {
                if (res.status === 401) throw new Error('Unauthorized');
                return res.json();
            })
            .then(msgs => {
                if (Array.isArray(msgs)) {
                    messagesContainer.innerHTML = '';
                    msgs.forEach(appendMessageUI);
                    scrollToBottom();
                }
            })
            .catch(() => {
                // Nếu bị 401: người dùng đã đăng xuất hoặc token hết hạn
                currentUser = { authenticated: false };
                updateUiForCurrentTab();
            });
    }

    // 2. WebSocket Connection với cơ chế Unsubscribe chống nhân đôi listener
    function initWebSocket() {
        if (!currentUser || !currentUser.authenticated) return;
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
        if (!sessionId) return;

        // Đảm bảo hủy đăng ký listener cũ trước khi tạo listener mới
        if (currentSubscription) {
            try { currentSubscription.unsubscribe(); } catch (e) {}
            currentSubscription = null;
        }

        currentSubscription = client.subscribe('/topic/support/session/' + sessionId, message => {
            try {
                const data = JSON.parse(message.body);
                // Render tin nhắn từ Server và chống trùng lặp tuyệt đối
                appendMessageUI(data);
                scrollToBottom();
                if (!isOpen && data.senderType !== 'CUSTOMER') {
                    unreadCount++;
                    updateUnreadBadge();
                }
            } catch (err) {}
        });
    }

    function disconnectWebSocket() {
        if (currentSubscription) {
            try { currentSubscription.unsubscribe(); } catch (e) {}
            currentSubscription = null;
        }
        if (stompClient) {
            try {
                if (stompClient.deactivate) stompClient.deactivate();
                else if (stompClient.disconnect) stompClient.disconnect();
            } catch (e) {}
            stompClient = null;
        }
    }

    // 3. Sự kiện giao diện
    function bindEvents() {
        openBtn.addEventListener('click', () => {
            isOpen = true;
            chatBox.style.display = 'flex';
            unreadCount = 0;
            updateUnreadBadge();
            // Luôn đồng bộ lại auth khi mở khung chat để phát hiện đăng xuất kịp thời
            checkAuthAndInit();
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
            });
        }

        sendBtn.addEventListener('click', handleSendMessage);
        chatInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSendMessage();
            }
        });

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
        updateUiForCurrentTab();
    }

    function updateUiForCurrentTab() {
        const isAuth = currentUser && currentUser.authenticated;

        if (currentTab === 'cinema') {
            if (!isAuth) {
                // CHƯA ĐĂNG NHẬP Ở TAB RẠP: Xóa sạch DOM, ngắt WebSocket, hiển thị thông báo bảo mật
                disconnectWebSocket();
                messagesContainer.innerHTML = `
                    <div class="rex-login-required p-4 text-center my-auto" style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%;">
                        <div style="font-size: 40px; margin-bottom: 12px;">🔒</div>
                        <h6 style="color: #fff; font-weight: 700; margin-bottom: 8px;">Yêu cầu đăng nhập</h6>
                        <p style="color: #94a3b8; font-size: 13px; line-height: 1.5; margin-bottom: 16px; max-width: 260px;">
                            Vui lòng đăng nhập để trò chuyện với nhân viên rạp và bảo mật thông tin của bạn.
                        </p>
                        <a href="/auth/login" class="btn btn-sm btn-rex px-4 py-2" style="font-weight: 700; text-decoration: none;">
                            ĐĂNG NHẬP NGAY
                        </a>
                    </div>
                `;
                if (inputBar) inputBar.style.display = 'none';
                if (cinemaSelectBar) cinemaSelectBar.style.display = 'none';
            } else {
                if (inputBar) inputBar.style.display = 'flex';
                if (cinemaSelectBar) cinemaSelectBar.style.display = 'flex';
                // Chỉ nạp lại lịch sử nếu đang chưa có tin nhắn
                if (!messagesContainer.querySelector('.rex-msg-group')) {
                    loadHistory();
                }
            }
        } else {
            // TAB TRỢ LÝ AI: Luôn cho phép hỏi đáp
            if (inputBar) inputBar.style.display = 'flex';
            if (messagesContainer.querySelector('.rex-login-required')) {
                messagesContainer.innerHTML = `
                    <div class="rex-msg-group ai">
                        <div class="rex-msg-sender">Trợ lý Rex AI 🤖</div>
                        <div class="rex-msg-bubble">
                            Chào bạn! Mình là <strong>Trợ lý AI của RexCinemas</strong>. Bạn có thể hỏi mình về thể loại phim, gợi ý phim theo độ tuổi, giá vé hoặc bắp nước nhé!
                        </div>
                    </div>
                `;
            }
        }
        scrollToBottom();
    }

    // 4. Gửi tin nhắn - KHÔNG tự render tạm thời (Optimistic UI) để chống nhân đôi tin nhắn!
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
                guestName: currentUser && currentUser.name ? currentUser.name : null
            })
        })
        .then(res => res.json())
        .then(data => {
            hideTypingIndicator();
            if (data && data.message) {
                // Render câu trả lời duy nhất từ server
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
                createdAt: ''
            });
            scrollToBottom();
        });
    }

    function sendToCinema(text) {
        if (!currentUser || !currentUser.authenticated) {
            updateUiForCurrentTab();
            return;
        }

        if (!selectedCinemaId) {
            window.RexNotifications?.toast('Vui lòng chọn cụm rạp trước khi kết nối với ban quản lý.', { type: 'warning' });
            if (cinemaSelect) cinemaSelect.focus();
            return;
        }

        const csrf = getCsrfToken();
        const headers = { 'Content-Type': 'application/json' };
        if (csrf.token) headers[csrf.headerName] = csrf.token;

        // CHỈ gửi payload lên Server, TUYỆT ĐỐI KHÔNG tự append vào DOM trước
        // Tin nhắn sẽ được render chính xác 1 lần duy nhất khi WebSocket/API trả về
        fetch('/api/support/send', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({
                sessionId: sessionId,
                cinemaId: selectedCinemaId,
                message: text
            })
        })
        .then(res => {
            if (res.status === 401) {
                currentUser = { authenticated: false };
                updateUiForCurrentTab();
                throw new Error('Vui lòng đăng nhập!');
            }
            if (!res.ok) throw new Error('Không thể gửi tin nhắn. Vui lòng kiểm tra lại kết nối.');
            return res.json();
        })
        .then(saved => {
            if (saved && saved.id) {
                // Kiểm tra nếu WebSocket chưa kịp append thì append
                appendMessageUI(saved);
                scrollToBottom();
            }
        })
        .catch(err => {
            if (err.message !== 'Vui lòng đăng nhập!') {
                window.RexNotifications?.toast('Không thể gửi tin nhắn. Vui lòng kiểm tra kết nối rồi thử lại.', { type: 'error' });
            }
        });
    }

    // 5. Hiển thị tin nhắn lên UI với cơ chế Deduplication nghiêm ngặt
    function appendMessageUI(msg) {
        if (!msg || !messagesContainer) return;

        // CHỐNG TRÙNG LẶP: Nếu ID tin nhắn đã có trên giao diện thì bỏ qua ngay lập tức!
        if (msg.id && messagesContainer.querySelector(`[data-msg-id="${msg.id}"]`)) {
            return;
        }

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
        renderMessage(bubbleEl, msg.message);

        group.appendChild(senderEl);
        group.appendChild(bubbleEl);

        if (msg.createdAt) {
            const timeEl = document.createElement('div');
            timeEl.className = 'rex-msg-time';
            timeEl.textContent = msg.createdAt;
            group.appendChild(timeEl);
        }

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

    // Chat text is untrusted (customer, staff, external AI). It is rendered as DOM
    // text nodes only, so message content can never become markup.
    function renderMessage(element, text) {
        if (window.RexChatRender) {
            window.RexChatRender.renderInto(element, text);
            return;
        }
        element.textContent = text || '';
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
