(() => {
  const MAX_VISIBLE = 3;
  const MAX_QUEUED = 20;
  const recent = new Map();
  const queue = [];
  let visible = 0;
  let confirmQueue = [];
  let activeConfirm = null;
  let dialog;

  const icons = { success: '✓', error: '!', warning: '!', info: 'i' };
  const defaults = { success: 5000, info: 5500, warning: 7000, error: 9000 };
  const technicalDetails = /(?:\b(?:java|org|com)\.[A-Za-z_$][\w.$]*|SQLState|SQL syntax|HibernateException|stack trace|\bat\s+[\w.$]+\(|jdbc:(?:mysql|postgresql):|api[_ -]?key|\bBearer\s+[A-Za-z0-9._-]+|\beyJ[A-Za-z0-9_-]{10,})/i;
  const genericError = 'Có lỗi xảy ra. Vui lòng thử lại. Nếu tình trạng vẫn tiếp diễn, hãy liên hệ hỗ trợ.';

  function safeMessage(value) {
    const text = String(value ?? '').trim();
    return technicalDetails.test(text) ? genericError : text;
  }

  function ensureRegion() {
    let region = document.getElementById('rex-toast-region');
    if (region) return region;
    region = document.createElement('div');
    region.id = 'rex-toast-region';
    region.className = 'rex-toast-region';
    region.setAttribute('aria-label', 'Thông báo');
    region.setAttribute('aria-live', 'polite');
    region.setAttribute('aria-relevant', 'additions text');
    const header = document.querySelector('.rex-nav');
    if (header) header.insertAdjacentElement('afterend', region);
    else document.body.prepend(region);
    return region;
  }

  function scheduleNext() {
    while (visible < MAX_VISIBLE && queue.length) showToast(queue.shift());
  }

  function showToast(item) {
    const region = ensureRegion();
    const toast = document.createElement('section');
    toast.className = `rex-toast rex-toast--${item.type}`;
    toast.setAttribute('role', item.type === 'error' ? 'alert' : 'status');
    toast.setAttribute('aria-atomic', 'true');

    const icon = document.createElement('span');
    icon.className = 'rex-toast__icon';
    icon.setAttribute('aria-hidden', 'true');
    icon.textContent = icons[item.type];

    const message = document.createElement('p');
    message.className = 'rex-toast__message';
    message.textContent = item.message;

    const close = document.createElement('button');
    close.className = 'rex-toast__close';
    close.type = 'button';
    close.setAttribute('aria-label', 'Đóng thông báo');
    close.textContent = '×';

    const content = document.createElement('div');
    content.className = 'rex-toast__content';
    content.append(icon, message, close);
    toast.append(content);
    if (item.duration > 0) {
      const progress = document.createElement('span');
      progress.className = 'rex-toast__progress';
      progress.style.setProperty('--rex-toast-duration', `${item.duration}ms`);
      toast.append(progress);
    }

    let timer = null;
    let startedAt = Date.now();
    let remaining = item.duration;
    let removed = false;
    const dismiss = () => {
      if (removed) return;
      removed = true;
      if (timer) clearTimeout(timer);
      toast.classList.add('is-leaving');
      window.setTimeout(() => {
        toast.remove();
        visible -= 1;
        scheduleNext();
        if (!visible && !queue.length) document.body.classList.remove('rex-toast-active');
      }, 180);
    };
    const resume = () => {
      if (remaining <= 0 || removed || timer) return;
      startedAt = Date.now();
      timer = window.setTimeout(dismiss, remaining);
      toast.style.setProperty('--rex-toast-duration', `${remaining}ms`);
    };
    const pause = () => {
      if (!timer) return;
      clearTimeout(timer);
      timer = null;
      remaining = Math.max(0, remaining - (Date.now() - startedAt));
    };

    close.addEventListener('click', dismiss);
    toast.addEventListener('mouseenter', pause);
    toast.addEventListener('mouseleave', resume);
    toast.addEventListener('focusin', pause);
    toast.addEventListener('focusout', event => {
      if (!toast.contains(event.relatedTarget)) resume();
    });
    region.append(toast);
    document.body.classList.add('rex-toast-active');
    visible += 1;
    requestAnimationFrame(() => toast.classList.add('is-visible'));
    if (item.duration > 0) resume();
  }

  function toast(message, options = {}) {
    const text = safeMessage(message);
    if (!text) return false;
    const type = ['success', 'error', 'warning', 'info'].includes(options.type) ? options.type : 'info';
    const duration = options.persistent ? 0 : Math.max(0, Number(options.duration ?? defaults[type]));
    const key = options.dedupeKey || `${type}:${text}`;
    const now = Date.now();
    if (recent.has(key) && now - recent.get(key) < 1400) return false;
    recent.set(key, now);
    for (const [entry, at] of recent) if (now - at > 5000) recent.delete(entry);

    const item = { message: text, type, duration };
    if (visible < MAX_VISIBLE) showToast(item);
    else if (queue.length < MAX_QUEUED) queue.push(item);
    else if (type === 'error' || type === 'warning') {
      const replaceIndex = queue.findIndex(queued => queued.type === 'info' || queued.type === 'success');
      if (replaceIndex >= 0) queue.splice(replaceIndex, 1, item);
      else return false;
    } else return false;
    return true;
  }

  function initializeFlashMessages() {
    document.querySelectorAll('.alert.alert-success:not([data-rex-persistent])').forEach(alert => {
      const message = alert.textContent.trim();
      if (message && !wasShownOnReload(message)) toast(message, { type: 'success', dedupeKey: `flash:${location.pathname}:${message}` });
      alert.remove();
    });
    document.querySelectorAll('.alert.alert-danger, .alert.alert-warning, .alert.alert-info').forEach(alert => {
      const originalText = alert.textContent.trim();
      if (alert.classList.contains('alert-danger') && technicalDetails.test(originalText)) {
        const dismiss = alert.querySelector('[data-bs-dismiss="alert"]');
        alert.replaceChildren(document.createTextNode(genericError));
        if (dismiss) alert.append(dismiss);
      }
      if (!alert.hasAttribute('role')) alert.setAttribute('role', alert.classList.contains('alert-danger') ? 'alert' : 'status');
      if (!alert.hasAttribute('aria-live')) alert.setAttribute('aria-live', alert.classList.contains('alert-danger') ? 'assertive' : 'polite');
    });
  }

  function wasShownOnReload(message) {
    try {
      let hash = 2166136261;
      for (const char of `${location.search}\u0000${message}`) hash = Math.imul(hash ^ char.charCodeAt(0), 16777619);
      const key = `rex-flash:${location.pathname}`;
      const value = String(hash >>> 0);
      const navigationType = performance.getEntriesByType('navigation')[0]?.type;
      const duplicate = navigationType === 'reload' && sessionStorage.getItem(key) === value;
      sessionStorage.setItem(key, value);
      return duplicate;
    } catch {
      return false;
    }
  }

  function buildDialog() {
    dialog = document.createElement('dialog');
    dialog.id = 'rex-confirm-dialog';
    dialog.className = 'rex-confirm-dialog';
    dialog.setAttribute('aria-labelledby', 'rex-confirm-title');
    dialog.setAttribute('aria-describedby', 'rex-confirm-description');
    dialog.innerHTML = '<form method="dialog" class="rex-confirm-dialog__panel"><span class="rex-confirm-dialog__icon" aria-hidden="true">!</span><h2 id="rex-confirm-title">Xác nhận thao tác</h2><p id="rex-confirm-description"></p><div class="rex-confirm-dialog__actions"><button type="button" class="rex-confirm-dialog__cancel">Hủy</button><button type="button" class="rex-confirm-dialog__accept">Xác nhận</button></div></form>';
    document.body.append(dialog);

    const cancel = dialog.querySelector('.rex-confirm-dialog__cancel');
    const accept = dialog.querySelector('.rex-confirm-dialog__accept');
    cancel.addEventListener('click', () => dialog.close('cancel'));
    accept.addEventListener('click', () => dialog.close('confirm'));
    dialog.addEventListener('click', event => {
      if (event.target === dialog) dialog.close('cancel');
    });
    dialog.addEventListener('close', () => {
      const request = activeConfirm;
      activeConfirm = null;
      if (request) request.resolve(dialog.returnValue === 'confirm');
      window.setTimeout(showNextConfirm, 0);
    });
  }

  function showNextConfirm() {
    if (activeConfirm || !confirmQueue.length) return;
    if (!dialog) buildDialog();
    activeConfirm = confirmQueue.shift();
    dialog.querySelector('#rex-confirm-title').textContent = activeConfirm.title;
    dialog.querySelector('#rex-confirm-description').textContent = activeConfirm.message;
    const accept = dialog.querySelector('.rex-confirm-dialog__accept');
    accept.classList.toggle('is-destructive', activeConfirm.destructive);
    accept.classList.toggle('is-warning', activeConfirm.warning);
    accept.textContent = activeConfirm.confirmLabel;
    dialog.showModal();
    dialog.querySelector('.rex-confirm-dialog__cancel').focus();
  }

  function confirm(options = {}) {
    return new Promise(resolve => {
      confirmQueue.push({
        title: options.title || 'Xác nhận thao tác',
        message: options.message || 'Bạn có chắc muốn tiếp tục thao tác này?',
        confirmLabel: options.confirmLabel || 'Xác nhận',
        destructive: Boolean(options.destructive),
        warning: Boolean(options.warning),
        resolve
      });
      showNextConfirm();
    });
  }

  function interceptBrowserConfirm(event) {
    const form = event.target;
    if (!(form instanceof HTMLFormElement)) return;
    if (form.dataset.rexConfirmApproved === 'true') {
      delete form.dataset.rexConfirmApproved;
      return;
    }
    const handler = form.getAttribute('onsubmit');
    const match = handler?.match(/confirm\((['"])([\s\S]*?)\1\)/i);
    const dataMessage = form.getAttribute('data-confirm');
    const action = form.getAttribute('action') || form.action || '';
    const endpointRisk = /\/(?:delete|cancel|refund(?:-|\/|$)|toggle|review)(?:\/|$|[?#])/i.test(action);
    if (!match && !dataMessage && !endpointRisk) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    const submitter = event.submitter;
    const label = (submitter?.innerText || submitter?.value || '').trim();
    const message = dataMessage || (match
      ? match[2].replace(/\\'/g, "'").replace(/\\n/g, '\n')
      : /\/delete(?:\/|$|[?#])/i.test(action)
        ? 'Mục này sẽ bị xóa khỏi hệ thống. Bạn có chắc muốn tiếp tục?'
        : /\/cancel(?:\/|$|[?#])/i.test(action)
          ? 'Thao tác này sẽ hủy booking và cập nhật tình trạng ghế. Bạn có chắc muốn tiếp tục?'
          : /\/refund(?:-|\/|$)/i.test(action)
            ? 'Thao tác này cập nhật trạng thái xử lý hoàn tiền. Hãy kiểm tra chứng từ trước khi tiếp tục.'
            : /\/review(?:\/|$|[?#])/i.test(action)
              ? 'Trạng thái yêu cầu sẽ được chuyển sang đang xử lý. Bạn có muốn tiếp tục?'
            : 'Trạng thái của mục này sẽ được thay đổi. Bạn có muốn tiếp tục?');
    const destructive = /xóa|xoá|hủy|huy|khóa|khoa|delete|cancel/i.test(label) || /\/delete|\/cancel/i.test(action);
    const financial = /hoàn tiền|refund|chuyển khoản/i.test(message) || /\/refund(?:-|\/|$)/i.test(action);
    confirm({
      title: label ? `Xác nhận: ${label}` : 'Xác nhận thao tác',
      message,
      confirmLabel: destructive || financial ? 'Tiếp tục' : 'Xác nhận',
      destructive,
      warning: financial && !destructive
    }).then(approved => {
      if (!approved) return;
      form.dataset.rexConfirmApproved = 'true';
      if (handler) form.removeAttribute('onsubmit');
      try {
        form.requestSubmit(submitter || undefined);
      } catch {
        delete form.dataset.rexConfirmApproved;
        toast('Không thể tiếp tục thao tác. Vui lòng kiểm tra lại biểu mẫu.', { type: 'error' });
      } finally {
        if (handler) window.setTimeout(() => form.setAttribute('onsubmit', handler), 0);
      }
    });
  }

  window.RexNotifications = Object.freeze({ toast, confirm });

  function initialize() {
    ensureRegion();
    initializeFlashMessages();
    document.addEventListener('submit', interceptBrowserConfirm, true);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initialize, { once: true });
  else initialize();
})();
