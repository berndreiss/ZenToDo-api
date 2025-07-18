package net.berndreiss.zentodo.tests;

import net.berndreiss.zentodo.data.Database;
import net.berndreiss.zentodo.data.Profile;
import net.berndreiss.zentodo.data.User;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

/**
 * Tests for basic database invariants.
 */
public class DatabaseTest {
    @Before
    public void prepare() {
        DatabaseTestSuite.prepare();
    }

    @After
    public void cleanUp() {
        DatabaseTestSuite.cleanup();
    }

    /**
     * Test whether default user (id==0) and profile where created and assigned properly.
     */
    @Test
    public void setup() {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        Optional<User> user = database.getUserManager().getUser(0L);
        List<Profile> profiles = database.getUserManager().getProfiles(0L);
        Assert.assertTrue("Default user was not created.", user.isPresent());
        Assert.assertFalse("Default profile for default user was not created.", profiles.isEmpty());
        Assert.assertEquals("Default profile was not assigned to user.", profiles.get(0).getProfileId().getId(), user.get().getProfile());
        database.close();
    }
}
