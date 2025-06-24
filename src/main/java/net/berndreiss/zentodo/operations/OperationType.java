package net.berndreiss.zentodo.operations;

/**
 * Possible operations when interacting with the server. The arguments that have to be passed are in the comments
 * including the adequate type in parentheses. Null values are to be represented by empty Strings.
 * THE POST METHODS ARE NOT IMPLEMENTED ON THE SERVER SIDE!
 */

public enum OperationType {
    /**
     * PROFILE (int), TASK (long), TASK_NAME (String), POSITION (int), FOCUS (boolean), DROPPED (boolean),
     * LIST (Long), LIST_POSITION (Long), REMINDER_DATE (Instant), RECURRENCE (String)
     */
    POST,
    /**
     * TASK (long), NAME (String), COLOR (String), PROFILES (int[])
     */
    POST_LIST,
    /**
     * TASK (long), NAME (String)
     */
    POST_PROFILE,
    /**
     * PROFILE (int), TASK (long), TASK (String), POSITION (int)
     */
    ADD_NEW_TASK,
    /**
     * TASK (long), NAME (@NotNull String), Color (String -> HEX)
     */
    ADD_NEW_LIST,
    /**
     * PROFILE (int), TASK (long)
     */
    DELETE,
    /**
     * PROFILE (int), TASK (long), POSITION (int)
     */
    SWAP,
    /**
     * PROFILE (int), TASK (long), LIST (long), LIST_POSITION (int)
     */
    SWAP_LIST,
    /**
     * PROFILE (int), TASK (long), TASK_NAME (String)
     */
    UPDATE_TASK,
    /**
     * PROFILE (int), TASK (long), FOCUS (boolean)
     */
    UPDATE_FOCUS,
    /**
     * PROFILE (int), TASK (long), DROPPED (boolean)
     */
    UPDATE_DROPPED,
    /**
     * PROFILE (int), TASK (long), LIST (Long)
     */
    UPDATE_LIST,
    /**
     * PROFILE (int), TASK (long), REMINDER_DATE (Instant)
     */
    UPDATE_REMINDER_DATE,
    /**
     * PROFILE (int), TASK (long), RECURRENCE (String)
     */
    UPDATE_RECURRENCE,
    /**
     * LIST (long), COLOR (String)
     */
    UPDATE_LIST_COLOR,
    /**
     * LIST (long), NAME (String)
     */
    UPDATE_USER_NAME,
    /**
     * USER (long), MAIL (String)
     */
    UPDATE_MAIL,
    /**
     * OLD_ID (long), NEW_ID (long)
     */
    UPDATE_ID,
}