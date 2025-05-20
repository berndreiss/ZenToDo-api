package net.berndreiss.zentodo.tests;

import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.persistence.ListManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ListTest {
    @Before
    public void prepare() throws InvalidActionException {
        DatabaseTestSuite.prepare();
    }

    @After
    public void cleanUp() throws InvalidActionException {
        DatabaseTestSuite.cleanup();
    }

    @Test
    public void updateList() throws PositionOutOfBoundException, DuplicateIdException, InvalidActionException {

        User user = DatabaseTestSuite.user;
        int profile = user.getProfile();
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        ListManagerI listManager = database.getListManager();
        Entry entry0 = entryManager.addNewEntry(user.getId(), profile, "TASK0");
        Entry entry1 = entryManager.addNewEntry(user.getId(), profile, "TASK1");
        Entry entry2 = entryManager.addNewEntry(user.getId(), profile, "TASK2");

        List<Entry> entriesNoList = listManager.getListEntries(user.getId(), user.getProfile(), null);
        Assert.assertFalse("Did not return entries without a list.", entriesNoList.isEmpty());
        Assert.assertEquals("Returned wrong amount of entries without a list.", 3, entriesNoList.size());

        TaskList list0 = listManager.addList(((ListManager) listManager).getUniqueUserId(), "LIST0", null);
        TaskList listNullId = listManager.addList(list0.getId(), "LIST NULL", null);
        Assert.assertNull("List returned for identical ids was not null.", listNullId);
        List<TaskList> listsDuplId = listManager.getLists();
        Assert.assertTrue("List with identical id was added.", listsDuplId.stream()
                .noneMatch(l -> {
                    if(l.getName() == null)
                        return false;
                    return l.getName().equals("LIST NULL");
                }));
        TaskList list1 = listManager.addList(((ListManager) listManager).getUniqueUserId(), "LIST1", null);
        listManager.updateList(user.getId(), profile, entry0.getId(), list0.getId());
        listManager.updateList(user.getId(), profile, entry1.getId(), list1.getId());
        listManager.updateList(user.getId(), profile, entry2.getId(), list1.getId());

        List<Entry> entries0 = listManager.getListEntries(user.getId(), profile, list0.getId());
        List<Entry> entries1 = listManager.getListEntries(user.getId(), profile, list1.getId());
        Assert.assertEquals(entries0.isEmpty() ? "Entry was not added to list." : "Too many entries were added to list.", 1, entries0.size());
        Assert.assertEquals("Wrong entry found in list.", entry0.getId(), entries0.get(0).getId());
        Assert.assertEquals(entries1.size() > 2 ? "Too few entries were added to list." : "Too many entries were added to list.", 2, entries1.size());
        Assert.assertEquals("Wrong entry found in list.", entry1.getId(), entries1.get(0).getId());
        Assert.assertEquals("Entry has wrong list position.", 0, (int) entries1.get(0).getListPosition());
        Assert.assertEquals("Wrong entry found in list.", entry2.getId(), entries1.get(1).getId());
        Assert.assertEquals("Entry has wrong list position.", 1, (int) entries1.get(1).getListPosition());

        listManager.updateList(user.getId(), profile, entry1.getId(), list0.getId());
        entries0 = listManager.getListEntries(user.getId(), profile, list0.getId());
        entries1 = listManager.getListEntries(user.getId(), profile, list1.getId());
        Assert.assertEquals("Entry was not added to other list.", 2, entries0.size());
        Assert.assertEquals("Entry was not remove from other list.", 1, entries1.size());
        Assert.assertEquals("Wrong entry found in list.", entry2.getId(), entries1.get(0).getId());
        Assert.assertEquals("List position was not adjusted after removing item.", 0, (int) entries1.get(0).getListPosition());

        listManager.updateList(user.getId(), user.getProfile(), entry0.getId(), null);
        List<Entry> listNull = listManager.getListEntries(user.getId(), user.getProfile(), null);
        Optional<Entry> entryNull = listNull.stream().filter(e -> e.getId() == entry0.getId()).findFirst();
        Assert.assertTrue("Entry was not added to null list.", entryNull.isPresent());
        Assert.assertNull("Entries list was not updated for null.", entryNull.get().getList());
        Assert.assertNull("Entries list position was not updated for null.", entryNull.get().getListPosition());

        User userOther = database.getUserManager().addUser(2, "asdfsalkdfjhasdlfkjhasdklsdfjh@asdklfjhasdflkjdhasf.net", null, 0);
        List<Entry> otherList = listManager.getListEntries(userOther.getId(), userOther.getProfile(), list0.getId());
        Assert.assertTrue("Entries for user without entries were returned.", otherList.isEmpty());
        database.close();

    }

    @Test
    public void addList() throws PositionOutOfBoundException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        ListManagerI listManager = database.getListManager();

        TaskList list = listManager.addList(((ListManager) listManager).getUniqueUserId(),"NAME", "GREEN");
        Assert.assertEquals("List has wrong name.", "NAME", list.getName());
        Assert.assertEquals("List has wrong color.", "GREEN", list.getColor());
        TaskList listNull = listManager.addList(((ListManager) listManager).getUniqueUserId(), null, null);
        Assert.assertNull("List has wrong name.", listNull.getName());
        Assert.assertNull("List has wrong color.", listNull.getColor());

        Optional<TaskList> listReturned = listManager.getList(list.getId());
        Assert.assertTrue("List was not added.", listReturned.isPresent());
        Assert.assertEquals("Returned list has wrong name.", "NAME", listReturned.get().getName());
        Assert.assertEquals("Returned list has wrong color.", "GREEN", listReturned.get().getColor());
        Optional<TaskList> listNullReturned = listManager.getList(listNull.getId());
        Assert.assertTrue("List was not added.", listNullReturned.isPresent());
        Assert.assertNull("Returned list has wrong name.", listNullReturned.get().getName());
        Assert.assertNull("Returned list has wrong color.", listNullReturned.get().getColor());

        database.close();
    }

    @Test
    public void removeList() throws PositionOutOfBoundException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        ListManagerI listManager = database.getListManager();

        TaskList list = listManager.addList(((ListManager) listManager).getUniqueUserId(), "NAME", "GREEN");
        listManager.removeList(list.getId());
        Optional<TaskList> listReturned = listManager.getList(list.getId());
        Assert.assertTrue("List was not removed.", listReturned.isEmpty());

        database.close();

    }

    @Test
    public void swapListEntries() throws PositionOutOfBoundException, InvalidActionException {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        ListManagerI listManager = database.getListManager();
        Entry entry0 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");
        Entry entry1 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK1");
        Entry entry2 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2");

        TaskList list = listManager.addList(((ListManager) listManager).getUniqueUserId(), null, null);

        listManager.addUserProfileToList(user.getId(), user.getProfile(), list.getId());
        listManager.updateList(user.getId(), user.getProfile(), entry0.getId(), list.getId());
        listManager.updateList(user.getId(), user.getProfile(), entry1.getId(), list.getId());
        listManager.updateList(user.getId(), user.getProfile(), entry2.getId(), list.getId());

        listManager.swapListEntries(user.getId(), user.getProfile(), list.getId(), entry2.getId(), 0);
        List<Entry> listReturned = listManager.getListEntries(user.getId(), user.getProfile(), list.getId());
        Assert.assertEquals(3, listReturned.size());
        Assert.assertEquals("List position for swapped item was not updated.", 0, (int) listReturned.get(0).getListPosition());
        Assert.assertEquals("List position for swapped item was not updated.", 2, (int) listReturned.get(2).getListPosition());
        Assert.assertEquals("List position for other item was has been changed.", 1, (int) listReturned.get(1).getListPosition());

        database.close();
    }


    @Test
    public void updateListName() throws PositionOutOfBoundException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        ListManagerI listManager = database.getListManager();

        TaskList list = listManager.addList(((ListManager) listManager).getUniqueUserId(), null, null);
        listManager.updateListName(list.getId(), "NAME");
        Optional<TaskList> listReturned = listManager.getList(list.getId());
        Assert.assertTrue(listReturned.isPresent());
        Assert.assertEquals("Name was not updated properly.", "NAME", listReturned.get().getName());

        listManager.updateListName(list.getId(), null);
        listReturned = listManager.getList(list.getId());
        Assert.assertTrue(listReturned.isPresent());
        Assert.assertNull("Name was not updated properly for null.", listReturned.get().getName());

        database.close();
    }


    @Test
    public void updateListColor() throws PositionOutOfBoundException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        ListManagerI listManager = database.getListManager();


        TaskList list = listManager.addList(((ListManager) listManager).getUniqueUserId(), null, null);
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
    public void addUserProfileToList() throws InvalidActionException {
        User user = DatabaseTestSuite.user;
        int profile = user.getProfile();
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        ListManagerI listManager = database.getListManager();

        TaskList list0 = listManager.addList(((ListManager) listManager).getUniqueUserId(), "LIST0", null);
        TaskList list1 = listManager.addList(((ListManager) listManager).getUniqueUserId(), "LIST1", null);

        listManager.addUserProfileToList(user.getId(), profile, list0.getId());
        listManager.addUserProfileToList(user.getId(), profile, list1.getId());

        List<TaskList> lists = listManager.getListsForUser(user.getId(), profile);
        Assert.assertFalse("No lists were associated with the user.", lists.isEmpty());
        Assert.assertEquals("The wrong amount of lists were associated with the user.", 2, lists.size());
        Assert.assertTrue("List0 is not in the returned list.", lists.contains(list0));
        Assert.assertTrue("List1 is not in the returned list.", lists.contains(list1));

        try{
            TaskList listDupl = listManager.addList(((ListManager) listManager).getUniqueUserId(), "LIST0", null);
            listManager.addUserProfileToList(user.getId(), user.getProfile(), listDupl.getId());
            Assert.fail("List with existing name was added for user.");
        } catch (InvalidActionException _){}
        database.close();


    }

    @Test
    public void removeUserProfileFromList() throws InvalidActionException {
        User user = DatabaseTestSuite.user;
        int profile = user.getProfile();
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        ListManagerI listManager = database.getListManager();

        TaskList list0 = listManager.addList(((ListManager) listManager).getUniqueUserId(), "LIST0", null);

        listManager.addUserProfileToList(user.getId(), profile, list0.getId());

        listManager.removeUserProfileFromList(user.getId(), profile, list0.getId());
        List<TaskList> lists = listManager.getListsForUser(user.getId(), profile);
        Assert.assertTrue("List was not removed from user.", lists.isEmpty());

        database.close();
    }

    @Test
    public void getLists() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        ListManagerI listManager = database.getListManager();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(2, "sdlfkhasdflkhasdfladkjshf@asdklfjhasdflkjadhsfaksdjfh.net", null, 0);
        TaskList list0 = listManager.addList(((ListManager) listManager).getUniqueUserId(), "LIST0", null);
        TaskList list1 = listManager.addList(((ListManager) listManager).getUniqueUserId(), "LIST1", null);

        listManager.addUserProfileToList(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile(), list0.getId());
        listManager.addUserProfileToList(user.getId(), user.getProfile(), list1.getId());

        List<TaskList> lists0 = listManager.getListsForUser(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile());
        Assert.assertEquals(1, lists0.size());
        Assert.assertEquals("Wrong list was associated with the default user.", list0.getId(), lists0.get(0).getId());

        List<TaskList> lists1 = listManager.getListsForUser(user.getId(), user.getProfile());
        Assert.assertEquals(1,  lists1.size());
        Assert.assertEquals("Wrong list was associated with the default user.", list1.getId(), lists1.get(0).getId());

        List<TaskList> listsAll = listManager.getLists();
        Assert.assertEquals("Not all lists were returned", 2, listsAll.size());
    }
    @Test
    public void updateId() throws InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        User user = DatabaseTestSuite.user;
        ListManagerI listManager = database.getListManager();
        EntryManagerI entryManager = database.getEntryManager();

        TaskList list0 = listManager.addList(0, "LIST0", null);
        TaskList list1 = listManager.addList(1, "LIST1", null);
        TaskList list2 = listManager.addList(2, "LIST2", null);
        Entry entry = entryManager.addNewEntry(user.getId(), user.getProfile(), "TASK");
        listManager.addUserProfileToList(user.getId(), user.getProfile(), list0.getId());
        listManager.updateList(user.getId(), user.getProfile(), entry.getId(), list0.getId());
        Long newId = listManager.updateId(list0.getId(), 3);
        Assert.assertNull("Id was returned.", newId);
        Optional<TaskList> listReturned = listManager.getList(3);
        Assert.assertTrue("List id was not updated.", listReturned.isPresent());
        Assert.assertEquals("List id was updated for wrong list.", "LIST0", listReturned.get().getName());

        List<TaskList> listsUser = listManager.getListsForUser(user.getId(), user.getProfile());
        Assert.assertFalse("List was not moved to new list id for user profile.", listsUser.isEmpty());
        Assert.assertEquals("Wrong list was returned.", 3, listsUser.get(0).getId());

        List<Entry> entries = listManager.getListEntries(user.getId(), user.getProfile(), 3L);
        Assert.assertFalse("Entry was not moved to new list id.", entries.isEmpty());

        listReturned = listManager.getList(1);
        newId = listManager.updateId(3, 1);
        listReturned = listManager.getList(1);
        Assert.assertTrue("List id was not updated.", listReturned.isPresent());
        Assert.assertEquals("List id was updated for wrong list.", "LIST0", listReturned.get().getName());

        List<TaskList> lists = listManager.getLists();
        Optional<TaskList> listChanged = lists.stream().filter(l -> l.getName().equals("LIST1")).findFirst();
        Assert.assertTrue("List does not exist anymore.", listChanged.isPresent());
        Assert.assertNotEquals("List id has not changed.", 1 , listChanged.get().getId());
        Assert.assertEquals("Wrong id was returned when update id.", listChanged.get().getId(), (long) newId);
    }
    @Test
    public void getListByName() throws InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        User user = DatabaseTestSuite.user;
        ListManagerI listManager = database.getListManager();
        UserManagerI userManager = database.getUserManager();
        TaskList list0 = listManager.addList(((ListManager) listManager).getUniqueUserId(), "LIST0", null);
        TaskList list1 = listManager.addList(((ListManager) listManager).getUniqueUserId(), "LIST1", null);

        Optional<TaskList> listReturned = listManager.getListByName(user.getId(), user.getProfile(), "LIST0");
        Assert.assertTrue("List for user was returned without assigning it.", listReturned.isEmpty());

        listManager.addUserProfileToList(user.getId(), user.getProfile(), list0.getId());

        listReturned = listManager.getListByName(user.getId(), user.getProfile(), "LIST0");
        Assert.assertTrue("List for user was not returned after assigning it.", listReturned.isPresent());

    }
}