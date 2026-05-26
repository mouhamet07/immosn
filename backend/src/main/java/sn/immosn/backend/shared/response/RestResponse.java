package sn.immosn.backend.shared.response;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

public record RestResponse<T>(
    boolean success,
    int status,
    String message,
    T data,
    LocalDateTime timestamp
){
    public RestResponse(boolean success, HttpStatus status, String message, T data) {
        this(success, status.value(), message, data, LocalDateTime.now());
    }

    public static <T> RestResponse<T> success(T data, HttpStatus status) {
        return new RestResponse<>(true, status.value(), "Opération réalisée avec succès", data, LocalDateTime.now());
    }

    public static <T> RestResponse<T> error(String message, HttpStatus status) {
        return new RestResponse<>(false, status.value(), message, null, LocalDateTime.now());
    }

    public static <T> RestResponse<T> badRequest(String message, T data) {
        return new RestResponse<>(false, 400, message, data, LocalDateTime.now());
    }

    public static <T> RestResponse<T> notFound(String message) {
        return new RestResponse<>(false, 404, message, null, LocalDateTime.now());
    }

    public static <T> RestResponse<T> forbidden(String message) {
        return new RestResponse<>(false, 403, message, null, LocalDateTime.now());
    }
}
