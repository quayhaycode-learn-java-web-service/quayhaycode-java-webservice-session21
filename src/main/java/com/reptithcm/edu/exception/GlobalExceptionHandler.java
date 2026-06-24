package com.reptithcm.edu.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(AppException.class)
    public ResponseEntity<String> handleAppException(AppException appException){
        log.error("Have error with appException: " + appException.getMessage());
        return ResponseEntity.ok(appException.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e){
        log.error("Have error with appException: " + e.getMessage());
        return ResponseEntity.ok(e.getMessage());
    }
}
