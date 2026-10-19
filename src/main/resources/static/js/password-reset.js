(function () {
    'use strict';

    const requestForm = document.getElementById('resetRequestForm');
    const verifyForm = document.getElementById('resetVerifyForm');
    const requestStep = document.getElementById('reset-request-step');
    const verifyStep = document.getElementById('reset-verify-step');
    const alertBox = document.getElementById('resetAlert');
    const emailInput = document.getElementById('resetEmail');
    const otpInput = document.getElementById('resetOtp');
    const sendButton = document.getElementById('btnSendResetOtp');
    const verifyButton = document.getElementById('btnVerifyReset');
    let resetEmail = '';
    let resetToken = '';

    const headers = () => {
        const pair = document.cookie.split('; ').find(x => x.startsWith('XSRF-TOKEN='));
        const result = { 'Content-Type': 'application/json' };
        if (pair) result['X-XSRF-TOKEN'] = decodeURIComponent(pair.substring('XSRF-TOKEN='.length));
        const csrf = document.getElementById('csrfToken');
        if (csrf?.value) result['X-CSRF-TOKEN'] = csrf.value;
        return result;
    };

    const show = (message, success) => {
        if (!alertBox) return;
        alertBox.textContent = message;
        alertBox.className = success ? 'alert alert-success' : 'alert alert-danger';
        alertBox.classList.remove('d-none');
    };

    const responseJson = async response => {
        const body = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(body.error || 'Không thể xử lý yêu cầu lúc này.');
        return body;
    };

    requestForm?.addEventListener('submit', async event => {
        event.preventDefault();
        if (!requestForm.checkValidity()) { requestForm.reportValidity(); return; }
        resetEmail = emailInput.value.trim().toLowerCase();
        sendButton.disabled = true;
        sendButton.textContent = 'ĐANG GỬI...';
        try {
            const response = await fetch('/api/auth/forgot-password/send-otp', {
                method: 'POST', headers: headers(), body: JSON.stringify({ email: resetEmail })
            });
            const result = await responseJson(response);
            requestStep.classList.add('d-none');
            verifyStep.classList.remove('d-none');
            show(result.message, true);
            otpInput?.focus();
        } catch (error) {
            show(error.message);
        } finally {
            sendButton.disabled = false;
            sendButton.textContent = 'GỬI MÃ OTP';
        }
    });

    verifyForm?.addEventListener('submit', async event => {
        event.preventDefault();
        if (!verifyForm.checkValidity()) { verifyForm.reportValidity(); return; }
        verifyButton.disabled = true;
        verifyButton.textContent = 'ĐANG XÁC THỰC...';
        try {
            const verifyResponse = await fetch('/api/auth/forgot-password/verify-otp', {
                method: 'POST', headers: headers(), body: JSON.stringify({ email: resetEmail, otp: otpInput.value.trim() })
            });
            const verified = await responseJson(verifyResponse);
            resetToken = verified.resetToken;
            const newPassword = document.getElementById('resetNewPassword').value;
            const confirmPassword = document.getElementById('resetConfirmPassword').value;
            const resetResponse = await fetch('/api/auth/forgot-password/reset', {
                method: 'POST', headers: headers(),
                body: JSON.stringify({ email: resetEmail, resetToken, newPassword, confirmPassword })
            });
            const result = await responseJson(resetResponse);
            show('Đặt lại mật khẩu thành công. Đang chuyển tới đăng nhập...', true);
            setTimeout(() => { window.location.href = result.redirectUrl || '/login?reset=success'; }, 900);
        } catch (error) {
            show(error.message);
            verifyButton.disabled = false;
            verifyButton.textContent = 'XÁC NHẬN & ĐẶT LẠI';
        }
    });
}());
