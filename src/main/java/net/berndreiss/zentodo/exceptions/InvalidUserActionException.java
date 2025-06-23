package net.berndreiss.zentodo.exceptions;

/**
 * Thrown when an invalid user action is performed:
 *   - adding a new user with a mail address that was already registered
 *   - removing the default user
 *   - removing the default profile
 *   - removing the last profile of a user
 *   - adding a profile to a non-existing user
 */
public class InvalidUserActionException extends InvalidActionException {
    public InvalidUserActionException(String message) {
        super(message);
    }
}
