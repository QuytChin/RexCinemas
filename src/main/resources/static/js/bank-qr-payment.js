(() => {
  const root = document.getElementById('bankQrPayment');
  if (!root) return;
  const countdown = document.getElementById('bankQrCountdown');
  const statusText = document.getElementById('paymentStatusText');
  const statusDot = document.querySelector('.bank-qr-status-dot');
  const hint = document.getElementById('bankQrHint');
  const expiredPanel = document.getElementById('bankQrExpired');
  const rebookLink = document.getElementById('bankQrRebook');
  let deadlineMs = Number(root.dataset.expiresAtEpoch);
  let serverNowMs = Number(root.dataset.serverNowEpoch);
  let clientClockAtSync = performance.now();
  let pollingTimer;
  let countdownTimer;
  let terminal = false;
  let deadlineReached = false;
  let activePage = true;
  let pollInFlight = false;
  let rebookUrl = root.dataset.seatMapUrl;
  window.addEventListener('pagehide', () => { activePage = false; clearInterval(pollingTimer); clearInterval(countdownTimer); });
  window.addEventListener('pageshow', event => { if (event.persisted) window.location.reload(); });

  const estimatedServerNow = () => serverNowMs + (performance.now() - clientClockAtSync);
  const stopAll = () => { terminal = true; clearInterval(pollingTimer); clearInterval(countdownTimer); };
  const stopCountdown = () => clearInterval(countdownTimer);
  const syncClock = (booking) => {
    if (Number.isFinite(Number(booking.serverNowEpochMillis))) {
      serverNowMs = Number(booking.serverNowEpochMillis);
      clientClockAtSync = performance.now();
    }
    if (booking.expiresAtEpochMillis != null && Number.isFinite(Number(booking.expiresAtEpochMillis)))
      deadlineMs = Number(booking.expiresAtEpochMillis);
    if (booking.rebookUrl && booking.rebookUrl.startsWith('/')) rebookUrl = booking.rebookUrl;
  };
  const setStatus = (state, message) => { root.dataset.state = state.toLowerCase(); statusText.textContent = message; };
  const hidePaymentInstructions = () => {
    root.dataset.paymentUnavailable = 'true';
    document.querySelectorAll('.bank-qr-copy-btn').forEach((button) => { button.disabled = true; });
  };
  const redirectToSeatMap = () => window.setTimeout(() => {
    if (activePage) window.location.assign(rebookUrl);
  }, 4000);
  const handleTerminal = (status) => {
    if (status === 'CONFIRMED') {
      setStatus(status, 'Thanh toán thành công'); stopAll();
      window.setTimeout(() => window.location.assign(`${root.dataset.ticketUrl}?success`), 700);
      return;
    }
    hidePaymentInstructions(); statusDot?.classList.add('is-terminal');
    if (status === 'EXPIRED') {
      setStatus(status, 'Hết thời gian giữ ghế');
      hint.textContent = 'Booking này không còn nhận thanh toán.';
      expiredPanel.hidden = false;
      rebookLink.href = rebookUrl;
      stopAll(); redirectToSeatMap();
    } else if (status === 'CANCELLED') {
      setStatus(status, 'Booking đã bị hủy'); hint.textContent = 'Bạn sẽ được chuyển về trang chọn ghế.';
      stopAll(); redirectToSeatMap();
    } else if (status === 'REFUND_PENDING') {
      setStatus(status, 'Giao dịch đang chờ hoàn tiền'); hint.textContent = 'Bộ phận hỗ trợ sẽ xử lý giao dịch này.';
      stopAll();
    }
  };
  const renderCountdown = () => {
    if (!Number.isFinite(deadlineMs) || !Number.isFinite(serverNowMs)) { countdown.textContent = '--:--'; return; }
    const secondsLeft = Math.max(0, Math.ceil((deadlineMs - estimatedServerNow()) / 1000));
    countdown.textContent = `${String(Math.floor(secondsLeft / 60)).padStart(2, '0')}:${String(secondsLeft % 60).padStart(2, '0')}`;
    if (secondsLeft === 0 && !deadlineReached && !terminal) {
      deadlineReached = true; hidePaymentInstructions();
      setStatus('PENDING', 'Đã đến hạn thanh toán, đang xác nhận trạng thái');
      hint.textContent = 'Đang kiểm tra trạng thái thanh toán với máy chủ.';
      stopCountdown(); poll();
    }
  };
  const poll = async () => {
    if (terminal || !activePage || pollInFlight) return;
    pollInFlight = true;
    try {
      const response = await fetch(root.dataset.statusUrl, {headers: {'Accept': 'application/json'}});
      if (!response.ok) return;
      const booking = await response.json();
      if (!activePage || terminal) return;
      syncClock(booking);
      if (['CONFIRMED', 'EXPIRED', 'CANCELLED', 'REFUND_PENDING'].includes(booking.status)) handleTerminal(booking.status);
      else if (booking.status === 'PENDING') {
        if (booking.paymentAllowed === false || deadlineReached) {
          hidePaymentInstructions(); setStatus('PENDING', 'Đã đến hạn thanh toán, đang xác nhận trạng thái');
        } else { setStatus('PENDING', 'Đang chờ thanh toán'); renderCountdown(); }
      }
    } catch (_) { /* A network error must never declare a booking expired. */ }
    finally { pollInFlight = false; }
  };
  document.querySelectorAll('.bank-qr-copy-btn').forEach((button) => button.addEventListener('click', async () => {
    if (root.dataset.paymentUnavailable === 'true') return;
    const value = button.dataset.copyValue || ''; if (!value) return;
    try { await navigator.clipboard.writeText(value); }
    catch (_) {
      const input = document.createElement('textarea');
      input.value = value; input.setAttribute('readonly', ''); input.style.position = 'fixed'; input.style.opacity = '0';
      document.body.appendChild(input); input.select(); document.execCommand('copy'); input.remove();
    }
    const original = button.textContent; button.textContent = 'Đã sao chép';
    window.setTimeout(() => { button.textContent = original; }, 1400);
  }));
  renderCountdown(); countdownTimer = window.setInterval(renderCountdown, 1000); poll(); pollingTimer = window.setInterval(poll, 4000);
})();
