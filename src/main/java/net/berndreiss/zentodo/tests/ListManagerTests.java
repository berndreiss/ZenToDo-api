package net.berndreiss.zentodo.tests;

import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.exceptions.DuplicateIdException;
import net.berndreiss.zentodo.exceptions.InvalidActionException;
import net.berndreiss.zentodo.exceptions.PositionOutOfBoundException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;

//TODO test for invalidaction when updating task with list not assigned to user profile
//TODO check for getList(null) -> should return empty
//TODO check for proper list profile user associations -> maybe add client stub test class?
/**
 * Test list operation (see TaskListI.java).
 */
public class ListManagerTests {
    /**
     * Returns a unique id for a new list.
     *
     * @param database the database to use
     * @return a unique id
     */
    public static long getUniqueListId(Database database) {
        Random random = new Random();
        long id = random.nextLong();
        while (database.getListManager().getList(id).isPresent())
            id = random.nextLong();
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
    public void updateList() throws DuplicateIdException, InvalidActionException {

        User user = DatabaseTestSuite.user;
        int profile = user.getProfile();
        Database database = DatabaseTestSuite.databaseSupplier.get();
        TaskManagerI taskManager = database.getTaskManager();
        ListManagerI listManager = database.getListManager();
        Task task0 = taskManager.addNewTask(user.getId(), profile, "TASK0");
        Task task1 = taskManager.addNewTask(user.getId(), profile, "TASK1");
        Task task2 = taskManager.addNewTask(user.getId(), profile, "TASK2");

        List<Task> entriesNoList = listManager.getListEntries(user.getId(), user.getProfile(), null);
        Assert.assertFalse("Did not return entries without a list.", entriesNoList.isEmpty());
        Assert.assertEquals("Returned wrong amount of entries without a list.", 3, entriesNoList.size());

        TaskList list0 = listManager.addList(getUniqueListId(database), "LIST0", null);
        TaskList listNullId = listManager.addList(list0.getId(), "LIST NULL", null);
        Assert.assertNull("List returned for identical ids was not null.", listNullId);
        List<TaskList> listsDuplicateId = listManager.getLists();
        Assert.assertTrue("List with identical id was added.", listsDuplicateId.stream()
                .noneMatch(l -> {
                    if (l.getName() == null)
                        return false;
                    return l.getName().equals("LIST NULL");
                }));
        TaskList list1 = listManager.addList(getUniqueListId(database), "LIST1", null);
        listManager.updateList(user.getId(), profile, task0.getId(), list0.getId());
        listManager.updateList(user.getId(), profile, task1.getId(), list1.getId());
        listManager.updateList(user.getId(), profile, task2.getId(), list1.getId());

        List<Task> entries0 = listManager.getListEntries(user.getId(), profile, list0.getId());
        List<Task> entries1 = listManager.getListEntries(user.getId(), profile, list1.getId());
        Assert.assertEquals(entries0.isEmpty() ? "Entry was not added to list." : "Too many entries were added to list.", 1, entries0.size());
        Assert.assertEquals("Wrong entry found in list.", task0.getId(), entries0.getFirst().getId());
        Assert.assertEquals(entries1.size() > 2 ? "Too few entries were added to list." : "Too many entries were added to list.", 2, entries1.size());
        Assert.assertEquals("Wrong entry found in list.", task1.getId(), entries1.get(0).getId());
        Assert.assertEquals("Entry has wrong list position.", 0, (int) entries1.get(0).getListPosition());
        Assert.assertEquals("Wrong entry found in list.", task2.getId(), entries1.get(1).getId());
        Assert.assertEquals("Entry has wrong list position.", 1, (int) entries1.get(1).getListPosition());

        listManager.updateList(user.getId(), profile, task1.getId(), list0.getId());
        entries0 = listManager.getListEntries(user.getId(), profile, list0.getId());
        entries1 = listManager.getListEntries(user.getId(), profile, list1.getId());
        Assert.assertEquals("Entry was not added to other list.", 2, entries0.size());
        Assert.assertEquals("Entry was not remove from other list.", 1, entries1.size());
        Assert.assertEquals("Wrong entry found in list.", task2.getId(), entries1.getFirst().getId());
        Assert.assertEquals("List position was not adjusted after removing item.", 0, (int) entries1.getFirst().getListPosition());

        listManager.updateList(user.getId(), user.getProfile(), task0.getId(), null);
        List<Task> listNull = listManager.getListEntries(user.getId(), user.getProfile(), null);
        Optional<Task> entryNull = listNull.stream().filter(e -> e.getId() == task0.getId()).findFirst();
        Assert.assertTrue("Entry was not added to null list.", entryNull.isPresent());
        Assert.assertNull("Entries list was not updated for null.", entryNull.get().getList());
        Assert.assertNull("Entries list position was not updated for null.", entryNull.get().getListPosition());

        User userOther = database.getUserManager().addUser(2, "asdfsalkdfjhasdlfkjhasdklsdfjh@asdklfjhasdflkjdhasf.net", null, 0);
        List<Task> otherList = listManager.getListEntries(userOther.getId(), userOther.getProfile(), list0.getId());
        Assert.assertTrue("Entries for user without entries were returned.", otherList.isEmpty());
        database.close();

    }

    //TODO check for duplicate id
    @Test
    public void addList() throws InvalidActionException, DuplicateIdException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        ListManagerI listManager = database.getListManager();

        TaskList list = listManager.addList(getUniqueListId(database), "NAME", "GREEN");
        Assert.assertEquals("List has wrong name.", "NAME", list.getName());
        Assert.assertEquals("List has wrong color.", "GREEN", list.getColor());
        TaskList listNull = listManager.addList(getUniqueListId(database), "", null);
        Assert.assertTrue("List has wrong name.", listNull.getName().isEmpty());
        Assert.assertNull("List has wrong color.", listNull.getColor());

        Optional<TaskList> listReturned = listManager.getList(list.getId());
        Assert.assertTrue("List was not added.", listReturned.isPresent());
        Assert.assertEquals("Returned list has wrong name.", "NAME", listReturned.get().getName());
        Assert.assertEquals("Returned list has wrong color.", "GREEN", listReturned.get().getColor());
        Optional<TaskList> listNullReturned = listManager.getList(listNull.getId());
        Assert.assertTrue("List was not added.", listNullReturned.isPresent());
        Assert.assertTrue("Returned list has wrong name.", listNullReturned.get().getName().isEmpty());
        Assert.assertNull("Returned list has wrong color.", listNullReturned.get().getColor());

        try {
            listManager.addList(getUniqueListId(database), null, null);
            Assert.fail("List with null name was added.");
        } catch (InvalidActionException _) {
        }
        database.close();
    }

