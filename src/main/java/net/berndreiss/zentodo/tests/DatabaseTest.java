package net.berndreiss.zentodo.tests;

import net.berndreiss.zentodo.data.Database;
import net.berndreiss.zentodo.data.Profile;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class DatabaseTest {
    @Before
    public void prepare(){DatabaseTestSuite.prepare();}

    @After
    public void cleanUp(){DatabaseTestSuite.cleanup();}

    @Test
    public void setup() {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        List<Profile> profiles = database.getUserManager().getProfiles(null);
        Assert.assertFalse("Default profile for no user was not created.", profiles.isEmpty());
        Assert.assertEquals("Default profile does not have id 0.", 0, profiles.get(0).getId());
    }
}
