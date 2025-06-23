package net.berndreiss.zentodo.exceptions;

/**
 * Thrown when an invalid action if performed:
 *   - adding a task with id==0
 *   - adding a list without a name
 *   - adding a duplicate list name for a user
 */
public class InvalidActionException extends Exception {
    public InvalidActionException(String message) {
        super(message);
    }
}
