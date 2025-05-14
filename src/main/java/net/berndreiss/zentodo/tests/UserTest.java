package net.berndreiss.zentodo.tests;

import net.berndreiss.zentodo.data.Database;
import net.berndreiss.zentodo.data.Profile;
import net.berndreiss.zentodo.data.User;
import net.berndreiss.zentodo.data.UserManagerI;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

public class UserTest {
    @Before
    public void prepare() {
        DatabaseTestSuite.prepare();}

    @After
    public void cleanUp() {
        DatabaseTestSuite.cleanup();}

    @Test
    public void addUser(){
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = database.getUserManager().addUser(1, "testAdd@gmail.com", "TEST", 2);
        Assert.assertEquals("Users profile id is not 0.", 0, user.getProfile());
        Optional<User> userReturned = userManager.getUserByEmail(user.getEmail());
        Assert.assertTrue("User was not created.", userReturned.isPresent());
        Assert.assertEquals("User was created with wrong ID.", userReturned.get().getId(), user.getId());
        Assert.assertEquals("User was created with wrong email.", userReturned.get().getEmail(), user.getEmail());
        Assert.assertEquals("User was created with wrong username.", userReturned.get().getUserName(), user.getUserName());
        Assert.assertEquals("User was created with wrong device.", userReturned.get().getDevice(), user.getDevice());
        Assert.assertEquals("User was created with wrong profile.", userReturned.get().getProfile(), user.getProfile());
        Assert.assertFalse("User was automatically enabled.", userReturned.get().isEnabled());

        Optional<Profile> profile = userManager.getProfile(user.getId(), user.getProfile());

        Assert.assertTrue("Default profile for user was not created.", profile.isPresent());
        Assert.assertEquals("Default profile does not have id 0.", 0, profile.get().getId());

        User userNull = userManager.addUser(2, "testNull@gmail.com", null, 3);
        Optional<User> userNullReturned = userManager.getUserByEmail(userNull.getEmail());
        Assert.assertTrue("User was not created for userName == null.", userNullReturned.isPresent());
        Assert.assertEquals("User was created with wrong ID for userName == null.", userNullReturned.get().getId(), userNull.getId());
        Assert.assertEquals("User was created with wrong email for userName == null.", userNullReturned.get().getEmail(), userNull.getEmail());
        Assert.assertEquals("User was created with wrong username for userName == null.", userNullReturned.get().getUserName(), userNull.getUserName());
        Assert.assertEquals("User was created with wrong device for userName == null.", userNullReturned.get().getDevice(), userNull.getDevice());
        Assert.assertEquals("User was created with wrong profile.", userNullReturned.get().getProfile(), userNull.getProfile());
        Assert.assertFalse("User was automatically enabled.", userNullReturned.get().isEnabled());

        Optional<Profile> profileNull = userManager.getProfile(userNull.getId(), userNull.getProfile());
        Assert.assertTrue("Default profile for user was not created for userName == null.", profileNull.isPresent());
    }
}
