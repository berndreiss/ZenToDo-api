package net.berndreiss.zentodo.data;

import net.berndreiss.zentodo.exceptions.DuplicateIdException;
import net.berndreiss.zentodo.exceptions.InvalidActionException;
import net.berndreiss.zentodo.exceptions.PositionOutOfBoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Methods for persisting tasks.
 * THERE MAY BE NECESSARY SIDE EFFECTS OF OPERATIONS. IF SO THEY ARE DESCRIBED IN THE COMMENTS.
 */
public interface TaskManagerI {

   /**
    * Update the id of a task.
    * @param userId the user id
    * @param profile the profile id
    * @param task the task id
    * @param newId the new id
    * @throws DuplicateIdException thrown if new id already exists
    */
   void updateId(long userId, int profile, long task, long newId) throws DuplicateIdException;

   /**
    * Add a new task with all its attributes. This is used when retrieving the whole database from the server.
    * DO NOT USE THIS ON AN EXISTING DATABASE! THIS WILL LEAD TO ERRORS OR, WORSE, INCONSISTENCIES!
    * @param task the task to be added.
    */
   void postTask(Task task);

   /**
    * Add a new task. A unique id has to be assigned.
    *   - needs to assign a unique id to the task returned
    *   - needs to add the task to the end of the list of all tasks and assign the position to the returned task
    * @param userId the user id
    * @param profile the profile id
    * @param task the task to be added
    * @return a new Task with a unique id
    */
   Task addNewTask(long userId, int profile, String task);

   /**
    * Add a new task to the database at a certain position.
    *   - All tasks with positions greater than the position of the task need to be incremented.
    *     //TODO add to tests?
    *   - needs to assign a unique id to the task returned
    * @param user the user id
    * @param profile the profile id
    * @param task the task to be added
    * @param position the position of the task
    * throws PositionOutOfBoundException the position exceeds the number of tasks already present
    */
   Task addNewTask(long user, int profile, String task, int position) throws PositionOutOfBoundException;

   /**
    * Add a new task to the database to a certain position with a custom id.
    *   - All tasks with positions greater than the position of the task need to be incremented.
    *     //TODO add to tests?
    * @param user the user id
    * @param profile the profile id
    * @param id   the id of the new task as provided by the server
    * @param task the task to be associated with the entry
    * @param position the position where the task is to be added
    * throws PositionOutOfBoundException the position exceeds the number of tasks already present
    * throws DuplicateIdException thrown when the id already exists
    * throws InvalidActionException thrown when the id==0
    */
   Task addNewTask(long user, int profile, long id, String task, int position) throws PositionOutOfBoundException, DuplicateIdException, InvalidActionException;

   /**
    * Remove a task from the database.
    *   - Also remove the task from the queue
    *   //TODO add to tests?
    *   - All tasks with a position greater than the deleted task need to be decremented.
    * @param user the user id
    * @param profile the profile id
    * @param id the id of the entry to be deleted
    */
   void removeTask(long user, int profile, long id);

   /**
    * Swap two tasks. We use the position for the other task here, so that the action is commutative.
    * @param user the user id
    * @param profile the profile id
    * @param id the id of the task to be moved
    * @param position the position with which to swap
    * throws PositionOutOfBoundException thrown when the position does not exist
    */
   void swapTasks(long user, int profile, long id, int position) throws PositionOutOfBoundException;


   /**
    * Update the task with the value provided.
    * @param user the user id
    * @param profile the profile id
    * @param id  the id of the task to be updated
    * @param value the value to update with
    */
   void updateTask(long user, int profile, long id, String value);

   /**
    * Update the focus field with the value provided.
    * @param user the user id
    * @param profile the profile id
    * @param id    the id of the task to be updated
    * @param value the value to update with
    */
   void updateFocus(long user, int profile, long id, boolean value);

   /**
    * Update the dropped field with the value provided.
    * @param user the user id
    * @param profile the profile id
    * @param id    the id of the task to be updated
    * @param value the value to update with
    */
   void updateDropped(long user, int profile, long id, boolean value);


   /**
    * Update the reminder date with the value provided.
    * @param user the user id
    * @param profile the profile id
    * @param id    the id of the task to be updated
    * @param value the value to update with
    */
   void updateReminderDate(long user, int profile, long id, Instant value);


   /**
    * Update the recurrence field with the value provided. Also needs to update the reminder date.
    * @param user the user id
    * @param profile the profile id
    * @param id           the id of the task to be updated
    * @param value        the value to update with
    */
   void updateRecurrence(long user, int profile, long id, String value);

   /**
    * Get a task.
    * @param user the user id
    * @param profile the profile id
    * @param id the task id
    * @return the task
    */
   Optional<Task> getTask(long user, int profile, long id);

   /**
    * Get all tasks for the user profile.
    * @param user the user id
    * @param profile the profile id
    * @return a list of tasks
    */
   List<Task> getTasks(long user, int profile);

   /**
    * Get all tasks that are in FOCUS mode.
    * @param user the user id
    * @param profile the profile id
    * @return a list of tasks in FOCUS
    */
   List<Task> loadFocus(long user, int profile);

   /**
    * Get all tasks that have been dropped.
    * @param user the user id
    * @param profile the profile id
    * @return a list of dropped tasks
    */
   List<Task> loadDropped(long user, int profile);
}
