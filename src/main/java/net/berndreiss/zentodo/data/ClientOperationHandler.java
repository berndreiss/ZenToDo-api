package net.berndreiss.zentodo.data;

import net.berndreiss.zentodo.OperationType;
import net.berndreiss.zentodo.util.ZenMessage;

import java.io.IOException;
import java.util.List;

/**
 * TODO IMPLEMENT DESCRIPTION
 */
public interface ClientOperationHandler extends OperationHandler {

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
     * @param type
     * @param arguments
     */
    void addToQueue(OperationType type, List<Object> arguments);

    /**
     * TODO DESCRIBE
     * @return
     */
    List<ZenMessage> geQueued();

    /**
     * TODO DESCRIBE
     * @param user
     * @return
     */
    String getToken(long user) throws IOException;

    /**
     * TODO DESCRIBE
     * @param user
     * @param token
     */
    void setToken(long user, String token) throws IOException;


    /**
     * TODO DESCRIBE
     * @param id
     * @param email
     * @param userName
     */
    void addUser(long id, String email, String userName);

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

}
