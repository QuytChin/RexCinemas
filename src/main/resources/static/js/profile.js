(() => {
  const picker = document.querySelector('[data-avatar-picker]');
  const file = document.querySelector('#avatarFile');
  const preview = document.querySelector('#profileAvatarPreview');
  const form = document.querySelector('#avatarForm');
  if (picker && file && form) {
    picker.addEventListener('click', () => file.click());
    file.addEventListener('change', () => {
      const selected = file.files?.[0]; if (!selected) return;
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(selected.type) || selected.size > 5 * 1024 * 1024) {
        file.value = ''; window.alert('Chỉ chọn ảnh JPG, PNG hoặc WebP tối đa 5 MB.'); return;
      }
      if (preview) preview.src = URL.createObjectURL(selected);
      form.requestSubmit();
    });
  }
  const passwordForm = document.querySelector('#passwordForm');
  passwordForm?.addEventListener('submit', event => {
    const current = passwordForm.querySelector('[name="currentPassword"]');
    const next = passwordForm.querySelector('[name="newPassword"]');
    const confirm = passwordForm.querySelector('[name="confirmPassword"]');
    if (!current.value || next.value.length < 8 || !/[A-Za-z]/.test(next.value) || !/\d/.test(next.value) || next.value !== confirm.value) {
      event.preventDefault(); window.alert('Kiểm tra mật khẩu hiện tại và xác nhận mật khẩu mới (ít nhất 8 ký tự, gồm chữ và số).');
    }
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
