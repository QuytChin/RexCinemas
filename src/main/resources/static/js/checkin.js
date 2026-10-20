(() => {
  const result = document.getElementById('scanResult');
  const manual = document.getElementById('manualCode');
  const manualBtn = document.getElementById('manualScanBtn');
  const cameraSelect = document.getElementById('cameraSelect');
  const cameraToggle = document.getElementById('cameraToggleBtn');
  const imageInput = document.getElementById('qrImageInput');
  const cameraMessage = document.getElementById('cameraMessage');
  const cameraDot = document.getElementById('cameraStatusDot');
  const reader = document.getElementById('reader');
  const csrfHeaders = () => {
    const pair = document.cookie.split('; ').find(item => item.startsWith('XSRF-TOKEN='));
    return pair ? { 'X-XSRF-TOKEN': decodeURIComponent(pair.slice('XSRF-TOKEN='.length)) } : {};
  };
  const escapeHtml = value => String(value ?? '—').replace(/[&<>"']/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[char]));
  const statusLabels = {
    CHECKED_IN: 'Đã check-in thành công', ALREADY_CHECKED_IN: 'Vé đã được check-in trước đó',
    NOT_FOUND: 'Không tìm thấy vé', PENDING: 'Vé chưa thanh toán', CANCELLED: 'Vé đã bị hủy',
    EXPIRED: 'Vé đã hết hạn', REFUNDED: 'Vé đang hoặc đã được hoàn tiền',
    INVALID_STATUS: 'Trạng thái vé không hợp lệ', TOO_EARLY: 'Chưa đến thời gian check-in',
    INVALID: 'Mã vé không hợp lệ'
  };
  let scanner = null;
  let cameraRunning = false;
  let busy = false;
  let lastCode = '';
  let lastAt = 0;

  function showMessage(kind, title, message, data = {}) {
    result.className = `scan-result ${kind}`;
    result.innerHTML = `<div class="scan-icon">${kind === 'success' ? '✓' : kind === 'loading' ? '…' : '!'}</div>
      <p class="eyebrow mb-1">${escapeHtml(data.status || '')}</p>
      <h3>${escapeHtml(title)}</h3><p>${escapeHtml(message)}</p>
      ${data.bookingCode ? `<div class="scan-ticket-info">
        <div><span>Mã vé</span><b>${escapeHtml(data.bookingCode)}</b></div>
        <div><span>Phim</span><b>${escapeHtml(data.movieTitle)}</b></div>
        <div><span>Khách hàng</span><b>${escapeHtml(data.customerName)}</b></div>
        <div><span>Suất chiếu</span><b>${escapeHtml(data.showtime)}</b></div>
        <div><span>Rạp / phòng</span><b>${escapeHtml(data.cinemaName)} / ${escapeHtml(data.auditoriumName)}</b></div>
        <div><span>Ghế</span><b>${escapeHtml(data.seats)}</b></div>
        ${data.checkedInAt ? `<div><span>Thời điểm check-in</span><b>${escapeHtml(data.checkedInAt)}</b></div>` : ''}
      </div>` : ''}`;
  }

  async function submit(rawCode) {
    const code = String(rawCode || '').trim();
    if (!code || busy) return;
    const now = Date.now();
    if (code === lastCode && now - lastAt < 2500) return;
    lastCode = code;
    lastAt = now;
    busy = true;
    manualBtn.disabled = true;
    showMessage('loading', 'Đang kiểm tra vé', 'Vui lòng chờ trong giây lát.');
    try {
      const response = await fetch('/admin/checkin/api/scan', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...csrfHeaders() },
        body: JSON.stringify({ code })
      });
      let data;
      try { data = await response.json(); }
      catch { throw new Error(response.status === 401 || response.redirected
        ? 'Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại rồi thử quét.'
        : `Máy chủ trả về phản hồi không hợp lệ (HTTP ${response.status}).`); }

      if (!response.ok && response.status >= 500) {
        showMessage('danger', 'Máy chủ đang gặp sự cố', data.error || data.message || `Không thể xử lý yêu cầu (HTTP ${response.status}). Hãy thử lại sau.`);
        return;
      }
      const success = response.ok && data.success === true;
      const title = statusLabels[data.status] || data.message || (success ? 'Check-in thành công' : 'Không thể check-in vé');
      showMessage(success ? 'success' : 'danger', title, data.message || data.error || '', { ...data, status: statusLabels[data.status] ? '' : data.status });
      if (success) window.setTimeout(() => window.location.reload(), 3500);
    } catch (error) {
      showMessage('danger', 'Không thể kiểm tra vé', error.message || 'Không kết nối được máy chủ. Hãy thử lại.');
    } finally {
      busy = false;
      manualBtn.disabled = false;
    }
  }

  async function loadCameras() {
    if (!window.Html5Qrcode) {
      cameraSelect.innerHTML = '<option value="">Không tải được công cụ camera</option>';
      cameraMessage.textContent = 'Không tải được thư viện quét QR. Bạn vẫn có thể nhập mã vé hoặc dùng ảnh QR.';
      return;
    }
    try {
      const cameras = await Html5Qrcode.getCameras();
      cameraSelect.replaceChildren();
      if (!cameras.length) {
        cameraSelect.add(new Option('Không tìm thấy camera', ''));
        cameraMessage.textContent = 'Không phát hiện camera. Hãy kiểm tra thiết bị hoặc nhập mã vé thủ công.';
        return;
      }
      cameras.forEach((camera, index) => cameraSelect.add(new Option(camera.label || `Camera ${index + 1}`, camera.id)));
      cameraSelect.disabled = cameras.length < 2;
      cameraToggle.disabled = false;
      cameraMessage.textContent = 'Camera sẵn sàng. Nhấn “Bật camera” để bắt đầu quét.';
    } catch {
      cameraSelect.innerHTML = '<option value="">Chưa cấp quyền camera</option>';
      cameraMessage.textContent = 'Trình duyệt chưa cấp quyền camera hoặc trang chưa chạy trên HTTPS/localhost. Bạn có thể nhập mã vé.';
    }
  }

  async function toggleCamera() {
    if (cameraRunning) {
      try { await scanner.stop(); } catch { /* Camera có thể đã dừng bởi trình duyệt. */ }
      cameraRunning = false;
      cameraDot?.classList.remove('is-active');
      cameraToggle.textContent = 'Bật camera';
      cameraMessage.textContent = 'Đã tắt camera.';
      reader.innerHTML = '<div class="qr-reader-placeholder"><span class="qr-placeholder-icon">▦</span><strong>Sẵn sàng quét vé</strong><span>Chọn “Bật camera” hoặc tải ảnh QR lên</span></div>';
      scanner = null;
      return;
    }
    const cameraId = cameraSelect.value;
    if (!cameraId) {
      cameraMessage.textContent = 'Không tìm thấy camera. Hãy nhập mã vé hoặc quét từ ảnh QR.';
      return;
    }
    cameraToggle.disabled = true;
    cameraMessage.textContent = 'Đang yêu cầu quyền truy cập camera…';
    try {
      scanner = new Html5Qrcode('reader', { verbose: false });
      await scanner.start(cameraId, {
        fps: 10,
        qrbox: viewfinder => {
          const size = Math.max(160, Math.min(280, viewfinder.width - 36, viewfinder.height - 36));
          return { width: size, height: size };
        },
        aspectRatio: 1
      }, text => submit(text), () => {});
      cameraRunning = true;
      cameraDot?.classList.add('is-active');
      cameraToggle.textContent = 'Tắt camera';
      cameraMessage.textContent = 'Camera đang hoạt động. Đưa mã QR vào khung quét.';
    } catch (error) {
      scanner = null;
      cameraMessage.textContent = error?.name === 'NotAllowedError'
        ? 'Bạn chưa cấp quyền camera. Hãy cho phép camera trong trình duyệt rồi thử lại.'
        : 'Không mở được camera. Hãy kiểm tra quyền truy cập, HTTPS và thiết bị camera.';
    } finally {
      cameraToggle.disabled = false;
    }
  }

  manualBtn?.addEventListener('click', () => submit(manual.value));
  manual?.addEventListener('keydown', event => { if (event.key === 'Enter') submit(manual.value); });
  cameraToggle?.addEventListener('click', toggleCamera);
  imageInput?.addEventListener('change', async () => {
    const file = imageInput.files?.[0];
    if (!file || !window.Html5Qrcode) return;
    try {
      const imageScanner = new Html5Qrcode('reader', { verbose: false });
      const decoded = await imageScanner.scanFile(file, true);
      await imageScanner.clear();
      await submit(decoded);
    } catch {
      cameraMessage.textContent = 'Không đọc được mã QR trong ảnh. Hãy chọn ảnh rõ nét hơn.';
    } finally {
      imageInput.value = '';
    }
  });
  loadCameras();
})();
