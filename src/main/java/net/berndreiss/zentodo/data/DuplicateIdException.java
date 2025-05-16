package net.berndreiss.zentodo.data;

public class DuplicateIdException extends Exception{
    public DuplicateIdException(String message){
        super(message);
    }
}
