package net.berndreiss.zentodo.exceptions;

public class DuplicateIdException extends Exception{
    public DuplicateIdException(String message){
        super(message);
    }
}
