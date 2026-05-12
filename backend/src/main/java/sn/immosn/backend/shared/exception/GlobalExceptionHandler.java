package sn.immosn.backend.shared.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import sn.immosn.backend.shared.response.RestResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<RestResponse> handleEntityNotFoundException(EntityNotFoundException ex){
        return ResponseEntity
        .status(404)
        .body(RestResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND));
    }
    @ExceptionHandler(EntityExistException.class)
    public ResponseEntity<RestResponse> handleEntityExistException(EntityExistException ex){
        return ResponseEntity
        .status(409)
        .body(RestResponse.error(ex.getMessage(), HttpStatus.CONFLICT));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse> handleException(Exception ex){
        return ResponseEntity
        .status(500)
        .body(RestResponse.error("Une erreur interne est survenue", HttpStatus.INTERNAL_SERVER_ERROR));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse> handleValidationException(MethodArgumentNotValidException ex){
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(RestResponse.badRequest("Erreur lors de la validation", errors));
    }
}
