package net.berndreiss.zentodo.data;

/**
 * Methods for persisting user independent data.
 */
public interface DatabaseOpsI {
    /**
     * Keeps track of the time delay between the server and client.
     *
     * @param delay the current delay
     */
    void setTimeDelay(long delay);
}
