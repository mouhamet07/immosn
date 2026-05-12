package sn.immosn.backend.shared.exception;

public class EntityExistException extends RuntimeException{
    public EntityExistException(String message){
        super(message);
    }
}
