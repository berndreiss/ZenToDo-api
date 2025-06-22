package net.berndreiss.zentodo.util;

/**
 * The status of the initialized user.
 */
public enum Status {
    /** Registered but not yet enabled. */
    REGISTERED,
    /** Online synchronizing with the server. */
    ONLINE,
    /** Currently not able to communicate with the server. */
    OFFLINE,
    /** OFFLINE and there is data in the queue, that has not been sent yet. */
    DIRTY,
    /** User has been deleted from another device. */
    DELETED
}
