package net.berndreiss.zentodo.data;

import net.berndreiss.zentodo.OperationType;
import net.berndreiss.zentodo.util.Message;

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
    void updateId(int entry, int id);

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
    List<Message> geQueued();

}
