package net.berndreiss.zentodo.data;

import com.sun.istack.NotNull;
import net.berndreiss.zentodo.exceptions.DuplicateUserIdException;
import net.berndreiss.zentodo.exceptions.InvalidUserActionException;
import net.berndreiss.zentodo.util.VectorClock;
import net.berndreiss.zentodo.util.ZenServerMessage;

import java.util.List;
import java.util.Optional;

/**
 * Methods for persisting users.
 */
public interface UserManagerI {
    /**
     * Add a new user.
     *   - The id is globally unique and needs to be retrieved from the server.
     * @param user the user id
     * @param email the email for the user
     * @param userName the username
     * @param device the active device
     * @return a new user
     * @throws DuplicateUserIdException thrown when the user id already exists
     * @throws InvalidUserActionException thrown when a user with the same email address already exists
     */
    User addUser(long user, @NotNull String email, String userName, Integer device) throws DuplicateUserIdException, InvalidUserActionException;

    /**
     * Add a profile for a user.
     * @param user the user id
     * @param name the name for the profile
     * @return a new profile
     * @throws InvalidUserActionException thrown if the user does not exist
     */
    Profile addProfile(long user, String name) throws InvalidUserActionException;

    /**
     * Add a profile for a user.
     * @param user the user id
     * @return a new profile
     * @throws InvalidUserActionException thrown if the user does not exist
     */
    Profile addProfile(long user) throws InvalidUserActionException;

    /**
     * Remove a user.
     * @param user the user id
     * @throws InvalidUserActionException thrown when trying to remove the default user (id==0)
     */
    void removeUser(long user) throws InvalidUserActionException;

    /**
     * Remove a profile from the user.
     * @param user the user id
     * @param profile the profile id
     * @throws InvalidUserActionException thrown when the profile is the last one of the user
     */
    void removeProfile(long user, int profile) throws InvalidUserActionException;

    /**
     * Get a user by their email address.
     * @param email the email address
     * @return the user associated to the email address, if they exist
     */
    Optional<User> getUserByEmail(String email);

    /**
     * Get a user by their id.
     * @param user the user id
     * @return the user, if they exist
     */
    Optional<User> getUser(long user);

    /**
     * Get a profile for the user.
     * @param user the user id
     * @param profile the profile id
     * @return the profile, if it exists
     */
    Optional<Profile> getProfile(long user, long profile);

    /**
     * Set the enabled field for the user to true.
     * @param user the user id
     */
    void enableUser(long user);

    /**
     * Set the active device for the user.
     * @param user the user id
     * @param device the device id
     */
    void setDevice(long user, int device);

    /**
     * Set the vector clock for a user.
     * @param user the user id
     * @param clock the clock to set
     */
    void setClock(long user, VectorClock clock);

    /**
     * Update the email address for the user.
     * @param user the user id
     * @param email the new email address
     * @throws InvalidUserActionException thrown if email address already exists
     */
    void updateEmail(Long user, String email) throws  InvalidUserActionException;

    /**
     * Update the username.
     * @param user the user id
     * @param name the new name
     */
    void updateUserName(Long user, String name);

    /**
     * Add a message to the users queue.
     * @param user the user id
     * @param message the messag to add
     */
    void addToQueue(User user, ZenServerMessage message);

    /**
     * Get all queued message for the user.
     * @param user the user id
     * @return a list of queued messages
     */
    List<ZenServerMessage> getQueued(long user);

    /**
     * Clear the queue for the user.
     * @param user the user id
     */
    void clearQueue(long user);

    /**
     * Get the user token.
     * @param user the user id
     * @return the JWT token, if it exists
     */
    Optional<String> getToken(long user);

    /**
     * Set the token for the user.
     * @param user the user id
     * @param token the JWT token to set
     */
    void setToken(long user, String token);

    /**
     * Get all users.
     * @return a list of existing users
     */
    List<User> getUsers();

    /**
     * Get all profiles for a user.
     * @param user the user id
     * @return a list of user profiles
     */
    List<Profile> getProfiles(long user);
}
