package net.berndreiss.zentodo.operations;

import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.User;
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
     * @return
     */
    String getToken(long userId);

    /**
     * TODO DESCRIBE
     * @param token
     */
    void setToken(long userId, String token);


    /**
     * TODO DESCRIBE
     * @param email
     * @param userName
     */
    User addUser(long userId, String email, String userName, long device);

    /**
     * TODO
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
     * @return
     */
    boolean userExists(long userId);

    /**
     * TODO DESCRIBE
     * @return
     */
    boolean isEnabled(long userId);

    /**
     * TODO DESCRIBE
     */
    void enableUser(long userId);

    /**
     * TODO
     * @param id
     */
    void setDevice(long userId, long id);

    /**
     * TODO
     * @param clock
     */
    void setClock(long userId,VectorClock clock);

}
