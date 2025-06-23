package net.berndreiss.zentodo.exceptions;

/**
 * Exception being thrown when an id already exists.
 */
public class DuplicateIdException extends Exception{
    public DuplicateIdException(String message){
        super(message);
    }
}
