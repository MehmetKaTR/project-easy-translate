package com.mehmetkatr.project_easy_translate.exception;

public class MailDeliveryException extends RuntimeException {
    public MailDeliveryException(String message) {
        super(message);
    }

    public MailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
