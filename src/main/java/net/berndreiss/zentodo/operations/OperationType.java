package net.berndreiss.zentodo.operations;

/**
 * Possible operations when interacting with the server. The arguments that have to be passed are in the comments
 * including the adequate type in parentheses. Null values are to be represented by empty Strings.
 * THE POST METHODS ARE NOT IMPLEMENTED ON THE SERVER SIDE!
 */

public enum OperationType {
    /**
     * PROFILE (int), ID (long), TASK (String), POSITION (int), FOCUS (boolean), DROPPED (boolean),
     * LIST (Long), LIST_POSITION (Long), REMINDER_DATE (Instant), RECURRENCE (String)
     */
    POST,
    /**
     * ID (long), NAME (String), COLOR (String), PROFILES (int[])
     */
    POST_LIST,
    /**
     * ID (long), NAME (String)
     */
    POST_PROFILE,
    /**
     * PROFILE (int), ID (long), TASK (String), POSITION (int)
     */
    ADD_NEW_TASK,
    /**
     * PROFILE (int), ID (long)
     */
    DELETE,
    /**
     * PROFILE (int), ID (long), POSITION (int)
     */
    SWAP,
    /**
     * PROFILE (int), ID (long), LIST (long), LIST_POSITION (int)
     */
    SWAP_LIST,
    /**
     * PROFILE (int), ID (long), TASK (String)
     */
    UPDATE_TASK,
    /**
     * PROFILE (int), ID (long), FOCUS (boolean)
     */
    UPDATE_FOCUS,
    /**
     * PROFILE (int), ID (long), DROPPED (boolean)
     */
    UPDATE_DROPPED,
    /**
     * PROFILE (int), ID (long), LIST (Long)
     */
    UPDATE_LIST,
    /**
     * PROFILE (int), ID (long), REMINDER_DATE (Instant)
     */
    UPDATE_REMINDER_DATE,
    /**
     * PROFILE (int), ID (long), RECURRENCE (String)
     */
    UPDATE_RECURRENCE,
    /**
     * LIST (long), COLOR (String)
     */
    UPDATE_LIST_COLOR,
    /**
     * ID (long), NAME (String)
     */
    UPDATE_USER_NAME,
    /**
     * ID (long), MAIL (String)
     */
    UPDATE_MAIL,
    /**
     * OLD_ID (long), NEW_ID (long)
     */
    UPDATE_ID,
}