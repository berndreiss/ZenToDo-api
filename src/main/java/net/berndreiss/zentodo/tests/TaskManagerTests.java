package net.berndreiss.zentodo.tests;

import net.berndreiss.zentodo.data.Database;
import net.berndreiss.zentodo.data.Task;
import net.berndreiss.zentodo.data.TaskManagerI;
import net.berndreiss.zentodo.data.User;
import net.berndreiss.zentodo.exceptions.DuplicateIdException;
import net.berndreiss.zentodo.exceptions.InvalidActionException;
import net.berndreiss.zentodo.exceptions.PositionOutOfBoundException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test methods of the TaskManagerI interface.
 */
public class TaskManagerTests {
    public static long getUniqueId(User user, Database database) {
        TaskManagerI taskManager = database.getTaskManager();
        List<Task> entries = taskManager.getTasks(user.getId(), user.getProfile());
        Set<Long> existingIds = entries.stream().map(Task::getId).collect(Collectors.toSet());
        long id = 42;

        while (existingIds.contains(id))
            id++;

        return id;
    }

    @Before
    public void prepare() {
        DatabaseTestSuite.prepare();
    }

    @After
    public void cleanUp() {
        DatabaseTestSuite.cleanup();
    }

    @Test
    public void addNewEntry() throws PositionOutOfBoundException, InvalidActionException, DuplicateIdException {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        TaskManagerI taskManager = database.getTaskManager();
        Task task0 = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");
        Task task1 = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK1");

        Optional<Task> entry0Returned = taskManager.getTask(user.getId(), DatabaseTestSuite.user.getProfile(), task0.getId());

        Assert.assertTrue("entry0 should be present", entry0Returned.isPresent());
        Assert.assertEquals("Returned entry has wrong id.", task0.getId(), entry0Returned.get().getId());
        Assert.assertEquals("Returned entry has wrong user id.", user.getId(), entry0Returned.get().getUserId());
        Assert.assertEquals("Returned entry has wrong profile.", user.getProfile(), entry0Returned.get().getProfile());
        Assert.assertEquals("Position for first added entry is not 0.", 0, entry0Returned.get().getPosition());
        Assert.assertEquals("Wrong task saved for new entry.", "TASK0", entry0Returned.get().getTask());
        Assert.assertEquals("Focus needs to be 0 for new entries.", 0, entry0Returned.get().getFocus() ? 1 : 0);
        Assert.assertEquals("Dropped needs to be 1 for new entries.", 1, entry0Returned.get().getDropped() ? 1 : 0);
        Assert.assertNull("List needs to be initialized with null.", entry0Returned.get().getList());
        Assert.assertNull("List position needs to be initialized with null.", entry0Returned.get().getListPosition());
        Assert.assertNull("Recurrence needs to be initialized with null.", entry0Returned.get().getRecurrence());
        Assert.assertNull("Reminder date needs to be initialized with null.", entry0Returned.get().getReminderDate());

        Optional<Task> entry1Returned = taskManager.getTask(user.getId(), DatabaseTestSuite.user.getProfile(), task1.getId());
        Assert.assertTrue("entry1 should be present", entry1Returned.isPresent());
        Assert.assertEquals(
                task1.getPosition() == 0 ? "Positions for added entries is not incremented." : "Position for added entry is arbitrary.",
                1, entry1Returned.get().getPosition()
        );

        Task task2 = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2", 1);
        Optional<Task> entry2Returned = taskManager.getTask(user.getId(), DatabaseTestSuite.user.getProfile(), task2.getId());
        Assert.assertTrue("entry2 should be present", entry2Returned.isPresent());
        Assert.assertEquals("Returned entry has wrong id for custom position.", task2.getId(), entry2Returned.get().getId());
        Assert.assertEquals("Returned entry has wrong user id for custom position.", user.getId(), entry2Returned.get().getUserId());
        Assert.assertEquals("Returned entry has wrong profile for custom position.", user.getProfile(), entry2Returned.get().getProfile());
        Assert.assertEquals("Wrong task saved for new entry for custom position.", "TASK2", entry2Returned.get().getTask());
        Assert.assertEquals("Focus needs to be 0 for new entries for custom position.", 0, entry2Returned.get().getFocus() ? 1 : 0);
        Assert.assertEquals("Dropped needs to be 1 for new entries for custom position.", 1, entry2Returned.get().getDropped() ? 1 : 0);
        Assert.assertNull("List needs to be initialized with null for custom position.", entry2Returned.get().getList());
        Assert.assertNull("List position needs to be initialized with null for custom position.", entry2Returned.get().getListPosition());
        Assert.assertNull("Recurrence needs to be initialized with null for custom position.", entry2Returned.get().getRecurrence());
        Assert.assertNull("Reminder date needs to be initialized with null for custom position.", entry2Returned.get().getReminderDate());
        Assert.assertEquals("New entry was not assigned custom position.", 1, entry2Returned.get().getPosition());

        entry0Returned = taskManager.getTask(user.getId(), DatabaseTestSuite.user.getProfile(), task0.getId());
        Assert.assertTrue("entry0 should still be present", entry0Returned.isPresent());
        Assert.assertEquals("Adding entry with custom position must not modify positions of entries before it.", 0, entry0Returned.get().getPosition());

        entry1Returned = taskManager.getTask(user.getId(), DatabaseTestSuite.user.getProfile(), task1.getId());
        Assert.assertTrue("entry1 should still be present", entry1Returned.isPresent());
        Assert.assertEquals("Adding entry with custom position needs to adjust the position of entries after it.", 2, entry1Returned.get().getPosition());

        long id = getUniqueId(user, database);
        Task task3 = null;
        try {
            task3 = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), id, "TASK3", 3);
        } catch (DuplicateIdException _) {
        }
        Assert.assertNotNull(task3);
        Optional<Task> entry3Returned = taskManager.getTask(user.getId(), DatabaseTestSuite.user.getProfile(), task3.getId());
        Assert.assertTrue("entry3 should be present", entry3Returned.isPresent());
        Assert.assertEquals("Returned entry has wrong id for custom id.", id, entry3Returned.get().getId());
        Assert.assertEquals("Returned entry has wrong user id for custom id.", user.getId(), entry3Returned.get().getUserId());
        Assert.assertEquals("Returned entry has wrong profile for custom id.", user.getProfile(), entry3Returned.get().getProfile());
        Assert.assertEquals("Wrong task saved for new entry for custom id.", "TASK3", entry3Returned.get().getTask());
        Assert.assertEquals("Focus needs to be 0 for new entries for custom id.", 0, entry3Returned.get().getFocus() ? 1 : 0);
        Assert.assertEquals("Dropped needs to be 1 for new entries for custom id.", 1, entry3Returned.get().getDropped() ? 1 : 0);
        Assert.assertNull("List needs to be initialized with null for custom id.", entry3Returned.get().getList());
        Assert.assertNull("List position needs to be initialized with null for custom id.", entry3Returned.get().getListPosition());
        Assert.assertNull("Recurrence needs to be initialized with null for custom id.", entry3Returned.get().getRecurrence());
        Assert.assertNull("Reminder date needs to be initialized with null for custom id.", entry3Returned.get().getReminderDate());
        Assert.assertEquals("New entry was not assigned right position for custom id.", 3, entry3Returned.get().getPosition());

        Task taskWithCustomPosition = taskManager.addNewTask(user.getId(), user.getProfile(), "TEST", 0);
        Assert.assertEquals("Added entry did not get custom position assigned.", 0, taskWithCustomPosition.getPosition());
        Optional<Task> entryCustomPosReturned = taskManager.getTask(user.getId(), user.getProfile(), taskWithCustomPosition.getId());
        Assert.assertTrue("Entry with custom position was not saved.", entryCustomPosReturned.isPresent());
        Assert.assertEquals("Returned entry has wrong id for custom position.", taskWithCustomPosition.getId(), entryCustomPosReturned.get().getId());
        Assert.assertEquals("Returned entry has wrong user id for custom position.", user.getId(), entryCustomPosReturned.get().getUserId());
        Assert.assertEquals("Returned entry has wrong profile for custom position.", user.getProfile(), entryCustomPosReturned.get().getProfile());
        Assert.assertEquals("Wrong task saved for new entry for custom position.", "TEST", entryCustomPosReturned.get().getTask());
        Assert.assertEquals("Focus needs to be 0 for new entries for custom position.", 0, entryCustomPosReturned.get().getFocus() ? 1 : 0);
        Assert.assertEquals("Dropped needs to be 1 for new entries for custom position.", 1, entryCustomPosReturned.get().getDropped() ? 1 : 0);
        Assert.assertNull("List needs to be initialized with null for custom position.", entryCustomPosReturned.get().getList());
        Assert.assertNull("List position needs to be initialized with null for custom position.", entryCustomPosReturned.get().getListPosition());
        Assert.assertNull("Recurrence needs to be initialized with null for custom position.", entryCustomPosReturned.get().getRecurrence());
        Assert.assertNull("Reminder date needs to be initialized with null for custom position.", entryCustomPosReturned.get().getReminderDate());
        Assert.assertEquals("Returned entry did not get custom position assigned.", 0, entryCustomPosReturned.get().getPosition());


        List<Task> entries = taskManager.getTasks(user.getId(), user.getProfile());
        Assert.assertEquals(5, entries.size());
        Assert.assertEquals("First entry in list is wrong entry: list needs to be sorted by position.", entries.get(0).getId(), taskWithCustomPosition.getId());
        Assert.assertEquals("Entry in list position " + 1 + " is wrong entry: list needs to be sorted by position.", entries.get(1).getId(), task0.getId());
        Assert.assertEquals("Entry in list position " + 2 + " is wrong entry: list needs to be sorted by position.", entries.get(2).getId(), task2.getId());
        Assert.assertEquals("Entry in list position " + 3 + " is wrong entry: list needs to be sorted by position.", entries.get(3).getId(), task1.getId());
        Assert.assertEquals("Entry in list position " + 4 + " is wrong entry: list needs to be sorted by position.", entries.get(4).getId(), task3.getId());
        for (int i = 0; i < entries.size(); i++)
            Assert.assertEquals("Positions of other entries where not adjusted for entry with custom position.", i, entries.get(i).getPosition());

        try {
            taskManager.addNewTask(user.getId(), user.getProfile(), task0.getId(), "TASK", 0);
            Assert.fail("Duplicate Id did not throw exception for added entry.");
        } catch (DuplicateIdException _) {
        }
        try {
            taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2", entries.size() + 1);
            Assert.fail("Add new entry did not throw exception for position out of bounds.");
        } catch (PositionOutOfBoundException _) {
        }
        try {
            taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), 2, "TASK2", entries.size() + 1);
            Assert.fail("Add new entry did not throw exception for position out of bounds.");
        } catch (PositionOutOfBoundException | DuplicateIdException _) {
        }
        try {
            taskManager.addNewTask(user.getId(), user.getProfile(), 0L, "task", 0);
            Assert.fail("Entry id must not be 0.");
        } catch (InvalidActionException _) {
        }
        database.close();
    }

    @Test
    public void getEntries() {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        TaskManagerI taskManager = database.getTaskManager();
        Task task0 = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");
        Task task1 = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK1");
        Task task2 = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2");

        List<Task> entries = new ArrayList<>();
        entries.add(task0);
        entries.add(task1);
        entries.add(task2);
        List<Task> entriesReturned = taskManager.getTasks(user.getId(), DatabaseTestSuite.user.getProfile());
        Assert.assertEquals("List returned by getEntries is of different size.", entries.size(), entriesReturned.size());
        for (int i = 0; i < entries.size(); i++)
            Assert.assertEquals("List returned by getEntries not equal to original list.", entries.get(i).getId(), entriesReturned.get(i).getId());
        database.close();
    }

    @Test
    public void removeEntry() {

        Database database = DatabaseTestSuite.databaseSupplier.get();
        TaskManagerI taskManager = database.getTaskManager();
        User user = DatabaseTestSuite.user;
        Task task0 = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");
        taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK1");
        taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2");
        Task task3 = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2");

        taskManager.removeTask(user.getId(), user.getProfile(), task3.getId());

        Optional<Task> entryReturned = taskManager.getTask(user.getId(), user.getProfile(), task3.getId());
        Assert.assertTrue("Entry was not removed.", entryReturned.isEmpty());

        List<Task> entriesReturned = taskManager.getTasks(user.getId(), DatabaseTestSuite.user.getProfile());

        for (int i = 0; i < entriesReturned.size(); i++)
            Assert.assertEquals("Remove last entry modified positions of entries with lower position.", i, entriesReturned.get(i).getPosition());

        taskManager.removeTask(user.getId(), user.getProfile(), task0.getId());

        for (int i = 0; i < entriesReturned.size(); i++)
            Assert.assertEquals("Remove entry did not adjust positions of other entries.", i, entriesReturned.get(i).getPosition());
        database.close();
    }

    @Test
    public void updateId() {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        TaskManagerI taskManager = database.getTaskManager();
        User user = DatabaseTestSuite.user;

        Task addedTask = taskManager.addNewTask(user.getId(), user.getProfile(), "TASK");

        long id = getUniqueId(user, database);

        try {
            taskManager.updateId(user.getId(), user.getProfile(), addedTask.getId(), id);
        } catch (DuplicateIdException _) {
        }
        Optional<Task> getOldId = taskManager.getTask(user.getId(), user.getProfile(), addedTask.getId());
        Assert.assertTrue("Entry with old id still exists.", getOldId.isEmpty());
        Optional<Task> getNewId = taskManager.getTask(user.getId(), user.getProfile(), id);
        Assert.assertTrue("Entry with new id does not exists.", getNewId.isPresent());
        try {
            taskManager.updateId(user.getId(), user.getProfile(), id, id);
            Assert.fail("Duplicate Id did not throw exception for update id.");
        } catch (DuplicateIdException _) {
        }
        database.close();
    }

    @Test
    public void swapEntries() throws PositionOutOfBoundException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        TaskManagerI taskManager = database.getTaskManager();

        User user = DatabaseTestSuite.user;

        Task task0 = taskManager.addNewTask(user.getId(), user.getProfile(), "TASK0");
        Task task1 = taskManager.addNewTask(user.getId(), user.getProfile(), "TASK1");
        Task task2 = taskManager.addNewTask(user.getId(), user.getProfile(), "TASK2");

        taskManager.swapTasks(user.getId(), user.getProfile(), task2.getId(), 0);

        List<Task> entries = taskManager.getTasks(user.getId(), user.getProfile());

        Assert.assertEquals(3, entries.size());
        Assert.assertEquals("Entry swap was not successful.", task2.getId(), entries.get(0).getId());
        Assert.assertEquals("Entry swap did not assign correct position.", 0, entries.get(0).getPosition());
        Assert.assertEquals("Entry swap was not successful.", task0.getId(), entries.get(2).getId());
        Assert.assertEquals("Entry swap did not assign correct position.", 2, entries.get(2).getPosition());
        Assert.assertEquals("Entry swap modified neutral position.", task1.getId(), entries.get(1).getId());
        try {
            taskManager.swapTasks(user.getId(), user.getProfile(), task0.getId(), 3);
            Assert.fail("Swap entries did not throw exception for position out of bounds.");
        } catch (PositionOutOfBoundException _) {
        }
        database.close();
    }

    @Test
    public void updateTask() {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        TaskManagerI taskManager = database.getTaskManager();
        Task task = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");

        taskManager.updateTask(user.getId(), user.getProfile(), task.getId(), "TASK NEW");
        Optional<Task> entryReturned = taskManager.getTask(user.getId(), user.getProfile(), task.getId());
        Assert.assertTrue(entryReturned.isPresent());
        Assert.assertEquals("Update task failed.", "TASK NEW", entryReturned.get().getTask());

        taskManager.updateTask(user.getId(), user.getProfile(), task.getId(), null);
        entryReturned = taskManager.getTask(user.getId(), user.getProfile(), task.getId());
        Assert.assertTrue(entryReturned.isPresent());
        Assert.assertNotNull("Task cannot be null.", entryReturned.get().getTask());

        database.close();

    }

    @Test
    public void updateFocus() {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        TaskManagerI taskManager = database.getTaskManager();
        Task task = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");

        taskManager.updateFocus(user.getId(), user.getProfile(), task.getId(), true);
        Optional<Task> entryReturned = taskManager.getTask(user.getId(), user.getProfile(), task.getId());
        Assert.assertTrue(entryReturned.isPresent());
        Assert.assertTrue("Focus has not been set.", entryReturned.get().getFocus());
        Assert.assertFalse("Setting focus did not set dropped to false.", entryReturned.get().getDropped());
        database.close();
    }

    @Test
    public void updateDropped() {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        TaskManagerI taskManager = database.getTaskManager();
        Task task = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");

        taskManager.updateDropped(user.getId(), user.getProfile(), task.getId(), false);
        Optional<Task> entryReturned = taskManager.getTask(user.getId(), user.getProfile(), task.getId());
        Assert.assertTrue(entryReturned.isPresent());
        Assert.assertFalse("Setting dropped did not work.", entryReturned.get().getDropped());
        database.close();
    }


    @Test
    public void updateReminderDate() {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        TaskManagerI taskManager = database.getTaskManager();
        Task task = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");

        Instant instant = Instant.now();
        taskManager.updateReminderDate(user.getId(), user.getProfile(), task.getId(), instant);
        Optional<Task> entryReturned = taskManager.getTask(user.getId(), user.getProfile(), task.getId());
        Assert.assertTrue(entryReturned.isPresent());
        Assert.assertEquals("Reminder date was not updated properly.", instant.toEpochMilli(), entryReturned.get().getReminderDate().toEpochMilli());
        database.close();
    }

    @Test
    public void updateRecurrence() {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        TaskManagerI taskManager = database.getTaskManager();
        Task task = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");

        taskManager.updateRecurrence(user.getId(), user.getProfile(), task.getId(), "m2");
        Optional<Task> entryReturned = taskManager.getTask(user.getId(), user.getProfile(), task.getId());
        Assert.assertTrue(entryReturned.isPresent());
        Assert.assertEquals("Recurrence was not set properly.", "m2", entryReturned.get().getRecurrence());

        database.close();

    }

    @Test
    public void loadFocusAndDropped() {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        TaskManagerI taskManager = database.getTaskManager();
        Task taskFocus = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");
        Task taskDropped = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK1");
        taskManager.updateFocus(user.getId(), user.getProfile(), taskFocus.getId(), true);

        List<Task> focused = taskManager.loadFocus(user.getId(), user.getProfile());
        List<Task> dropped = taskManager.loadDropped(user.getId(), user.getProfile());
        Assert.assertFalse("Focused tasks were not loaded.", focused.isEmpty());
        Assert.assertFalse("Dropped tasks were not loaded.", dropped.isEmpty());
        Assert.assertEquals("Wrong task was loaded for focused tasks.", taskFocus.getId(), focused.get(0).getId());
        Assert.assertEquals("Wrong task was loaded for dropped tasks.", taskDropped.getId(), dropped.get(0).getId());

        database.close();
    }

}
