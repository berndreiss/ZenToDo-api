package net.berndreiss.zentodo.tests;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import net.berndreiss.zentodo.data.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class EntryTest {
    @Before
    public void prepare() throws InvalidActionException {
        DatabaseTestSuite.prepare();}

    @After
    public void cleanUp() throws InvalidActionException {
        DatabaseTestSuite.cleanup();}

    public static long getUniqueId(User user, Database database){
        EntryManagerI entryManager = database.getEntryManager();
        List<Entry> entries = entryManager.getEntries(user.getId(), user.getProfile());
        Set<Long> existingIds = entries.stream().map(Entry::getId).collect(Collectors.toSet());
        long id = 42;

        while (existingIds.contains(id))
            id++;

        return id;
    }

    @Test
    public void addNewEntry() throws PositionOutOfBoundException {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        Entry entry0 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");
        Entry entry1 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK1");

        Optional<Entry> entry0Returned = entryManager.getEntry(user.getId(), DatabaseTestSuite.user.getProfile(), entry0.getId());

        Assert.assertTrue("entry0 should be present", entry0Returned.isPresent());
        Assert.assertEquals("Returned entry has wrong id.", entry0.getId(), entry0Returned.get().getId());
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

        Optional<Entry> entry1Returned = entryManager.getEntry(user.getId(), DatabaseTestSuite.user.getProfile(), entry1.getId());
        Assert.assertTrue("entry1 should be present", entry1Returned.isPresent());
        Assert.assertEquals(
                entry1.getPosition() == 0 ? "Positions for added entries is not incremented." : "Position for added entry is arbitrary.",
                1, entry1Returned.get().getPosition()
        );

        Entry entry2 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2", 1);
        Optional<Entry> entry2Returned = entryManager.getEntry(user.getId(), DatabaseTestSuite.user.getProfile(),  entry2.getId());
        Assert.assertTrue("entry2 should be present", entry2Returned.isPresent());
        Assert.assertEquals("Returned entry has wrong id for custom position.", entry2.getId(), entry2Returned.get().getId());
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

        entry0Returned = entryManager.getEntry(user.getId(), DatabaseTestSuite.user.getProfile(), entry0.getId());
        Assert.assertTrue("entry0 should still be present", entry0Returned.isPresent());
        Assert.assertEquals("Adding entry with custom position must not modify positions of entries before it.", 0, entry0Returned.get().getPosition());

        List<Entry> ens = database.getEntryManager().getEntries(user.getId(), user.getProfile());
        entry1Returned = entryManager.getEntry(user.getId(), DatabaseTestSuite.user.getProfile(), entry1.getId());
        Assert.assertEquals("Adding entry with custom position needs to adjust the position of entries after it.", 2, entry1Returned.get().getPosition());
        Assert.assertTrue("entry1 should still be present", entry1Returned.isPresent());

        long id = getUniqueId(user, database);
        Entry entry3 = null;
        try {
            entry3 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), id, "TASK3", 3);
        } catch(DuplicateIdException _){}
        Optional<Entry> entry3Returned = entryManager.getEntry(user.getId(), DatabaseTestSuite.user.getProfile(), entry3.getId());
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

        Entry entryWithCustomPosition = entryManager.addNewEntry(user.getId(), user.getProfile(), "TEST", 0);
        Assert.assertEquals("Added entry did not get custom position assigned.", 0, entryWithCustomPosition.getPosition());
        Optional<Entry> entryCustPosReturned = entryManager.getEntry(user.getId(), user.getProfile(), entryWithCustomPosition.getId());
        Assert.assertTrue("Entry with custom position was not saved.", entryCustPosReturned.isPresent());
        Assert.assertEquals("Returned entry has wrong id for custom position.", entryWithCustomPosition.getId(), entryCustPosReturned.get().getId());
        Assert.assertEquals("Returned entry has wrong user id for custom position.", user.getId(), entryCustPosReturned.get().getUserId());
        Assert.assertEquals("Returned entry has wrong profile for custom position.", user.getProfile(), entryCustPosReturned.get().getProfile());
        Assert.assertEquals("Wrong task saved for new entry for custom position.", "TEST", entryCustPosReturned.get().getTask());
        Assert.assertEquals("Focus needs to be 0 for new entries for custom position.", 0, entryCustPosReturned.get().getFocus() ? 1 : 0);
        Assert.assertEquals("Dropped needs to be 1 for new entries for custom position.", 1, entryCustPosReturned.get().getDropped() ? 1 : 0);
        Assert.assertNull("List needs to be initialized with null for custom position.", entryCustPosReturned.get().getList());
        Assert.assertNull("List position needs to be initialized with null for custom position.", entryCustPosReturned.get().getListPosition());
        Assert.assertNull("Recurrence needs to be initialized with null for custom position.", entryCustPosReturned.get().getRecurrence());
        Assert.assertNull("Reminder date needs to be initialized with null for custom position.", entryCustPosReturned.get().getReminderDate());
        Assert.assertEquals("Returned entry did not get custom position assigned.", 0, entryCustPosReturned.get().getPosition());


        List<Entry> entries = entryManager.getEntries(user.getId(), user.getProfile());
        Assert.assertEquals(5, entries.size());
        Assert.assertEquals("First entry in list is wrong entry: list needs to be sorted by position.", entries.get(0).getId(), entryWithCustomPosition.getId());
        Assert.assertEquals("Entry in list position " + 1 + " is wrong entry: list needs to be sorted by position.", entries.get(1).getId(), entry0.getId());
        Assert.assertEquals("Entry in list position " + 2 + " is wrong entry: list needs to be sorted by position.", entries.get(2).getId(), entry2.getId());
        Assert.assertEquals("Entry in list position " + 3 + " is wrong entry: list needs to be sorted by position.", entries.get(3).getId(), entry1.getId());
        Assert.assertEquals("Entry in list position " + 4 + " is wrong entry: list needs to be sorted by position.", entries.get(4).getId(), entry3.getId());
        for (int i = 0; i < entries.size(); i++)
            Assert.assertEquals("Positions of other entries where not adjusted for entry with custom position.", i, entries.get(i).getPosition());

        try {
            entryManager.addNewEntry(user.getId(), user.getProfile(), entry0.getId(), "TASK", 0);
            Assert.fail("Duplicate Id did not throw exception for added entry.");
        } catch (DuplicateIdException _){}
        try {
            entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2", entries.size()+1);
            Assert.fail("Add new entry did not throw exception for position out of bounds.");
        } catch (PositionOutOfBoundException _){}
        try {
            entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(),2, "TASK2", entries.size()+1);
            Assert.fail("Add new entry did not throw exception for position out of bounds.");
        } catch (PositionOutOfBoundException | DuplicateIdException _){}
        database.close();
    }

    @Test
    public void getEntries() throws PositionOutOfBoundException {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        Entry entry0 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");
        Entry entry1 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK1");
        Entry entry2 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2");

        List<Entry> entries = new ArrayList<>();
        entries.add(entry0);
        entries.add(entry1);
        entries.add(entry2);
        List<Entry> entriesReturned = entryManager.getEntries(user.getId(), DatabaseTestSuite.user.getProfile());
        Assert.assertEquals("List returned by getEntries is of different size.", entries.size(), entriesReturned.size());
        for (int i =0; i < entries.size(); i++)
            Assert.assertEquals("List returned by getEntries not equal to original list.", entries.get(i).getId(), entriesReturned.get(i).getId());
        database.close();
    }

    @Test
    public void removeEntry() throws PositionOutOfBoundException {

        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        User user = DatabaseTestSuite.user;
        Entry entry0 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");
        entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK1");
        entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2");
        Entry entry3 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2");

        entryManager.removeEntry(user.getId(), user.getProfile(), entry3.getId());

        Optional<Entry> entryReturned = entryManager.getEntry(user.getId(), user.getProfile(), entry3.getId());
        Assert.assertTrue("Entry was not removed.", entryReturned.isEmpty());

        List<Entry> entriesReturned = entryManager.getEntries(user.getId(), DatabaseTestSuite.user.getProfile());

        for (int i = 0; i < entriesReturned.size(); i++)
            Assert.assertEquals("Remove last entry modified positions of entries with lower position.", i, entriesReturned.get(i).getPosition());

        entryManager.removeEntry(user.getId(), user.getProfile(), entry0.getId());

        for (int i = 0; i < entriesReturned.size(); i++)
            Assert.assertEquals("Remove entry did not adjust positions of other entries.", i, entriesReturned.get(i).getPosition());
        database.close();
    }

    @Test
    public void updateId() throws PositionOutOfBoundException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        User user = DatabaseTestSuite.user;

        Entry addedEntry = entryManager.addNewEntry(user.getId(), user.getProfile(), "TASK");

        long id = getUniqueId(user, database);

        try {
            entryManager.updateId(user.getId(), user.getProfile(), addedEntry.getId(), id);
        } catch(DuplicateIdException _){}
        Optional<Entry> getOldId = entryManager.getEntry(user.getId(), user.getProfile(), addedEntry.getId());
        Assert.assertTrue("Entry with old id still exists.", getOldId.isEmpty());
        Optional<Entry> getNewId = entryManager.getEntry(user.getId(), user.getProfile(), id);
        Assert.assertTrue("Entry with new id does not exists.", getNewId.isPresent());
        try {
            entryManager.updateId(user.getId(), user.getProfile(), id, id);
            Assert.fail("Duplicate Id did not throw exception for update id.");
        } catch (DuplicateIdException _){}
        database.close();
    }

    @Test
    public void swapEntries() throws PositionOutOfBoundException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();

        User user = DatabaseTestSuite.user;

        Entry entry0  = entryManager.addNewEntry(user.getId(), user.getProfile(), "TASK0");
        Entry entry1  = entryManager.addNewEntry(user.getId(), user.getProfile(), "TASK1");
        Entry entry2  = entryManager.addNewEntry(user.getId(), user.getProfile(), "TASK2");

        entryManager.swapEntries(user.getId(), user.getProfile(), entry2.getId(), 0);

        List<Entry> entries = entryManager.getEntries(user.getId(), user.getProfile());

        Assert.assertEquals(3, entries.size());
        Assert.assertEquals("Entry swap was not successful.", entry2.getId(), entries.get(0).getId());
        Assert.assertEquals("Entry swap did not assign correct position.", 0, entries.get(0).getPosition());
        Assert.assertEquals("Entry swap was not successful.", entry0.getId(), entries.get(2).getId());
        Assert.assertEquals("Entry swap did not assign correct position.", 2, entries.get(2).getPosition());
        Assert.assertEquals("Entry swap modified neutral position.", entry1.getId(), entries.get(1).getId());
        try {
            entryManager.swapEntries(user.getId(), user.getProfile(), entry0.getId(), 3);
            Assert.fail("Swap entries did not throw exception for position out of bounds.");
        } catch (PositionOutOfBoundException _){}
        database.close();
    }

    @Test
    public void updateTask() throws PositionOutOfBoundException {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        Entry entry = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");

        entryManager.updateTask(user.getId(), user.getProfile(), entry.getId(), "TASKNEW");
        Optional<Entry> entryReturned = entryManager.getEntry(user.getId(), user.getProfile(), entry.getId());
        Assert.assertTrue(entryReturned.isPresent());
        Assert.assertEquals("Update task failed.", "TASKNEW", entryReturned.get().getTask());

        entryManager.updateTask(user.getId(), user.getProfile(), entry.getId(), null);
        entryReturned = entryManager.getEntry(user.getId(), user.getProfile(), entry.getId());
        Assert.assertTrue(entryReturned.isPresent());
        Assert.assertNotNull("Task cannot be null.", entryReturned.get().getTask());

        database.close();

    }

    @Test
    public void updateFocus() throws PositionOutOfBoundException {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        Entry entry = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");

        entryManager.updateFocus(user.getId(),user.getProfile(), entry.getId(), true);
        Optional<Entry> entryReturned = entryManager.getEntry(user.getId(), user.getProfile(), entry.getId());
        Assert.assertTrue(entryReturned.isPresent());
        Assert.assertTrue("Focus has not been set.", entryReturned.get().getFocus());
        Assert.assertFalse("Setting focus did not set dropped to false.", entryReturned.get().getDropped());
        database.close();
    }

    @Test
    public void updateDropped() throws PositionOutOfBoundException {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        Entry entry = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");

        entryManager.updateDropped(user.getId(),user.getProfile(), entry.getId(), false);
        Optional<Entry> entryReturned = entryManager.getEntry(user.getId(), user.getProfile(), entry.getId());
        Assert.assertTrue(entryReturned.isPresent());
        Assert.assertFalse("Setting dropped did not work.", entryReturned.get().getDropped());
        database.close();
    }



    @Test
    public void updateReminderDate() throws PositionOutOfBoundException {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        Entry entry = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");

        Instant instant = Instant.now();
        entryManager.updateReminderDate(user.getId(), user.getProfile(), entry.getId(), instant);
        Optional<Entry> entryReturned = entryManager.getEntry(user.getId(), user.getProfile(), entry.getId());
        Assert.assertTrue(entryReturned.isPresent());
        Assert.assertEquals("Reminder date was not updated properly.", instant.toEpochMilli(), entryReturned.get().getReminderDate().toEpochMilli());
        database.close();
    }

    @Test
    public void updateRecurrence() throws PositionOutOfBoundException {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        Entry entry = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");

        entryManager.updateRecurrence(user.getId(), user.getProfile(), entry.getId(), "m2");
        Optional<Entry> entryReturned = entryManager.getEntry(user.getId(), user.getProfile(), entry.getId());
        Assert.assertTrue(entryReturned.isPresent());
        Assert.assertEquals("Recurrence was not set properly.", "m2", entryReturned.get().getRecurrence());

        database.close();

    }


}
