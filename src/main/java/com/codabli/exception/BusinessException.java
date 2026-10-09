package com.codabli.exception;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object[] args;

    public BusinessException(ErrorCode errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public Object[] getArgs() { return args; }
}
