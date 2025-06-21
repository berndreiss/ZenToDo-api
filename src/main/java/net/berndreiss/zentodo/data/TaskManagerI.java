package net.berndreiss.zentodo.data;

import net.berndreiss.zentodo.exceptions.DuplicateIdException;
import net.berndreiss.zentodo.exceptions.InvalidActionException;
import net.berndreiss.zentodo.exceptions.PositionOutOfBoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskManagerI {

   /**
    * TODO DESCRIBE
    *
    * @param entry
    * @param id
    */
   void updateId(long userId, int profile, long entry, long id) throws DuplicateIdException;

   /**
    * TODO DESCRIBE
    * @param delay
    */
   ;

   void postTask(Task task);

   Task addNewTask(long userId, int profile, String task);

   /**
    * Add a single new task to the database.
    *
    * @param task the task to be associated with the entry
    */
   Task addNewTask(long userId, int profile, String task, int position) throws PositionOutOfBoundException;

   /**
    * Add a single new task to the database.
    *
    * @param id   the id of the new task as provided by the server
    * @param task the task to be associated with the entry
    */
   Task addNewTask(long userId, int profile, long id, String task, int position) throws DuplicateIdException, PositionOutOfBoundException, InvalidActionException;

   /**
    * Delete entry from database including the local queue(s). All entries with position greater than the deleted
    * entries position are decremented.
    *
    * @param id the id of the entry to be deleted
    */
   void removeTask(long userId, int profile, long id);

   /**
    * Swap entry with id with the entry at position.
    *
    * @param id       the id of the entry to be moved
    * @param position the position with which to swap
    */
   void swapTasks(long userId, int profile, long id, int position) throws PositionOutOfBoundException;


   /**
    * Update the task with the value provided.
    *
    * @param id    the id of the task to be updated
    * @param value the value to update with
    */
   void updateTask(long userId, int profile, long id, String value);

   /**
    * Update the field with the value provided.
    *
    * @param id    the id of the task to be updated
    * @param value the value to update with
    */
   void updateFocus(long userId, int profile, long id, boolean value);

   /**
    * Update the field with the value provided.
    *
    * @param id    the id of the task to be updated
    * @param value the value to update with
    */
   void updateDropped(long userId, int profile, long id, boolean value);


   /**
    * Update the field with the value provided.
    *
    * @param id    the id of the task to be updated
    * @param value the value to update with
    */
   void updateReminderDate(long userId, int profile, long id, Instant value);


   /**
    * Update the field with the value provided. Also needs to update the reminder date.
    *
    * @param id           the id of the task to be updated
    * @param value        the value to update with
    */
   void updateRecurrence(long userId, int profile, long id, String value);



   Optional<Task> getTask(long userId, int profile, long id);

   List<Task> getTasks(long userId, int profile);

   List<Task> loadFocus(long userId, int profile);
   List<Task> loadDropped(long userId, int profile);
}
