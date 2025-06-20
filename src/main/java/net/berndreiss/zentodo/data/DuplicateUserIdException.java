package net.berndreiss.zentodo.data;

public class DuplicateUserIdException extends DuplicateIdException{
    public DuplicateUserIdException(String message){
        super(message);
    }
}
