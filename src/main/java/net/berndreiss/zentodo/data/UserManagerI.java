package net.berndreiss.zentodo.data;

import net.berndreiss.zentodo.exceptions.DuplicateUserIdException;
import net.berndreiss.zentodo.exceptions.InvalidUserActionException;
import net.berndreiss.zentodo.util.VectorClock;
import net.berndreiss.zentodo.util.ZenServerMessage;

import java.util.List;
import java.util.Optional;

public interface UserManagerI {
    /**
     * TODO DESCRIBE
     * @param id
     * @param email
     * @param userName
     */
    User addUser(long userId, String email, String userName, Integer device) throws DuplicateUserIdException, InvalidUserActionException;
    Profile addProfile(long userId, String name) throws InvalidUserActionException;
    Profile addProfile(long userId) throws InvalidUserActionException;

    /**
     * TODO
     */
    void removeUser(long userId) throws InvalidUserActionException;
    void removeProfile(long userId, int profileId) throws InvalidUserActionException;

    /**
     *
     * @param email
     * @return
     */
    Optional<User> getUserByEmail(String email);
    Optional<User> getUser(long id);
    Optional<Profile> getProfile(long userId, long id);

    /**
     * TODO DESCRIBE
     */
    void enableUser(long userId);

    /**
     * TODO
     * @param email
     * @param id
     */
    void setDevice(long userId, int id);

    /**
     * TODO
     * @param email
     * @param clock
     */
    void setClock(long userId, VectorClock clock);

    /**
     * TODO
     * @param id
     * @param email
     */
    void updateEmail(Long userId, String email) throws  InvalidUserActionException;

    /**
     * TODO
     * @param id
     * @param name
     */
    void updateUserName(Long userId, String name);

    /**
     * TODO DESCRIBE
     * @param message
     */
    void addToQueue(User user, ZenServerMessage message);

    /**
     * TODO DESCRIBE
     * @return
     */
    List<ZenServerMessage> getQueued(long userId);


    void clearQueue(long userId);

    /**
     * TODO DESCRIBE
     * @param user
     * @return
     */
    String getToken(long userId);

    /**
     * TODO DESCRIBE
     * @param user
     * @param token
     */
    void setToken(long userId, String token);


    List<User> getUsers();
    List<Profile> getProfiles(long userId);

}
