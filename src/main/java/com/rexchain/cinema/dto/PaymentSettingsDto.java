package com.rexchain.cinema.dto;

import java.time.LocalDateTime;

/** Non-secret payment configuration. Provider secrets never belong in this DTO. */
public class PaymentSettingsDto {
    private Long id;
    private String paymentMethod = "BANK_QR";
    private boolean enabled;
    private String bankName = "";
    private String bankCode = "";
    private String bankAccount = "";
    private String accountHolder = "";
    private boolean defaultAccount = true;
    private int paymentTimeoutMinutes = 5;
    private String paymentPrefix = "REX";
    private String paymentDescriptionTemplate = "{BOOKING_CODE}";
    private Long version;
    private LocalDateTime updatedAt;
    private boolean schemaAvailable;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }
    public boolean isDefaultAccount() { return defaultAccount; }
    public void setDefaultAccount(boolean defaultAccount) { this.defaultAccount = defaultAccount; }
    public int getPaymentTimeoutMinutes() { return paymentTimeoutMinutes; }
    public void setPaymentTimeoutMinutes(int paymentTimeoutMinutes) { this.paymentTimeoutMinutes = paymentTimeoutMinutes; }
    public String getPaymentPrefix() { return paymentPrefix; }
    public void setPaymentPrefix(String paymentPrefix) { this.paymentPrefix = paymentPrefix; }
    public String getPaymentDescriptionTemplate() { return paymentDescriptionTemplate; }
    public void setPaymentDescriptionTemplate(String paymentDescriptionTemplate) { this.paymentDescriptionTemplate = paymentDescriptionTemplate; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isSchemaAvailable() { return schemaAvailable; }
    public void setSchemaAvailable(boolean schemaAvailable) { this.schemaAvailable = schemaAvailable; }
}
