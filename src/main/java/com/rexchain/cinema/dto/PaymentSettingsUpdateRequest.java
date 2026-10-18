package com.rexchain.cinema.dto;

public class PaymentSettingsUpdateRequest {
    private boolean enabled;
    private String bankName;
    private String bankCode;
    private String bankAccount;
    private String accountHolder;
    private boolean defaultAccount;
    private int paymentTimeoutMinutes;
    private String paymentPrefix;
    private String paymentDescriptionTemplate;

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
}
