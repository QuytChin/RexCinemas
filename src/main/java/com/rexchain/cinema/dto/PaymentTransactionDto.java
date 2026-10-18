package com.rexchain.cinema.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Read-only reconciliation record for one provider transaction. */
public class PaymentTransactionDto {
    private Long id;
    private String providerTransactionId;
    private String bookingCode;
    private String paymentReference;
    private String gateway;
    private String transactionDate;
    private String accountNumberLast4;
    private String code;
    private String content;
    private String transferType;
    private BigDecimal amount;
    private String referenceCode;
    private String status;
    private String reason;
    private LocalDateTime receivedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime settledAt;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public void setProviderTransactionId(String value) { providerTransactionId = value; }
    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String value) { bookingCode = value; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String value) { paymentReference = value; }
    public String getGateway() { return gateway; }
    public void setGateway(String value) { gateway = value; }
    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String value) { transactionDate = value; }
    public String getAccountNumberLast4() { return accountNumberLast4; }
    public void setAccountNumberLast4(String value) { accountNumberLast4 = value; }
    public String getCode() { return code; }
    public void setCode(String value) { code = value; }
    public String getContent() { return content; }
    public void setContent(String value) { content = value; }
    public String getTransferType() { return transferType; }
    public void setTransferType(String value) { transferType = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String value) { referenceCode = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime value) { receivedAt = value; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime value) { verifiedAt = value; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime value) { settledAt = value; }
}
