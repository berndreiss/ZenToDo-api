package net.berndreiss.zentodo.exceptions;

/**
 * Thrown when an invalid action if performed:
 * - adding a task with id==0
 * - adding a list without a name
 * - adding a duplicate list name for a user
 * - trying to add a task to a list that is not assigned to the user profile
 */
public class InvalidActionException extends Exception {
    public InvalidActionException(String message) {
        super(message);
    }
}