    @Test
    public void removeList() throws InvalidActionException, DuplicateIdException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        User user = DatabaseTestSuite.user;
        ListManagerI listManager = database.getListManager();

        TaskList list = listManager.addList(getUniqueListId(database), "NAME", "GREEN");
        listManager.addUserProfileToList(user.getId(), user.getProfile(), list.getId());
        Task task = database.getTaskManager().addNewTask(user.getId(), user.getProfile(), "TASK");
        listManager.updateList(user.getId(), user.getProfile(), task.getId(), list.getId());

        listManager.removeList(list.getId());
        Optional<TaskList> listReturned = listManager.getList(list.getId());
        Assert.assertTrue("List was not removed.", listReturned.isEmpty());

        List<Task> entries = listManager.getListEntries(user.getId(), user.getProfile(), list.getId());
        Assert.assertTrue("List was not removed from entries.", entries.isEmpty());

        Optional<Task> entryReturned = database.getTaskManager().getTask(user.getId(), user.getProfile(), task.getId());
        Assert.assertTrue("Entry was removed too.", entryReturned.isPresent());
        Assert.assertNull("Entries list field was not set to null.", entryReturned.get().getList());
        Assert.assertNull("Entries list position field was not set to null.", entryReturned.get().getListPosition());
        database.close();

    }

    @Test
    public void swapListEntries() throws PositionOutOfBoundException, InvalidActionException, DuplicateIdException {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        TaskManagerI taskManager = database.getTaskManager();
        ListManagerI listManager = database.getListManager();
        Task task0 = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");
        Task task1 = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK1");
        Task task2 = taskManager.addNewTask(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2");

        TaskList list = listManager.addList(getUniqueListId(database), "", null);

        listManager.addUserProfileToList(user.getId(), user.getProfile(), list.getId());
        listManager.updateList(user.getId(), user.getProfile(), task0.getId(), list.getId());
        listManager.updateList(user.getId(), user.getProfile(), task1.getId(), list.getId());
        listManager.updateList(user.getId(), user.getProfile(), task2.getId(), list.getId());

        listManager.swapListEntries(user.getId(), user.getProfile(), task2.getId(), list.getId(), 0);
        List<Task> listReturned = listManager.getListEntries(user.getId(), user.getProfile(), list.getId());
        Assert.assertEquals(3, listReturned.size());
        Assert.assertEquals("List position for swapped item was not updated.", 0, (int) listReturned.get(0).getListPosition());
        Assert.assertEquals("List position for swapped item was not updated.", 2, (int) listReturned.get(2).getListPosition());
        Assert.assertEquals("List position for other item was has been changed.", 1, (int) listReturned.get(1).getListPosition());

        try {
            listManager.swapListEntries(user.getId(), user.getProfile(), task2.getId(), list.getId(), 3);
            Assert.fail("PositionOutOfBounds exception was not thrown.");
        } catch (PositionOutOfBoundException _) {
        }
        database.close();
    }

    @Test
    public void updateListName() throws InvalidActionException, DuplicateIdException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        ListManagerI listManager = database.getListManager();

        TaskList list = listManager.addList(getUniqueListId(database), "", null);
        listManager.updateListName(list.getId(), "NAME");
        Optional<TaskList> listReturned = listManager.getList(list.getId());
        Assert.assertTrue(listReturned.isPresent());
        Assert.assertEquals("Name was not updated properly.", "NAME", listReturned.get().getName());

        try {
            listManager.updateListName(list.getId(), null);
            Assert.fail("List name must not be null.");
        } catch (InvalidActionException _) {
        }
        database.close();
    }

    @Test
    public void updateListColor() throws InvalidActionException, DuplicateIdException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        ListManagerI listManager = database.getListManager();


        TaskList list = listManager.addList(getUniqueListId(database), "", null);
        listManager.updateListColor(list.getId(), "COLOR");
        Optional<TaskList> listReturned = listManager.getList(list.getId());
        Assert.assertTrue(listReturned.isPresent());
        Assert.assertEquals("Color was not updated properly.", "COLOR", listReturned.get().getColor());

        listManager.updateListColor(list.getId(), null);
        listReturned = listManager.getList(list.getId());
        Assert.assertTrue(listReturned.isPresent());
        Assert.assertNull("Color was not updated properly for null.", listReturned.get().getColor());

        database.close();


    }

    @Test
    public void addUserProfileToList() throws InvalidActionException, DuplicateIdException {
        User user = DatabaseTestSuite.user;
        int profile = user.getProfile();
        Database database = DatabaseTestSuite.databaseSupplier.get();
        ListManagerI listManager = database.getListManager();

        TaskList list0 = listManager.addList(getUniqueListId(database), "LIST0", null);
        TaskList list1 = listManager.addList(getUniqueListId(database), "LIST1", null);

        listManager.addUserProfileToList(user.getId(), profile, list0.getId());
        listManager.addUserProfileToList(user.getId(), profile, list1.getId());

        List<TaskList> lists = listManager.getListsForUser(user.getId(), profile);
        Assert.assertFalse("No lists were associated with the user.", lists.isEmpty());
        Assert.assertEquals("The wrong amount of lists were associated with the user.", 2, lists.size());
        Assert.assertTrue("List0 is not in the returned list.", lists.stream().anyMatch(l -> l.getName().equals("LIST0")));
        Assert.assertTrue("List1 is not in the returned list.", lists.stream().anyMatch(l -> l.getName().equals("LIST1")));

        try {
            TaskList listDuplicate = listManager.addList(getUniqueListId(database), "LIST0", null);
            listManager.addUserProfileToList(user.getId(), user.getProfile(), listDuplicate.getId());
            Assert.fail("List with existing name was added for user.");
        } catch (InvalidActionException _) {
        }
        database.close();


    }

    @Test
    public void removeUserProfileFromList() throws InvalidActionException, DuplicateIdException {
        User user = DatabaseTestSuite.user;
        int profile = user.getProfile();
        Database database = DatabaseTestSuite.databaseSupplier.get();
        ListManagerI listManager = database.getListManager();

        TaskList list0 = listManager.addList(getUniqueListId(database), "LIST0", null);
        Task task = database.getTaskManager().addNewTask(user.getId(), user.getProfile(), "TASK");
        listManager.addUserProfileToList(user.getId(), user.getProfile(), list0.getId());
        listManager.updateList(user.getId(), user.getProfile(), task.getId(), list0.getId());

        listManager.addUserProfileToList(user.getId(), user.getProfile(), list0.getId());
        listManager.addUserProfileToList(user.getId(), profile, list0.getId());

        listManager.removeUserProfileFromList(user.getId(), profile, list0.getId());
        List<TaskList> lists = listManager.getListsForUser(user.getId(), profile);
        Assert.assertTrue("List was not removed from user.", lists.isEmpty());


        List<Task> entries = listManager.getListEntries(user.getId(), user.getProfile(), list0.getId());
        Assert.assertTrue("List was not removed from entries.", entries.isEmpty());

        Optional<Task> entryReturned = database.getTaskManager().getTask(user.getId(), user.getProfile(), task.getId());
        Assert.assertTrue("Entry was removed too.", entryReturned.isPresent());
        Assert.assertNull("Entries list field was not set to null.", entryReturned.get().getList());
        Assert.assertNull("Entries list position field was not set to null.", entryReturned.get().getListPosition());
        database.close();
    }

    @Test
    public void getLists() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        ListManagerI listManager = database.getListManager();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(2, "sdlfkhasdflkhasdfladkjshf@asdklfjhasdflkjadhsfaksdjfh.net", null, 0);
        TaskList list0 = listManager.addList(getUniqueListId(database), "LIST0", null);
        TaskList list1 = listManager.addList(getUniqueListId(database), "LIST1", null);

        listManager.addUserProfileToList(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile(), list0.getId());
        listManager.addUserProfileToList(user.getId(), user.getProfile(), list1.getId());

        List<TaskList> lists0 = listManager.getListsForUser(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile());
        Assert.assertEquals(1, lists0.size());
        Assert.assertEquals("Wrong list was associated with the default user.", list0.getId(), lists0.getFirst().getId());

        List<TaskList> lists1 = listManager.getListsForUser(user.getId(), user.getProfile());
        Assert.assertEquals(1, lists1.size());
        Assert.assertEquals("Wrong list was associated with the default user.", list1.getId(), lists1.getFirst().getId());

        List<TaskList> listsAll = listManager.getLists();
        Assert.assertEquals("Not all lists were returned", 2, listsAll.size());
    }

    @Test
    public void updateId() throws InvalidActionException, DuplicateIdException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        User user = DatabaseTestSuite.user;
        ListManagerI listManager = database.getListManager();
        TaskManagerI taskManager = database.getTaskManager();

        TaskList list0 = listManager.addList(0, "LIST0", null);
        listManager.addList(1, "LIST1", null);
        listManager.addList(2, "LIST2", null);
        Task task = taskManager.addNewTask(user.getId(), user.getProfile(), "TASK");
        listManager.addUserProfileToList(user.getId(), user.getProfile(), list0.getId());
        listManager.updateList(user.getId(), user.getProfile(), task.getId(), list0.getId());
        listManager.updateId(list0.getId(), 3);
        Optional<TaskList> listReturned = listManager.getList(3L);
        Assert.assertTrue("List id was not updated.", listReturned.isPresent());
        Assert.assertEquals("List id was updated for wrong list.", "LIST0", listReturned.get().getName());

        List<TaskList> listsUser = listManager.getListsForUser(user.getId(), user.getProfile());
        Assert.assertFalse("List was not moved to new list id for user profile.", listsUser.isEmpty());
        Assert.assertEquals("Wrong list was returned.", 3, listsUser.getFirst().getId());

        List<Task> entries = listManager.getListEntries(user.getId(), user.getProfile(), 3L);
        Assert.assertFalse("Entry was not moved to new list id.", entries.isEmpty());

        try {
            listManager.updateId(3, 1);
            Assert.fail("DuplicateIdException has not been thrown.");
        } catch (DuplicateIdException _) {
        }
    }

    @Test
    public void getListByName() throws InvalidActionException, DuplicateIdException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        User user = DatabaseTestSuite.user;
        ListManagerI listManager = database.getListManager();
        TaskList list0 = listManager.addList(getUniqueListId(database), "LIST0", null);
        listManager.addList(getUniqueListId(database), "LIST1", null);

        Optional<TaskList> listReturned = listManager.getListByName(user.getId(), user.getProfile(), "LIST0");
        Assert.assertTrue("List for user was returned without assigning it.", listReturned.isEmpty());

        listManager.addUserProfileToList(user.getId(), user.getProfile(), list0.getId());

        listReturned = listManager.getListByName(user.getId(), user.getProfile(), "LIST0");
        Assert.assertTrue("List for user was not returned after assigning it.", listReturned.isPresent());

        listReturned = listManager.getListByName(user.getId(), user.getProfile(), null);
        Assert.assertTrue("List was returned for name == null.", listReturned.isEmpty());

    }
}