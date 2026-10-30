(() => {
  const root = document.getElementById('bankQrPayment');
  if (!root) return;

  const countdown = document.getElementById('bankQrCountdown');
  const statusText = document.getElementById('paymentStatusText');
  const statusDot = document.querySelector('.bank-qr-status-dot');
  const hint = document.getElementById('bankQrHint');
  const expiredPanel = document.getElementById('bankQrExpired');
  const rebookLink = document.getElementById('bankQrRebook');
  const pollIntervalMs = 3500;
  const pollTimeoutMs = 8000;

  let deadlineMs = Number(root.dataset.expiresAtEpoch);
  let serverNowMs = Number(root.dataset.serverNowEpoch);
  let clientClockAtSync = performance.now();
  let pollTimer = null;
  let countdownTimer = null;
  let requestTimeout = null;
  let requestController = null;
  let terminal = false;
  let deadlineReached = false;
  let activePage = true;
  let pollInFlight = false;
  let rebookUrl = root.dataset.seatMapUrl;
  let paymentSocket = null;

  const isLocalPath = (value) => typeof value === 'string' && value.startsWith('/') && !value.startsWith('//');
  const estimatedServerNow = () => serverNowMs + (performance.now() - clientClockAtSync);
  const stopCountdown = () => {
    if (countdownTimer) window.clearInterval(countdownTimer);
    countdownTimer = null;
  };
  const stopAll = () => {
    terminal = true;
    if (pollTimer) window.clearTimeout(pollTimer);
    pollTimer = null;
    stopCountdown();
    if (requestTimeout) window.clearTimeout(requestTimeout);
    requestTimeout = null;
    requestController?.abort();
    requestController = null;
  };
  const schedulePoll = (delay = pollIntervalMs) => {
    if (terminal || !activePage || document.visibilityState === 'hidden') return;
    if (pollTimer) window.clearTimeout(pollTimer);
    pollTimer = window.setTimeout(() => {
      pollTimer = null;
      poll();
    }, delay);
  };
  const syncClock = (booking) => {
    if (Number.isFinite(Number(booking.serverNowEpochMillis))) {
      serverNowMs = Number(booking.serverNowEpochMillis);
      clientClockAtSync = performance.now();
    }
    if (booking.expiresAtEpochMillis != null && Number.isFinite(Number(booking.expiresAtEpochMillis))) {
      deadlineMs = Number(booking.expiresAtEpochMillis);
    }
    if (isLocalPath(booking.rebookUrl)) rebookUrl = booking.rebookUrl;
  };
  const setStatus = (state, message) => {
    root.dataset.state = state.toLowerCase();
    if (statusText) statusText.textContent = message;
  };
  const hidePaymentInstructions = () => {
    root.dataset.paymentUnavailable = 'true';
    document.querySelectorAll('.bank-qr-copy-btn').forEach((button) => { button.disabled = true; });
  };
  const redirectToSeatMap = () => window.setTimeout(() => {
    if (activePage && isLocalPath(rebookUrl)) window.location.assign(rebookUrl);
  }, 4000);
  const goToTicket = () => {
    if (!isLocalPath(root.dataset.ticketUrl)) return;
    stopAll();
    window.location.replace(`${root.dataset.ticketUrl}?success`);
  };
  const handleTerminal = (status) => {
    if (status === 'CONFIRMED') {
      setStatus(status, '\u0054\u0068\u0061\u006e\u0068\u0020\u0074\u006f\u00e1\u006e\u0020\u0074\u0068\u00e0\u006e\u0068\u0020\u0063\u00f4\u006e\u0067');
      hidePaymentInstructions();
      goToTicket();
      return;
    }

    hidePaymentInstructions();
    statusDot?.classList.add('is-terminal');
    if (status === 'EXPIRED') {
      setStatus(status, '\u0048\u1ebf\u0074\u0020\u0074\u0068\u1eddi\u0020\u0067\u0069\u0061\u006e\u0020\u0067\u0069\u1eef\u0020\u0067\u0068\u1ebf');
      if (hint) hint.textContent = '\u0042\u006f\u006f\u006b\u0069\u006e\u0067\u0020\u006e\u00e0\u0079\u0020\u006b\u0068\u00f4\u006e\u0067\u0020\u0063\u00f2\u006e\u0020\u006e\u0068\u1ead\u006e\u0020\u0074\u0068\u0061\u006e\u0068\u0020\u0074\u006f\u00e1\u006e.';
      if (expiredPanel) expiredPanel.hidden = false;
      if (rebookLink && isLocalPath(rebookUrl)) rebookLink.href = rebookUrl;
      stopAll();
      redirectToSeatMap();
    } else if (status === 'CANCELLED') {
      setStatus(status, '\u0042\u006f\u006f\u006b\u0069\u006e\u0067\u0020\u0111\u00e3\u0020\u0062\u1ecb\u0020\u0068\u1ee7\u0079');
      if (hint) hint.textContent = '\u0042\u1ea1\u006e\u0020\u0073\u1ebd\u0020\u0111\u01b0\u1ee3\u0063\u0020\u0063\u0068\u0075\u0079\u1ec3\u006e\u0020\u0076\u1ec1\u0020\u0074\u0072\u0061\u006e\u0067\u0020\u0063\u0068\u1ecd\u006e\u0020\u0067\u0068\u1ebf.';
      stopAll();
      redirectToSeatMap();
    } else if (status === 'REFUND_PENDING') {
      setStatus(status, '\u0047\u0069\u0061\u006f\u0020\u0064\u1ecb\u0063\u0068\u0020\u0111\u0061\u006e\u0067\u0020\u0063\u0068\u1edd\u0020\u0068\u006f\u00e0\u006e\u0020\u0074\u0069\u1ec1\u006e');
      if (hint) hint.textContent = '\u0042\u1ed9\u0020\u0070\u0068\u1ead\u006e\u0020\u0068\u1ed7\u0020\u0074\u0072\u1ee3\u0020\u0073\u1ebd\u0020\u0078\u1eed\u0020\u006c\u00fd\u0020\u0067\u0069\u0061\u006f\u0020\u0064\u1ecb\u0063\u0068\u0020\u006e\u00e0\u0079.';
      stopAll();
    }
  };
  const renderCountdown = () => {
    if (!countdown) return;
    if (!Number.isFinite(deadlineMs) || !Number.isFinite(serverNowMs)) {
      countdown.textContent = '--:--';
      return;
    }
    const secondsLeft = Math.max(0, Math.ceil((deadlineMs - estimatedServerNow()) / 1000));
    countdown.textContent = `${String(Math.floor(secondsLeft / 60)).padStart(2, '0')}:${String(secondsLeft % 60).padStart(2, '0')}`;
    if (secondsLeft === 0 && !deadlineReached && !terminal) {
      deadlineReached = true;
      hidePaymentInstructions();
      setStatus('PENDING', '\u0110\u00e3\u0020\u0111\u1ebf\u006e\u0020\u0068\u1ea1\u006e\u0020\u0074\u0068\u0061\u006e\u0068\u0020\u0074\u006f\u00e1\u006e,\u0020\u0111\u0061\u006e\u0067\u0020\u0078\u00e1\u0063\u0020\u006e\u0068\u1ead\u006e\u0020\u0074\u0072\u1ea1\u006e\u0067\u0020\u0074\u0068\u00e1\u0069');
      if (hint) hint.textContent = '\u0110\u0061\u006e\u0067\u0020\u006b\u0069\u1ec3\u006d\u0020\u0074\u0072\u0061\u0020\u0074\u0068\u00e0\u006e\u0068\u0020\u0074\u006f\u00e1\u006e\u0020\u0076\u1edbi\u0020\u006d\u00e1\u0079\u0020\u0063\u0068\u1ee7.';
      stopCountdown();
      schedulePoll(0);
    }
  };
  const poll = async () => {
    if (terminal || !activePage || pollInFlight || document.visibilityState === 'hidden') return;
    const statusUrl = root.dataset.statusUrl;
    if (!isLocalPath(statusUrl)) return;

    pollInFlight = true;
    requestController = new AbortController();
    requestTimeout = window.setTimeout(() => requestController?.abort(), pollTimeoutMs);
    try {
      const response = await fetch(statusUrl, {
        method: 'GET',
        credentials: 'same-origin',
        cache: 'no-store',
        headers: { Accept: 'application/json' },
        signal: requestController.signal,
      });
      if (!response.ok) throw new Error(`Payment status returned ${response.status}`);
      const contentType = response.headers.get('content-type') || '';
      if (!contentType.includes('application/json')) throw new Error('Payment status response was not JSON');

      const booking = await response.json();
      if (!activePage || terminal) return;
      syncClock(booking);
      const status = String(booking.status || '').toUpperCase();
      if (['CONFIRMED', 'EXPIRED', 'CANCELLED', 'REFUND_PENDING'].includes(status)) {
        handleTerminal(status);
      } else if (status === 'PENDING') {
        if (booking.paymentAllowed === false || deadlineReached) {
          hidePaymentInstructions();
          setStatus('PENDING', '\u0110\u00e3\u0020\u0111\u1ebf\u006e\u0020\u0068\u1ea1\u006e\u0020\u0074\u0068\u0061\u006e\u0068\u0020\u0074\u006f\u00e1\u006e,\u0020\u0111\u0061\u006e\u0067\u0020\u0078\u00e1\u0063\u0020\u006e\u0068\u1ead\u006e\u0020\u0074\u0072\u1ea1\u006e\u0067\u0020\u0074\u0068\u00e1\u0069');
        } else {
          setStatus('PENDING', '\u0110\u0061\u006e\u0067\u0020\u0063\u0068\u1edd\u0020\u0074\u0068\u0061\u006e\u0068\u0020\u0074\u006f\u00e1\u006e');
          renderCountdown();
        }
      }
    } catch (error) {
      if (error.name !== 'AbortError' && hint) {
        hint.textContent = '\u0110\u0061\u006e\u0067\u0020\u0074\u1ea3\u0069\u0020\u0074\u0072\u1ea1\u006e\u0067\u0020\u0074\u0068\u00e1\u0069\u0020\u0074\u0068\u0061\u006e\u0068\u0020\u0074\u006f\u00e1\u006e.\u0020\u0048\u1ec7\u0020\u0074\u0068\u1ed1\u006e\u0067\u0020\u0073\u1ebd\u0020\u0074\u1ef1\u0020\u0074\u0068\u1eed\u0020\u006c\u1ea1\u0069.';
      }
    } finally {
      if (requestTimeout) window.clearTimeout(requestTimeout);
      requestTimeout = null;
      requestController = null;
      pollInFlight = false;
      schedulePoll();
    }
  };

  document.querySelectorAll('.bank-qr-copy-btn').forEach((button) => button.addEventListener('click', async () => {
    if (root.dataset.paymentUnavailable === 'true') return;
    const value = button.dataset.copyValue || '';
    if (!value) return;
    try {
      await navigator.clipboard.writeText(value);
    } catch (_) {
      const input = document.createElement('textarea');
      input.value = value;
      input.setAttribute('readonly', '');
      input.style.position = 'fixed';
      input.style.opacity = '0';
      document.body.appendChild(input);
      input.select();
      document.execCommand('copy');
      input.remove();
    }
    const original = button.textContent;
    button.textContent = '\u0110\u00e3\u0020\u0073\u0061\u006f\u0020\u0063\u0068\u00e9\u0070';
    window.setTimeout(() => { button.textContent = original; }, 1400);
  }));

  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') schedulePoll(0);
    else if (pollTimer) { window.clearTimeout(pollTimer); pollTimer = null; }
  });
  window.addEventListener('pagehide', () => {
    activePage = false;
    stopAll();
    if (paymentSocket) paymentSocket.deactivate();
  });
  window.addEventListener('pageshow', (event) => {
    if (event.persisted) window.location.reload();
  });

  renderCountdown();
  countdownTimer = window.setInterval(renderCountdown, 1000);
  schedulePoll(0);

  try {
    if (window.StompJs?.Client && window.SockJS) {
      paymentSocket = new window.StompJs.Client({
        webSocketFactory: () => new window.SockJS('/ws'),
        reconnectDelay: 3000,
        onConnect: () => {
          paymentSocket.subscribe('/user/queue/payment-status', (message) => {
            try {
              const update = JSON.parse(message.body);
              if (update.bookingCode !== root.dataset.bookingCode) return;
              if (String(update.status || '').toUpperCase() === 'CONFIRMED') {
                handleTerminal('CONFIRMED');
              }
            } catch (error) {
              console.warn('Invalid realtime payment update', error);
            }
          });
          // Reconcile immediately after connect/reconnect so an event sent while
          // the socket was down cannot leave the page waiting for the next poll.
          schedulePoll(0);
        },
      });
      paymentSocket.activate();
    }
  } catch (error) {
    console.warn('Realtime payment updates unavailable; polling remains active', error);
  }
})();
