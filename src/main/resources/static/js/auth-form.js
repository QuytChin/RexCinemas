(function () {
    'use strict';

    // Helper CSRF Headers
    const getCsrfHeaders = () => {
        const pair = document.cookie.split('; ').find(x => x.startsWith('XSRF-TOKEN='));
        const headers = { 'Content-Type': 'application/json' };
        if (pair) {
            headers['X-XSRF-TOKEN'] = decodeURIComponent(pair.substring('XSRF-TOKEN='.length));
        }
        const tokenInput = document.getElementById('csrfToken');
        if (tokenInput && tokenInput.value) {
            headers['X-CSRF-TOKEN'] = tokenInput.value;
        }
        return headers;
    };

    // Toggle ẩn / hiện mật khẩu
    document.querySelectorAll('[data-password-toggle]').forEach(function (button) {
        button.addEventListener('click', function () {
            const input = document.getElementById(button.dataset.passwordToggle);
            if (!input) return;
            const visible = input.type === 'text';
            input.type = visible ? 'password' : 'text';
            button.textContent = visible ? '◉' : '◎';
            button.setAttribute('aria-label', visible ? 'Hiện mật khẩu' : 'Ẩn mật khẩu');
            button.setAttribute('aria-pressed', String(!visible));
            input.focus();
        });
    });

    // Kiểm tra khớp mật khẩu
    document.querySelectorAll('[data-confirm-password]').forEach(function (confirmation) {
        const password = document.getElementById(confirmation.dataset.confirmPassword);
        if (!password) return;
        const validate = function () {
            confirmation.setCustomValidity(confirmation.value && confirmation.value !== password.value ? 'Mật khẩu xác nhận không khớp' : '');
        };
        password.addEventListener('input', validate);
        confirmation.addEventListener('input', validate);
    });

    // Form submit mặc định (cho trang Login...)
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

    // ==========================================
    // LOGIC ĐĂNG KÝ VỚI MÃ OTP GỬI QUA GMAIL
    // ==========================================
    const registerForm = document.getElementById('registerForm');
    const otpForm = document.getElementById('otpForm');
    const inPageAlert = document.getElementById('inPageAlert');
    const stepRegister = document.getElementById('step-register');
    const stepOtp = document.getElementById('step-otp');
    const displayEmail = document.getElementById('displayEmail');
    const btnSubmitRegister = document.getElementById('btnSubmitRegister');
    const btnVerifyOtp = document.getElementById('btnVerifyOtp');
    const btnResendOtp = document.getElementById('btnResendOtp');
    const btnBackToForm = document.getElementById('btnBackToForm');
    const resendCountdown = document.getElementById('resendCountdown');
    const otpInput = document.getElementById('otpInput');

    let countdownTimer = null;
    let registeredEmail = '';

    function showAlert(msg, isSuccess = false) {
        if (!inPageAlert) return;
        inPageAlert.textContent = msg;
        inPageAlert.className = isSuccess ? 'alert alert-success' : 'alert alert-danger';
        inPageAlert.classList.remove('d-none');
        inPageAlert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function clearAlert() {
        if (!inPageAlert) return;
        inPageAlert.classList.add('d-none');
        inPageAlert.textContent = '';
    }

    function startResendTimer(seconds = 60) {
        if (!btnResendOtp || !resendCountdown) return;
        btnResendOtp.disabled = true;
        resendCountdown.textContent = seconds;
        if (countdownTimer) clearInterval(countdownTimer);

        countdownTimer = setInterval(() => {
            seconds--;
            resendCountdown.textContent = seconds;
            if (seconds <= 0) {
                clearInterval(countdownTimer);
                btnResendOtp.disabled = false;
                btnResendOtp.textContent = 'Gửi lại mã OTP';
            }
        }, 1000);
    }

    if (registerForm) {
        registerForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            clearAlert();

            if (!registerForm.checkValidity()) {
                registerForm.reportValidity();
                return;
            }

            const password = document.getElementById('password')?.value;
            const confirmPassword = document.getElementById('confirmPassword')?.value;
            if (password !== confirmPassword) {
                showAlert('Mật khẩu xác nhận không khớp. Vui lòng kiểm tra lại.');
                return;
            }

            const genderRadio = registerForm.querySelector('input[name="gender"]:checked');
            const data = {
                fullName: document.getElementById('fullName')?.value.trim(),
                email: document.getElementById('email')?.value.trim().toLowerCase(),
                phone: document.getElementById('phone')?.value.trim(),
                gender: genderRadio ? genderRadio.value : 'Nam',
                birthDate: document.getElementById('birthDate')?.value,
                password: password,
                confirmPassword: confirmPassword,
                termsAccepted: document.getElementById('termsAccepted')?.checked || false
            };

            registeredEmail = data.email;
            btnSubmitRegister.disabled = true;
            btnSubmitRegister.textContent = 'ĐANG GỬI MÃ OTP...';

            try {
                const response = await fetch('/auth/register/send-otp', {
                    method: 'POST',
                    headers: getCsrfHeaders(),
                    body: JSON.stringify(data)
                });

                const result = await response.json();
                if (!response.ok) {
                    throw new Error(result.error || 'Không thể gửi mã OTP');
                }

                // Gửi OTP thành công -> chuyển sang bước 2
                stepRegister.classList.add('d-none');
                stepOtp.classList.remove('d-none');
                if (displayEmail) displayEmail.textContent = registeredEmail;
                showAlert(result.message || 'Mã OTP đã được gửi đến email của bạn!', true);
                startResendTimer(60);
                setTimeout(() => {
                    otpInput?.focus();
                }, 200);
            } catch (err) {
                showAlert(err.message || 'Lỗi kết nối máy chủ');
            } finally {
                btnSubmitRegister.disabled = false;
                btnSubmitRegister.textContent = 'HOÀN THÀNH';
            }
        });
    }

    if (otpForm) {
        otpForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            clearAlert();

            const otp = otpInput?.value.trim();
            if (!otp || otp.length !== 6) {
                showAlert('Vui lòng nhập đầy đủ 6 chữ số mã OTP.');
                return;
            }

            btnVerifyOtp.disabled = true;
            btnVerifyOtp.textContent = 'ĐANG XÁC THỰC...';

            try {
                const response = await fetch('/auth/register/verify-otp', {
                    method: 'POST',
                    headers: getCsrfHeaders(),
                    body: JSON.stringify({
                        email: registeredEmail,
                        otp: otp
                    })
                });

                const result = await response.json();
                if (!response.ok) {
                    throw new Error(result.error || 'Xác thực OTP thất bại');
                }

                // Đăng ký thành công -> chuyển về trang login
                showAlert('Đăng ký tài khoản thành công! Đang chuyển hướng...', true);
                setTimeout(() => {
                    window.location.href = result.redirectUrl || '/login?registered';
                }, 1200);
            } catch (err) {
                showAlert(err.message || 'Mã OTP không hợp lệ');
                btnVerifyOtp.disabled = false;
                btnVerifyOtp.textContent = 'XÁC NHẬN & ĐĂNG KÝ';
            }
        });
    }

    if (btnResendOtp) {
        btnResendOtp.addEventListener('click', async function () {
            if (btnResendOtp.disabled) return;
            clearAlert();
            btnResendOtp.disabled = true;
            btnResendOtp.textContent = 'Đang gửi lại...';

            try {
                const response = await fetch('/auth/register/resend-otp', {
                    method: 'POST',
                    headers: getCsrfHeaders(),
                    body: JSON.stringify({ email: registeredEmail })
                });
                const result = await response.json();
                if (!response.ok) {
                    throw new Error(result.error || 'Không thể gửi lại mã OTP');
                }
                showAlert(result.message || 'Đã gửi lại mã OTP mới về email của bạn!', true);
                startResendTimer(60);
            } catch (err) {
                showAlert(err.message || 'Lỗi khi gửi lại mã OTP');
                btnResendOtp.disabled = false;
                btnResendOtp.textContent = 'Gửi lại mã OTP';
            }
        });
    }

    if (btnBackToForm) {
        btnBackToForm.addEventListener('click', function () {
            clearAlert();
            stepOtp.classList.add('d-none');
            stepRegister.classList.remove('d-none');
        });
    }
}());
