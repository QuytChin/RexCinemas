(() => {
  const picker = document.querySelector('[data-avatar-picker]');
  const file = document.querySelector('#avatarFile');
  const preview = document.querySelector('#profileAvatarPreview');
  const form = document.querySelector('#avatarForm');
  const avatarError = document.querySelector('#avatarClientError');
  if (picker && file && form) {
    picker.addEventListener('click', () => file.click());
    file.addEventListener('change', () => {
      const selected = file.files?.[0]; if (!selected) return;
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(selected.type) || selected.size > 5 * 1024 * 1024) {
        file.value = '';
        if (avatarError) { avatarError.textContent = 'Vui lòng chọn ảnh JPG, PNG hoặc WebP có dung lượng tối đa 5 MB.'; avatarError.hidden = false; }
        return;
      }
      if (avatarError) { avatarError.textContent = ''; avatarError.hidden = true; }
      if (preview) preview.src = URL.createObjectURL(selected);
      form.requestSubmit();
    });
  }
  const passwordForm = document.querySelector('#passwordForm');
  passwordForm?.addEventListener('submit', event => {
    const current = passwordForm.querySelector('[name="currentPassword"]');
    const next = passwordForm.querySelector('[name="newPassword"]');
    const confirm = passwordForm.querySelector('[name="confirmPassword"]');
    const error = document.querySelector('#passwordClientError');
    if (!current.value || next.value.length < 8 || !/[A-Za-z]/.test(next.value) || !/\d/.test(next.value) || next.value !== confirm.value) {
      event.preventDefault();
      if (error) { error.textContent = 'Vui lòng kiểm tra mật khẩu hiện tại và xác nhận mật khẩu mới. Mật khẩu mới cần ít nhất 8 ký tự, gồm chữ và số.'; error.hidden = false; }
      (current.value ? next : current).focus();
    } else if (error) {
      error.textContent = ''; error.hidden = true;
    }
  });
  passwordForm?.addEventListener('input', () => {
    const error = document.querySelector('#passwordClientError');
    if (error && !error.hidden) { error.textContent = ''; error.hidden = true; }
  });
  document.querySelectorAll('[data-password-toggle]').forEach(toggle => toggle.addEventListener('click', () => {
    const input = toggle.parentElement.querySelector('input');
    const icon = toggle.querySelector('use');
    const visible = input.type === 'text';
    input.type = visible ? 'password' : 'text';
    toggle.setAttribute('aria-label', visible ? 'Hiện mật khẩu' : 'Ẩn mật khẩu');
    icon.setAttribute('href', `/images/icons.svg#${visible ? 'eye' : 'eye-off'}`);
  }));
})();
