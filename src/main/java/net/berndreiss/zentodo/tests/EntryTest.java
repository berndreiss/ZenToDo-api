package net.berndreiss.zentodo.tests;

import net.berndreiss.zentodo.data.Database;
import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.EntryManagerI;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

public class EntryTest {
    @Before
    public void prepare() {
        DatabaseTestSuite.prepare();}

    @After
    public void cleanUp() {
        DatabaseTestSuite.cleanup();}

    @Test
    public void addNewEntry(){
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        Entry entry0 = entryManager.addNewEntry(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");
        Entry entry1 = entryManager.addNewEntry(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile(), "TASK1");

        Optional<Entry> entry0Returned = entryManager.getEntry(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile(), entry0.getId());

        Assert.assertTrue("entry0 should be present", entry0Returned.isPresent());
        Assert.assertEquals("Returned entry has wrong id.", entry0.getId(), entry0Returned.get().getId());
        Assert.assertEquals("Returned entry has wrong user id.", DatabaseTestSuite.user.getId(), entry0Returned.get().getUserId());
        Assert.assertEquals("Returned entry has wrong profile.", DatabaseTestSuite.user.getProfile(), entry0Returned.get().getProfile());
        Assert.assertEquals("Position for first added entry is not 0.", 0, entry0Returned.get().getPosition());
        Assert.assertEquals("Wrong task saved for new entry.", "TASK0", entry0Returned.get().getTask());
        Assert.assertEquals("Focus needs to be 0 for new entries.", 0, entry0Returned.get().getFocus() ? 1 : 0);
        Assert.assertEquals("Dropped needs to be 1 for new entries.", 1, entry0Returned.get().getDropped() ? 1 : 0);
        Assert.assertNull("List needs to be initialized with null.", entry0Returned.get().getList());
        Assert.assertNull("List position needs to be initialized with null.", entry0Returned.get().getListPosition());
        Assert.assertNull("Recurrence needs to be initialized with null.", entry0Returned.get().getRecurrence());
        Assert.assertNull("Reminder date needs to be initialized with null.", entry0Returned.get().getReminderDate());

        Optional<Entry> entry1Returned = entryManager.getEntry(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile(), entry1.getId());
        Assert.assertTrue("entry1 should be present", entry1Returned.isPresent());
        Assert.assertEquals(
                entry1.getPosition() == 0 ? "Positions for added entries is not incremented." : "Position for added entry is arbitrary.",
                1, entry1Returned.get().getPosition()
        );

        Entry entry2 = entryManager.addNewEntry(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2", 1);
        Optional<Entry> entry2Returned = entryManager.getEntry(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile(),  entry2.getId());
        Assert.assertTrue("entry2 should be present", entry2Returned.isPresent());
        Assert.assertEquals("Returned entry has wrong id for custom position.", entry2.getId(), entry2Returned.get().getId());
        Assert.assertEquals("Returned entry has wrong user id for custom position.", DatabaseTestSuite.user.getId(), entry2Returned.get().getUserId());
        Assert.assertEquals("Returned entry has wrong profile for custom position.", DatabaseTestSuite.user.getProfile(), entry2Returned.get().getProfile());
        Assert.assertEquals("Wrong task saved for new entry for custom position.", "TASK2", entry2Returned.get().getTask());
        Assert.assertEquals("Focus needs to be 0 for new entries for custom position.", 0, entry2Returned.get().getFocus() ? 1 : 0);
        Assert.assertEquals("Dropped needs to be 1 for new entries for custom position.", 1, entry2Returned.get().getDropped() ? 1 : 0);
        Assert.assertNull("List needs to be initialized with null for custom position.", entry2Returned.get().getList());
        Assert.assertNull("List position needs to be initialized with null for custom position.", entry2Returned.get().getListPosition());
        Assert.assertNull("Recurrence needs to be initialized with null for custom position.", entry2Returned.get().getRecurrence());
        Assert.assertNull("Reminder date needs to be initialized with null for custom position.", entry2Returned.get().getReminderDate());
        Assert.assertEquals("New entry was not assigned custom position.", 1, entry2Returned.get().getPosition());

        entry0Returned = entryManager.getEntry(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile(), entry0.getId());
        Assert.assertTrue("entry0 should still be present", entry0Returned.isPresent());
        Assert.assertEquals("Adding entry with custom position must not modify positions of entries before it.", 0, entry0Returned.get().getPosition());

        entry1Returned = entryManager.getEntry(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile(), entry1.getId());
        Assert.assertTrue("entry1 should still be present", entry1Returned.isPresent());
        Assert.assertEquals("Adding entry with custom position needs to adjust the position of entries after it.", 2, entry1Returned.get().getPosition());

        Entry entry3 = entryManager.addNewEntry(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile(), 42, "TASK3", 3);
        Optional<Entry> entry3Returned = entryManager.getEntry(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile(), entry3.getId());
        Assert.assertTrue("entry3 should be present", entry3Returned.isPresent());
        Assert.assertEquals("Returned entry has wrong id for custom id.", 42, entry3Returned.get().getId());
        Assert.assertEquals("Returned entry has wrong user id for custom id.", DatabaseTestSuite.user.getId(), entry3Returned.get().getUserId());
        Assert.assertEquals("Returned entry has wrong profile for custom id.", DatabaseTestSuite.user.getProfile(), entry3Returned.get().getProfile());
        Assert.assertEquals("Wrong task saved for new entry for custom id.", "TASK3", entry3Returned.get().getTask());
        Assert.assertEquals("Focus needs to be 0 for new entries for custom id.", 0, entry3Returned.get().getFocus() ? 1 : 0);
        Assert.assertEquals("Dropped needs to be 1 for new entries for custom id.", 1, entry3Returned.get().getDropped() ? 1 : 0);
        Assert.assertNull("List needs to be initialized with null for custom id.", entry3Returned.get().getList());
        Assert.assertNull("List position needs to be initialized with null for custom id.", entry3Returned.get().getListPosition());
        Assert.assertNull("Recurrence needs to be initialized with null for custom id.", entry3Returned.get().getRecurrence());
        Assert.assertNull("Reminder date needs to be initialized with null for custom id.", entry3Returned.get().getReminderDate());
        Assert.assertEquals("New entry was not assigned right position for custom id.", 3, entry3Returned.get().getPosition());

    }

    @Test
    public void getEntries(){
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        entryManager.getEntries(DatabaseTestSuite.user.getId(), DatabaseTestSuite.user.getProfile());
    }
}
