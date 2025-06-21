package net.berndreiss.zentodo.tests;

import net.berndreiss.zentodo.data.*;

import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.Profile;
import net.berndreiss.zentodo.data.TaskList;
import net.berndreiss.zentodo.data.User;
import net.berndreiss.zentodo.exceptions.InvalidActionException;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;


@RunWith(Suite.class)
@Suite.SuiteClasses({
        DatabaseTest.class,
        EntryTest.class,
        UserTest.class,
        ListTest.class
})
public class DatabaseTestSuite {
    public static Supplier<Database> databaseSupplier;
    public static User user;
    public static Profile profile;

    public static void clearDatabase(Database database) throws InvalidActionException {
        UserManagerI userManager = database.getUserManager();
        EntryManagerI entryManager = database.getEntryManager();
        ListManagerI listManager = database.getListManager();
        for (TaskList l: listManager.getLists())
            listManager.removeList(l.getId());
        for (User u : userManager.getUsers()) {
            userManager.clearQueue(u.getId());
            if (u.getId() == 0) {
                continue;
            }
            for (Profile p : userManager.getProfiles(u.getId())) {
                for (Entry e : entryManager.getEntries(u.getId(), p.getId()))
                    entryManager.removeEntry(e.getUserId(), p.getId(), e.getId());
                Assert.assertTrue(entryManager.getEntries(u.getId(), p.getId()).isEmpty());
                try {
                    userManager.removeProfile(u.getId(), p.getId());
                } catch (InvalidActionException _){}

            }
            userManager.removeUser(u.getId());
        }

        for (Profile p: userManager.getProfiles(0)) {
            List<Entry> entries = entryManager.getEntries(0, p.getId());
            for (Entry e : entries)
                entryManager.removeEntry(0, 0, e.getId());
            if (p.getId() != 0)
                userManager.removeProfile(0, p.getId());
        }
        database.close();
    }

    public static void prepare() throws InvalidActionException {
        Database database = databaseSupplier.get();
        clearDatabase(database);
        database = databaseSupplier.get();
        Optional<User> userOpt = database.getUserManager().getUser(0L);
        Assert.assertFalse("PREPARE: Default user was not created.", userOpt.isEmpty());
        user = userOpt.get();
        List<Profile> profiles = database.getUserManager().getProfiles(0L);
        Assert.assertFalse("PREPARE: Default profile for default user was not created.", profiles.isEmpty());
        Assert.assertEquals("PREPARE: Default profile was not assigned to user.", profiles.getFirst().getProfileId().getId(), user.getProfile());
        profile = profiles.getFirst();
        database.close();

    }

    public static void cleanup() throws InvalidActionException {
        clearDatabase(databaseSupplier.get());
    }

}