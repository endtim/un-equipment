package com.unequipment.platform.common.exception;

public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static final int SUCCESS = 200;

    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;

    public static final int BIZ_ERROR = 40000;
    public static final int INVALID_REQUEST = 40001;
    public static final int RESOURCE_NOT_FOUND = 40400;
    public static final int PERMISSION_DENIED = 40300;
    public static final int INTERNAL_ERROR = 50000;

    public static final int AUTH_INVALID_CREDENTIALS = 41001;
    public static final int AUTH_USERNAME_EXISTS = 41002;

    public static final int ORDER_TIME_RANGE_INVALID = 42001;
    public static final int ORDER_TIME_CONFLICT = 42002;
    public static final int ORDER_INVALID_ACTION = 42003;
    public static final int ORDER_STATUS_NOT_ALLOWED = 42004;
    public static final int ORDER_TYPE_NOT_ALLOWED = 42005;
    public static final int ORDER_NOT_FOUND = 42006;

    public static final int FINANCE_INSUFFICIENT_BALANCE = 43001;
    public static final int FINANCE_ACCOUNT_NOT_FOUND = 43002;
}
