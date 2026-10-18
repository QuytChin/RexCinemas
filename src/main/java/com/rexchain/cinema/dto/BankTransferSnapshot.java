package com.rexchain.cinema.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BankTransferSnapshot {
    private String bookingCode;
    private String bankName;
    private String bankCode;
    private String bankAccount;
    private String accountHolder;
    private BigDecimal amount;
    private String paymentReference;
    private LocalDateTime expiresAt;

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String value) { bookingCode = value; }
    public String getBankName() { return bankName; }
    public void setBankName(String value) { bankName = value; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String value) { bankCode = value; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String value) { bankAccount = value; }
    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String value) { accountHolder = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String value) { paymentReference = value; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime value) { expiresAt = value; }
}
