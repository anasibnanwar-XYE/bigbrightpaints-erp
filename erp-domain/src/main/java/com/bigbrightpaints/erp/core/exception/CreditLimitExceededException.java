package com.bigbrightpaints.erp.core.exception;

public class CreditLimitExceededException extends ApplicationException {
    public CreditLimitExceededException(String userMessage) {
        super(ErrorCode.BUSINESS_LIMIT_EXCEEDED, userMessage);
    }
}
