package com.rexchain.cinema.service;

import com.rexchain.cinema.config.SePayProperties;
import com.rexchain.cinema.dto.PaymentSettingsDto;
import com.rexchain.cinema.dto.PaymentSettingsUpdateRequest;
import com.rexchain.cinema.repository.PaymentSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentSettingsService {
    private final PaymentSettingsRepository repository;
    private final SePayProperties sepay;

    public PaymentSettingsService(PaymentSettingsRepository repository, SePayProperties sepay) {
        this.repository = repository; this.sepay = sepay;
    }

    public PaymentSettingsDto current() {
        PaymentSettingsDto value = repository.findBankQr().orElseGet(this::fromEnvironment);
        if (value.getBankName() == null || value.getBankName().isBlank()) value.setBankName(sepay.getBankName());
        if (value.getAccountHolder() == null || value.getAccountHolder().isBlank()) value.setAccountHolder(sepay.getAccountHolder());
        if (value.getBankCode() == null || value.getBankCode().isBlank()) value.setBankCode(sepay.getBankCode());
        if (value.getBankAccount() == null || value.getBankAccount().isBlank()) value.setBankAccount(sepay.getBankAccount());
        if (value.getPaymentPrefix() == null || value.getPaymentPrefix().isBlank()) value.setPaymentPrefix(sepay.getPaymentPrefix());
        value.setSchemaAvailable(repository.isSchemaAvailable());
        return value;
    }

    @Transactional
    public PaymentSettingsDto save(PaymentSettingsUpdateRequest request, String actor) {
        validate(request);
        PaymentSettingsDto value = current();
        value.setEnabled(request.isEnabled()); value.setBankName(clean(request.getBankName()));
        value.setBankCode(clean(request.getBankCode())); value.setBankAccount(clean(request.getBankAccount()));
        value.setAccountHolder(clean(request.getAccountHolder())); value.setDefaultAccount(request.isDefaultAccount());
        value.setPaymentTimeoutMinutes(SeatReservationService.CHECKOUT_HOLD_MINUTES);
        value.setPaymentPrefix(clean(request.getPaymentPrefix()));
        value.setPaymentDescriptionTemplate(clean(request.getPaymentDescriptionTemplate()));
        return repository.save(value, actor);
    }

    private PaymentSettingsDto fromEnvironment() {
        PaymentSettingsDto value = new PaymentSettingsDto();
        value.setEnabled(sepay.isEnabled()); value.setBankName(sepay.getBankName()); value.setAccountHolder(sepay.getAccountHolder());
        value.setBankCode(sepay.getBankCode()); value.setBankAccount(sepay.getBankAccount()); value.setPaymentPrefix(sepay.getPaymentPrefix());
        value.setSchemaAvailable(false); return value;
    }

    private void validate(PaymentSettingsUpdateRequest request) {
        if (request.getBankName() == null || request.getBankName().trim().length() > 120 || request.getBankName().trim().isEmpty()) throw new IllegalArgumentException("Vui lòng chọn ngân hàng");
        if (request.getBankCode() == null || !request.getBankCode().trim().matches("[A-Za-z0-9]{3,20}")) throw new IllegalArgumentException("Mã ngân hàng/BIN không hợp lệ");
        if (request.getBankAccount() == null || !request.getBankAccount().trim().matches("[0-9]{6,30}")) throw new IllegalArgumentException("Số tài khoản phải có từ 6 đến 30 chữ số");
        if (request.getPaymentTimeoutMinutes() != 0
                && request.getPaymentTimeoutMinutes() != SeatReservationService.CHECKOUT_HOLD_MINUTES)
            throw new IllegalArgumentException("Thời hạn giữ chỗ là quy tắc cố định "
                    + SeatReservationService.CHECKOUT_HOLD_MINUTES + " phút, không cấu hình được");
        if (request.getPaymentPrefix() == null || !request.getPaymentPrefix().trim().matches("[A-Za-z0-9]{2,20}")) throw new IllegalArgumentException("Tiền tố mã thanh toán không hợp lệ");
        if (request.getPaymentDescriptionTemplate() == null || request.getPaymentDescriptionTemplate().trim().length() > 120) throw new IllegalArgumentException("Mẫu nội dung chuyển khoản không hợp lệ");
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
}
