package net.berndreiss.zentodo.exceptions;

/**
 * Thrown when a list position is out of bound:
 *   - when assigning task positions
 *   - when assigning list positions
 */
public class PositionOutOfBoundException extends Exception {
    public PositionOutOfBoundException(String message) {
        super(message);
    }
}
