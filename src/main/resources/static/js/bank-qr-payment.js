(() => {
  const root = document.getElementById('bankQrPayment');
  if (!root) return;
  const countdown = document.getElementById('bankQrCountdown');
  const statusText = document.getElementById('paymentStatusText');
  const statusDot = document.querySelector('.bank-qr-status-dot');
  const expiresAt = root.dataset.expiresAt;
  const expiresMs = expiresAt ? Date.parse(expiresAt.replace(' ', 'T')) : NaN;
  let pollingTimer;
  let countdownTimer;
  let terminal = false;

  const stopPolling = () => {
    terminal = true;
    clearInterval(pollingTimer);
    clearInterval(countdownTimer);
  };
  const updateStatus = (status, message) => {
    statusText.textContent = message;
    root.dataset.state = status.toLowerCase();
    if (status !== 'PENDING') {
      statusDot?.classList.add('is-terminal');
      stopPolling();
    }
  };
  const renderCountdown = () => {
    if (!Number.isFinite(expiresMs)) return;
    const left = Math.max(0, Math.floor((expiresMs - Date.now()) / 1000));
    countdown.textContent = `${String(Math.floor(left / 60)).padStart(2, '0')}:${String(left % 60).padStart(2, '0')}`;
    if (!left && !terminal) statusText.textContent = 'Đã hết thời gian giữ ghế, đang xác nhận trạng thái';
  };
  const poll = async () => {
    if (terminal) return;
    try {
      const response = await fetch(root.dataset.statusUrl, {headers: {'Accept': 'application/json'}});
      if (!response.ok) return;
      const booking = await response.json();
      if (booking.status === 'CONFIRMED') {
        updateStatus('CONFIRMED', 'Thanh toán thành công');
        window.setTimeout(() => window.location.assign(`${root.dataset.ticketUrl}?success`), 700);
      } else if (booking.status === 'EXPIRED') {
        updateStatus('EXPIRED', 'Đã hết thời gian thanh toán');
      } else if (booking.status === 'CANCELLED') {
        updateStatus('CANCELLED', 'Booking đã được hủy');
      } else if (booking.status === 'REFUND_PENDING') {
        updateStatus('REFUND_PENDING', 'Đã ghi nhận giao dịch, đang chờ xử lý hoàn tiền');
      } else {
        statusText.textContent = 'Đang chờ thanh toán';
      }
    } catch (_) { /* Retry on the next interval. */ }
  };
  document.querySelectorAll('.bank-qr-copy-btn').forEach(button => button.addEventListener('click', async () => {
    const value = button.dataset.copyValue || '';
    if (!value) return;
    try {
      await navigator.clipboard.writeText(value);
    } catch (_) {
      const input = document.createElement('textarea');
      input.value = value; input.setAttribute('readonly', ''); input.style.position = 'fixed'; input.style.opacity = '0';
      document.body.appendChild(input); input.select(); document.execCommand('copy'); input.remove();
    }
    const original = button.textContent;
    button.textContent = 'Đã sao chép';
    window.setTimeout(() => { button.textContent = original; }, 1400);
  }));
  renderCountdown();
  countdownTimer = setInterval(renderCountdown, 1000);
  poll();
  pollingTimer = setInterval(poll, 4000);
})();
