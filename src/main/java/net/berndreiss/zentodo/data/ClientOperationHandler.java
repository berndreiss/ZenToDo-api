package net.berndreiss.zentodo.data;

import net.berndreiss.zentodo.OperationType;
import net.berndreiss.zentodo.util.VectorClock;
import net.berndreiss.zentodo.util.ZenMessage;
import net.berndreiss.zentodo.util.ZenServerMessage;

import java.io.IOException;
import java.util.List;

/**
 * TODO IMPLEMENT DESCRIPTION
 */
public interface ClientOperationHandler extends OperationHandler {

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
    void updateId(long entry, long id);

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


    void clearQueue();

    /**
     * TODO DESCRIBE
     * @param user
     * @return
     */
    String getToken(long user);

    /**
     * TODO DESCRIBE
     * @param user
     * @param token
     */
    void setToken(long user, String token);


    /**
     * TODO DESCRIBE
     * @param id
     * @param email
     * @param userName
     */
    User addUser(long id, String email, String userName, long device);

    /**
     * TODO
     * @param email
     */
    void removeUser(String email);

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
    boolean userExists(String email);

    /**
     * TODO DESCRIBE
     * @param email
     * @return
     */
    boolean isEnabled(String email);

    /**
     * TODO DESCRIBE
     * @param email
     */
    void enableUser(String email);

    /**
     * TODO
     * @param email
     * @param id
     */
    void setDevice(String email, long id);

    /**
     * TODO
     * @param email
     * @param clock
     */
    void setClock(String email,VectorClock clock);

}
