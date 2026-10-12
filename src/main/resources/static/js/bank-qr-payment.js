(() => {
  const root = document.getElementById('bankQrPayment');
  if (!root) return;
  const countdown = document.getElementById('bankQrCountdown');
  const statusText = document.getElementById('paymentStatusText');
  const expiresAt = root.dataset.expiresAt;
  const expiresMs = expiresAt ? Date.parse(expiresAt.replace(' ', 'T')) : NaN;
  const renderCountdown = () => {
    if (!Number.isFinite(expiresMs)) return;
    const left = Math.max(0, Math.floor((expiresMs - Date.now()) / 1000));
    countdown.textContent = `${String(Math.floor(left / 60)).padStart(2, '0')}:${String(left % 60).padStart(2, '0')}`;
    if (!left) statusText.textContent = 'Đã hết thời gian giữ ghế';
  };
  const poll = async () => {
    try {
      const response = await fetch(root.dataset.statusUrl, {headers: {'Accept': 'application/json'}});
      if (!response.ok) return;
      const booking = await response.json();
      if (booking.status === 'CONFIRMED') {
        statusText.textContent = 'Thanh toán thành công';
        window.location.assign(`${root.dataset.ticketUrl}?success`);
      } else if (['CANCELLED', 'EXPIRED'].includes(booking.status)) {
        statusText.textContent = 'Booking đã hết hiệu lực';
      }
    } catch (_) { /* Retry on the next interval. */ }
  };
  renderCountdown();
  setInterval(renderCountdown, 1000);
  poll();
  setInterval(poll, 4000);
})();
