package sn.immosn.backend.shared.response;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

public record RestResponse<T>(
    boolean success,
    HttpStatus status,
    String message,
    T data,
    LocalDateTime timestamp
){
    public RestResponse(boolean success, HttpStatus status, String message, T data) {
        this(success, status, message, data, LocalDateTime.now());
    }
    public static <T> RestResponse<T> success(T data, HttpStatus status) {
        return new RestResponse<>(true, status, "Donnee recupere avec success", data, LocalDateTime.now());
    }
    public static <T> RestResponse<T> error(String message, HttpStatus status) {
        return new RestResponse<>(false, status, message, null, LocalDateTime.now());
    }
}