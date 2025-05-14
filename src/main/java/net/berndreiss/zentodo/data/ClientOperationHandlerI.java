package net.berndreiss.zentodo.data;

import net.berndreiss.zentodo.util.VectorClock;
import net.berndreiss.zentodo.util.ZenServerMessage;

import java.util.List;

/**
 * TODO IMPLEMENT DESCRIPTION
 */
public interface ClientOperationHandlerI extends OperationHandlerI {

    /**
     * Add a list of entries to the database including all the fields in the entry.
     *
     * @param entries the entries to be added
     */
    void post(List<Entry> entries);


    /**
     * TODO DESCRIBE
     * @param entry
     * @param id
     */
    void updateId(long userId, long profile, long entry, long id);

    /**
     * TODO DESCRIBE
     * @param delay
     */
    void setTimeDelay(long delay);

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


    /**
     * TODO DESCRIBE
     * @param id
     * @param email
     * @param userName
     */
    User addUser(long userId, String email, String userName, long device);

    /**
     * TODO
     * @param email
     */
    void removeUser(long userId);

    /**
     *
     * @param email
     * @return
     */
    User getUserByEmail(String email);

    /**
     * TODO DESCRIBE
     * @param email
     * @return
     */
    boolean userExists(long userId);

    /**
     * TODO DESCRIBE
     * @param email
     * @return
     */
    boolean isEnabled(long userId);

    /**
     * TODO DESCRIBE
     * @param email
     */
    void enableUser(long userId);

    /**
     * TODO
     * @param email
     * @param id
     */
    void setDevice(long userId, long id);

    /**
     * TODO
     * @param email
     * @param clock
     */
    void setClock(long userId,VectorClock clock);

}
