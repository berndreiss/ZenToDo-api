package net.berndreiss.zentodo.operations;

import net.berndreiss.zentodo.data.Task;
import net.berndreiss.zentodo.data.User;
import net.berndreiss.zentodo.util.VectorClock;
import net.berndreiss.zentodo.util.ZenServerMessage;

import java.util.List;
import java.util.Optional;

/**
 * Additional methods needed for the server to interact with the client.
 */
public interface ClientOperationHandlerI extends OperationHandlerI {

    /**
     * Add a list of tasks to the database including all the fields in the task.
     *
     * @param tasks the tasks to be added
     */
    void post(List<Task> tasks);


    /**
     * Update the id of a task.
     *
     * @param user    the user id
     * @param profile the profile id
     * @param task    the task id to update
     * @param newId   the newId
     */
    void updateId(long user, long profile, long task, long newId);

    /**
     * Set the time delay
     *
     * @param delay the delay to set
     */
    void setTimeDelay(long delay);

    /**
     * Add a message to the queue
     *
     * @param user    the user active
     * @param message the message to add
     */
    void addToQueue(User user, ZenServerMessage message);

    /**
     * Get all queued messages for the user.
     *
     * @param user the user id
     * @return a list of queued messages
     */
    List<ZenServerMessage> getQueued(long user);

    /**
     * Clear the queue for the user.
     *
     * @param user the user id
     */
    void clearQueue(long user);

    /**
     * Get the token for the user.
     *
     * @param user the user id
     * @return a JWT token if it exists
     */
    Optional<String> getToken(long user);

    /**
     * Set the token for the user.
     *
     * @param user  the user id
     * @param token the JWT token to set
     */
    void setToken(long user, String token);

    /**
     * Add a new user.
     *
     * @param user     the user id
     * @param email    the users email
     * @param userName the username
     * @param device   the device
     * @return a new User
     */
    User addUser(long user, String email, String userName, long device);

    /**
     * Remove an existing user.
     *
     * @param user the user id
     */
    void removeUser(long user);

    /**
     * Get the user by email.
     *
     * @param email the email of the user
     * @return the user if it exists
     */
    Optional<User> getUserByEmail(String email);

    /**
     * Check whether a user exists.
     *
     * @param user the user id
     * @return true if the user exists, false otherwise
     */
    boolean userExists(long user);

    /**
     * Check whether user is enabled.
     *
     * @param user the user id
     * @return true if the user has been enabled, false otherwise
     */
    boolean isEnabled(long user);

    /**
     * Set the enabled field of the user to true.
     *
     * @param user the user id
     */
    void enableUser(long user);

    /**
     * Set the device for the user.
     *
     * @param user the user id
     * @param id   the device id to set
     */
    void setDevice(long user, long id);

    /**
     * Set the clock for the user.
     *
     * @param user  the user id
     * @param clock the clock to set
     */
    void setClock(long user, VectorClock clock);

}
