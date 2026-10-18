package com.rexchain.cinema.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sepay")
public class SePayProperties {
    private boolean enabled;
    private String apiKey = "";
    private String bankAccount = "";
    private String bankCode = "";
    private String webhookSecret = "";
    private String webhookAuthMode = "API_KEY";
    private int webhookTimestampToleranceSeconds = 300;
    private String paymentPrefix = "REX";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    public String getWebhookAuthMode() { return webhookAuthMode; }
    public void setWebhookAuthMode(String webhookAuthMode) { this.webhookAuthMode = webhookAuthMode; }
    public int getWebhookTimestampToleranceSeconds() { return webhookTimestampToleranceSeconds; }
    public void setWebhookTimestampToleranceSeconds(int value) { webhookTimestampToleranceSeconds = value; }
    public String getPaymentPrefix() { return paymentPrefix; }
    public void setPaymentPrefix(String paymentPrefix) { this.paymentPrefix = paymentPrefix; }
}
