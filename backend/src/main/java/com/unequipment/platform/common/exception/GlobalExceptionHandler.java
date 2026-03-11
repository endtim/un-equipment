package com.unequipment.platform.common.exception;

import com.unequipment.platform.common.api.ApiResponse;
import javax.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException ex) {
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValid(MethodArgumentNotValidException ex) {
        FieldError error = ex.getBindingResult().getFieldError();
        return ApiResponse.error(ErrorCodes.INVALID_REQUEST, error == null ? "invalid request" : error.getDefaultMessage());
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ApiResponse<Void> handleBadRequest(Exception ex) {
        return ApiResponse.error(ErrorCodes.INVALID_REQUEST, "invalid request");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleDefault(Exception ex) {
        return ApiResponse.error(ErrorCodes.INTERNAL_ERROR, "internal server error");
    }
}
