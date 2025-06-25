package net.berndreiss.zentodo.data;

/**
 * Methods for persisting user independent data.
 */
public interface MetadataManagerI {
    /**
     * Keeps track of the time delay between the server and client.
     *
     * @param delay the current delay
     */
    void setTimeDelay(long delay);

    /**
     * Get the time delay between the server and client.
     * @return the time delay
     */
    long getTimeDelay();

    /**
     * Set the last user active.
     * @param user the id of the user
     */
    void setLastUser(long user);

    /**
     * Get the last user logged in on the device.
     * @return the id of the last user
     */
    long getLastUser();

    /**
     * Set DB version.
     * @param version the version
     */
    void setVersion(String version);

    /**
     * Get the DB version.
     * @return the version
     */
    String getVersion();
}
