package com.rappidrive.domain.valueobjects;

import com.rappidrive.domain.enums.PaymentMethodType;

import java.util.Objects;

/**
 * Value object representing a payment method.
 * Immutable - once created, cannot be changed.
 */
public final class PaymentMethod {
    private final PaymentMethodType type;
    private final String cardLast4;
    private final String cardBrand;
    private final String pixKey;
    
    private PaymentMethod(PaymentMethodType type, String cardLast4, String cardBrand, String pixKey) {
        this.type = Objects.requireNonNull(type, "Payment method type cannot be null");
        this.cardLast4 = cardLast4;
        this.cardBrand = cardBrand;
        this.pixKey = pixKey;
        
        validate();
    }
    
    /**
     * Creates a credit card payment method.
     *
     * @param cardLast4 last 4 digits of the card
     * @param cardBrand card brand (VISA, MASTERCARD, etc.)
     * @return a new PaymentMethod instance
     */
    public static PaymentMethod creditCard(String cardLast4, String cardBrand) {
        if (cardLast4 == null || cardLast4.trim().isEmpty()) {
            throw new IllegalArgumentException("Card last 4 digits cannot be null or empty");
        }
        if (cardLast4.length() != 4 || !cardLast4.matches("\\d{4}")) {
            throw new IllegalArgumentException("Card last 4 digits must be exactly 4 digits");
        }
        if (cardBrand == null || cardBrand.trim().isEmpty()) {
            throw new IllegalArgumentException("Card brand cannot be null or empty");
        }
        return new PaymentMethod(PaymentMethodType.CREDIT_CARD, cardLast4, cardBrand.toUpperCase(), null);
    }
    
    /**
     * Creates a debit card payment method.
     *
     * @param cardLast4 last 4 digits of the card
     * @param cardBrand card brand (VISA, MASTERCARD, etc.)
     * @return a new PaymentMethod instance
     */
    public static PaymentMethod debitCard(String cardLast4, String cardBrand) {
        if (cardLast4 == null || cardLast4.trim().isEmpty()) {
            throw new IllegalArgumentException("Card last 4 digits cannot be null or empty");
        }
        if (cardLast4.length() != 4 || !cardLast4.matches("\\d{4}")) {
            throw new IllegalArgumentException("Card last 4 digits must be exactly 4 digits");
        }
        if (cardBrand == null || cardBrand.trim().isEmpty()) {
            throw new IllegalArgumentException("Card brand cannot be null or empty");
        }
        return new PaymentMethod(PaymentMethodType.DEBIT_CARD, cardLast4, cardBrand.toUpperCase(), null);
    }
    
    /**
     * Creates a PIX payment method.
     *
     * @param pixKey PIX key used for payment
     * @return a new PaymentMethod instance
     */
    public static PaymentMethod pix(String pixKey) {
        if (pixKey == null || pixKey.trim().isEmpty()) {
            throw new IllegalArgumentException("PIX key cannot be null or empty");
        }
        return new PaymentMethod(PaymentMethodType.PIX, null, null, pixKey);
    }
    
    /**
     * Creates a cash payment method.
     *
     * @return a new PaymentMethod instance
     */
    public static PaymentMethod cash() {
        return new PaymentMethod(PaymentMethodType.CASH, null, null, null);
    }
    
    private void validate() {
        if (type == PaymentMethodType.CREDIT_CARD || type == PaymentMethodType.DEBIT_CARD) {
            if (cardLast4 == null || cardBrand == null) {
                throw new IllegalArgumentException("Card payments must have cardLast4 and cardBrand");
            }
        } else if (type == PaymentMethodType.PIX) {
            if (pixKey == null) {
                throw new IllegalArgumentException("PIX payments must have pixKey");
            }
        }
        // CASH requires no additional validation
    }
    
    /**
     * Checks if this payment method can be refunded.
     *
     * @return true if refundable, false otherwise
     */
    public boolean canBeRefunded() {
        return type.isRefundable();
    }
    
    /**
     * Checks if this payment method requires gateway processing.
     *
     * @return true if requires processing, false otherwise
     */
    public boolean requiresGatewayProcessing() {
        return type.requiresGatewayProcessing();
    }
    
    public PaymentMethodType getType() {
        return type;
    }
    
    public String getCardLast4() {
        return cardLast4;
    }
    
    public String getCardBrand() {
        return cardBrand;
    }
    
    public String getPixKey() {
        return pixKey;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentMethod that = (PaymentMethod) o;
        return type == that.type &&
               Objects.equals(cardLast4, that.cardLast4) &&
               Objects.equals(cardBrand, that.cardBrand) &&
               Objects.equals(pixKey, that.pixKey);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, cardLast4, cardBrand, pixKey);
    }
    
    @Override
    public String toString() {
        if (type == PaymentMethodType.CREDIT_CARD) {
            return "Credit Card ****" + cardLast4 + " (" + cardBrand + ")";
        } else if (type == PaymentMethodType.DEBIT_CARD) {
            return "Debit Card ****" + cardLast4 + " (" + cardBrand + ")";
        } else if (type == PaymentMethodType.PIX) {
            String maskedPix = pixKey.length() > 4 
                ? "***" + pixKey.substring(pixKey.length() - 4) 
                : "***";
            return "PIX: " + maskedPix;
        } else {
            return "Cash";
        }
    }
}
