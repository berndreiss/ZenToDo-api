package net.berndreiss.zentodo.exceptions;

/**
 * Exception being thrown when a user id already exists.
 */
public class DuplicateUserIdException extends DuplicateIdException{
    public DuplicateUserIdException(String message){
        super(message);
    }
}
