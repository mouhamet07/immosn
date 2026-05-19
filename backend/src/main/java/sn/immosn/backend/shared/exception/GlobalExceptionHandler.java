package sn.immosn.backend.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import sn.immosn.backend.shared.response.RestResponse;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<RestResponse<Void>> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.debug("EntityNotFoundException : {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(RestResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(EntityExistException.class)
    public ResponseEntity<RestResponse<Void>> handleEntityExistException(EntityExistException ex) {
        log.debug("EntityExistException : {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(RestResponse.error(ex.getMessage(), HttpStatus.CONFLICT));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<RestResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("IllegalStateException : {}", ex.getMessage());
        // 422 Unprocessable Entity — on évite HttpStatus.UNPROCESSABLE_ENTITY (déprécié Spring 7)
        return ResponseEntity
            .status(422)
            .body(RestResponse.error(ex.getMessage(), HttpStatus.valueOf(422)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
            errors.put(err.getField(), err.getDefaultMessage())
        );
        log.debug("Validation échouée : {}", errors);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.badRequest("Erreur de validation", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Void>> handleException(Exception ex) {
        log.error("Erreur interne non gérée : {}", ex.getMessage(), ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(RestResponse.error("Une erreur interne est survenue", HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
