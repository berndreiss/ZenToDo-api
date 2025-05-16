package net.berndreiss.zentodo.tests;

import net.berndreiss.zentodo.data.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
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
    public void updateList() throws PositionOutOfBoundException {

        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        ListManagerI listManager = database.getListManager();
        Entry entry0 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");
        Entry entry1 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK1");
        Entry entry2 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2");

        database.close();

    }

    @Test
    public void getList() throws PositionOutOfBoundException {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        ListManagerI listManager = database.getListManager();
        Entry entry0 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");
        Entry entry1 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK1");
        Entry entry2 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2");

        database.close();

    }

    @Test
    public void swapListEntries() throws PositionOutOfBoundException {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        ListManagerI listManager = database.getListManager();
        Entry entry0 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");
        Entry entry1 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK1");
        Entry entry2 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2");

        database.close();


    }

    @Test
    public void updateListColor() throws PositionOutOfBoundException {
        User user = DatabaseTestSuite.user;
        Database database = DatabaseTestSuite.databaseSupplier.get();
        EntryManagerI entryManager = database.getEntryManager();
        ListManagerI listManager = database.getListManager();
        Entry entry0 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK0");
        Entry entry1 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK1");
        Entry entry2 = entryManager.addNewEntry(user.getId(), DatabaseTestSuite.user.getProfile(), "TASK2");

        database.close();


    }
}