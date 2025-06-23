package net.berndreiss.zentodo.tests;

import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.exceptions.InvalidActionException;
import net.berndreiss.zentodo.exceptions.InvalidUserActionException;
import net.berndreiss.zentodo.util.ClientStub;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;


/**
 * A metaclass including all other test classes for the API.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        DatabaseTest.class,
        TaskManagerTests.class,
        UserTest.class,
        ListManagerTests.class
})
public class DatabaseTestSuite {
    /**
     * The database to run the tests against
     */
    public static Supplier<Database> databaseSupplier;
    /**
     * The default user for the tests
     */
    public static User user;
    /**
     * The default profile for the tests
     */
    public static Profile profile;

    /**
     * Remove all data from the database -> clears users, tasks, and lists.
     *
     * @param database the database to clear
     */
    public static void clearDatabase(Database database) {
        UserManagerI userManager = database.getUserManager();
        TaskManagerI taskManager = database.getTaskManager();
        ListManagerI listManager = database.getListManager();
        for (TaskList l : listManager.getLists())
            listManager.removeList(l.getId());
        for (User u : userManager.getUsers()) {
            userManager.clearQueue(u.getId());
            if (u.getId() == 0) {
                continue;
            }
            for (Profile p : userManager.getProfiles(u.getId())) {
                for (Task e : taskManager.getTasks(u.getId(), p.getId()))
                    taskManager.removeTask(e.getUserId(), p.getId(), e.getId());
                Assert.assertTrue(taskManager.getTasks(u.getId(), p.getId()).isEmpty());
                try {
                    userManager.removeProfile(u.getId(), p.getId());
                } catch (InvalidActionException _) {
                }

            }
            try {
                userManager.removeUser(u.getId());
            } catch (InvalidUserActionException e) {
                ClientStub.logger.error("Problem removing user when clearing database.", e);
                throw new RuntimeException(e);
            }
        }

        for (Profile p : userManager.getProfiles(0)) {
            List<Task> tasks = taskManager.getTasks(0, p.getId());
            for (Task t : tasks)
                taskManager.removeTask(0, 0, t.getId());
            if (p.getId() != 0) {
                try {
                    userManager.removeProfile(0, p.getId());
                } catch (InvalidUserActionException e) {
                    ClientStub.logger.error("Problem removing profile when clearing database.", e);
                    throw new RuntimeException(e);
                }
            }
        }
        database.close();
    }

    /**
     * Prepare for tests -> clear the database and assert default user and profile exist,
     */
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
        database.close();
    }

    /**
     * Clear the database.
     */
    public static void cleanup() {
        clearDatabase(databaseSupplier.get());
    }

}