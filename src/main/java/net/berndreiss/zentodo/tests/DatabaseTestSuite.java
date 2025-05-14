package net.berndreiss.zentodo.tests;

import net.berndreiss.zentodo.data.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.util.function.Supplier;


@RunWith(Suite.class)
@Suite.SuiteClasses({
        DatabaseTest.class,
        EntryTest.class,
        UserTest.class
})
public class DatabaseTestSuite {
    public static Supplier<Database> databaseSupplier;
    public static User user;

    public static void clearDatabase(Database database) {
        UserManagerI userManager = database.getUserManager();
        EntryManagerI entryManager = database.getEntryManager();
        for (User u : userManager.getUsers()) {
            for (Profile p : userManager.getProfiles(u.getId())) {
                for (Entry e : entryManager.getEntries(u.getId(), p.getId()))
                    entryManager.removeEntry(e.getUserId(), p.getId(), e.getId());
                Assert.assertTrue(entryManager.getEntries(u.getId(), p.getId()).isEmpty());
                userManager.removeProfile(p.getId());
            }
            userManager.removeUser(u.getId());
        }
        for (Profile p : userManager.getProfiles(null)) {
            for (Entry e : entryManager.getEntries(null, p.getId()))
                entryManager.removeEntry(null, p.getId(), e.getId());
            userManager.removeProfile(p.getId());
        }

        for (Entry e : entryManager.getEntries(null, 0L))
            entryManager.removeEntry(null, 0L, e.getId());
        Assert.assertTrue(userManager.getUsers().isEmpty());

    }

    public static void prepare() {
        Database database = databaseSupplier.get();
        clearDatabase(database);
        user = database.getUserManager().addUser(0, "test@gmail.com", null, 0);

    }

    public static void cleanup() {
        clearDatabase(databaseSupplier.get());
    }

}