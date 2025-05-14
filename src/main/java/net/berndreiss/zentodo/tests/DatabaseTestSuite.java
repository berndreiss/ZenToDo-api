package net.berndreiss.zentodo.tests;

import jakarta.persistence.EntityManagerFactory;
import net.berndreiss.zentodo.data.*;

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
        UserTest.class
})
public class DatabaseTestSuite {
    public static Supplier<Database> databaseSupplier;
    public static Database database;
    public static User user;
    public static Profile profile;

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
    }

    public static void prepare() {
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

    }

    public static void cleanup() {
        clearDatabase(databaseSupplier.get());
    }

}