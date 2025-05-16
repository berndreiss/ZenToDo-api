package net.berndreiss.zentodo.data;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EntryManagerI {

   /**
    * TODO DESCRIBE
    *
    * @param entry
    * @param id
    */
   void updateId(long userId, long profile, long entry, long id) throws DuplicateIdException;

   /**
    * TODO DESCRIBE
    * @param delay
    */
   ;


   Entry addNewEntry(long userId, long profile, String task);

   /**
    * Add a single new task to the database.
    *
    * @param task the task to be associated with the entry
    */
   Entry addNewEntry(long userId, long profile, String task, int position) throws PositionOutOfBoundException;

   /**
    * Add a single new task to the database.
    *
    * @param id   the id of the new task as provided by the server
    * @param task the task to be associated with the entry
    */
   Entry addNewEntry(long userId, long profile, long id, String task, int position) throws DuplicateIdException, PositionOutOfBoundException;

   /**
    * Delete entry from database including the local queue(s). All entries with position greater than the deleted
    * entries position are decremented.
    *
    * @param id the id of the entry to be deleted
    */
   void removeEntry(long userId, long profile, long id);

   /**
    * Swap entry with id with the entry at position.
    *
    * @param id       the id of the entry to be moved
    * @param position the position with which to swap
    */
   void swapEntries(long userId, long profile, long id, int position) throws PositionOutOfBoundException;


   /**
    * Update the task with the value provided.
    *
    * @param id    the id of the task to be updated
    * @param value the value to update with
    */
   void updateTask(long userId, long profile, long id, String value);

   /**
    * Update the field with the value provided.
    *
    * @param id    the id of the task to be updated
    * @param value the value to update with
    */
   void updateFocus(long userId, long profile, long id, boolean value);

   /**
    * Update the field with the value provided.
    *
    * @param id    the id of the task to be updated
    * @param value the value to update with
    */
   void updateDropped(long userId, long profile, long id, boolean value);


   /**
    * Update the field with the value provided.
    *
    * @param id    the id of the task to be updated
    * @param value the value to update with
    */
   void updateReminderDate(long userId, long profile, long id, Instant value);


   /**
    * Update the field with the value provided. Also needs to update the reminder date.
    *
    * @param id           the id of the task to be updated
    * @param value        the value to update with
    */
   void updateRecurrence(long userId, long profile, long id, String value);



   Optional<Entry> getEntry(long userId, long profile, long id);

   List<Entry> getEntries(long userId, long profile);
}
