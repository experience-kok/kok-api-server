package com.example.auth.exception;

/**
 * 이메일 발송 관련 예외
 */
public class EmailSendException extends RuntimeException {
    
    private final String errorCode;
    private final String recipientEmail;
    
    public EmailSendException(String message, String errorCode, String recipientEmail, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.recipientEmail = recipientEmail;
    }
    
    public EmailSendException(String message, String errorCode, String recipientEmail) {
        super(message);
        this.errorCode = errorCode;
        this.recipientEmail = recipientEmail;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getRecipientEmail() {
        return recipientEmail;
    }
}
