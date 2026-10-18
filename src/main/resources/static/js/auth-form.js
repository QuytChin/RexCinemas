(function () {
    'use strict';

    document.querySelectorAll('[data-password-toggle]').forEach(function (button) {
        button.addEventListener('click', function () {
            const input = document.getElementById(button.dataset.passwordToggle);
            if (!input) return;
            const visible = input.type === 'text';
            input.type = visible ? 'password' : 'text';
            button.textContent = visible ? 'Hiện' : 'Ẩn';
            button.setAttribute('aria-label', visible ? 'Hiện mật khẩu' : 'Ẩn mật khẩu');
            button.setAttribute('aria-pressed', String(!visible));
            input.focus();
        });
    });

    document.querySelectorAll('[data-confirm-password]').forEach(function (confirmation) {
        const password = document.getElementById(confirmation.dataset.confirmPassword);
        if (!password) return;
        const validate = function () {
            confirmation.setCustomValidity(confirmation.value && confirmation.value !== password.value ? 'Mật khẩu xác nhận không khớp' : '');
        };
        password.addEventListener('input', validate);
        confirmation.addEventListener('input', validate);
    });

    document.querySelectorAll('form[data-auth-form]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!form.checkValidity()) return;
            const submit = form.querySelector('button[type="submit"]');
            if (!submit || submit.disabled) {
                event.preventDefault();
                return;
            }
            submit.disabled = true;
            submit.textContent = 'ĐANG XỬ LÝ...';
            submit.setAttribute('aria-busy', 'true');
        });
    });
}());
