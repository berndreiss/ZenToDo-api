package net.berndreiss.zentodo.tests;

import jakarta.persistence.criteria.CriteriaBuilder;
import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.util.VectorClock;
import net.berndreiss.zentodo.util.ZenServerMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

public class UserTest {
    static final String mail0 = "adfaefawe@apvfoafap098aadfaafhaweihuaf.asfihuawefiuh";
    static final String mail1 = "adfaefawe@apvfoafap098aadfaafhaweihuaf.asfihuawefiui";
    static final String mail2 = "adfaefawe@apvfoafap098aadfaafhaweihuaf.asfihuawefiui";
    @Before
    public void prepare() throws InvalidActionException {
        DatabaseTestSuite.prepare();}

    @After
    public void cleanUp() throws InvalidActionException {
        DatabaseTestSuite.cleanup();}

    @Test
    public void addUser() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = database.getUserManager().addUser(1, mail0, "TEST", 2);
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

        User userNull = userManager.addUser(2, mail1, null, 3);
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
        try {
            userManager.addUser(2, mail2, null, 3);
            Assert.fail("Add user did not throw exception for duplicate id.");
        } catch (DuplicateIdException _){}
        try {
            userManager.addUser(5, mail1, null, 3);
            Assert.fail("Add user did not throw exception for duplicate mail.");
        } catch (InvalidActionException | DuplicateIdException _){}
    }

    @Test
    public void removeUser() throws DuplicateIdException, InvalidActionException {

        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();

        User user = userManager.addUser(1, mail1, null, 0);

        userManager.addProfile(0);
        userManager.addProfile(1);
        userManager.addProfile(1);

        userManager.removeUser(user.getId());

        List<User> users = userManager.getUsers();
        Assert.assertEquals((users.size() > 1 ? "User still in list of users after deletion." : "Default user has been deleted as well."), 1, users.size());
        Optional<User> userReturned = userManager.getUser(1);
        Assert.assertTrue("User was not removed.", userReturned.isEmpty());
        List<Profile> profiles1 = userManager.getProfiles(1);
        Assert.assertTrue("Profiles have not been removed for user.", profiles1.isEmpty());

        try{
            userManager.removeUser(0);
            Assert.fail("No exceptions has been thrown when trying to remove the default user.");
        } catch(InvalidActionException _){}
        database.close();
    }
    @Test
    public void addProfile() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(1,mail1, null, 0);
        Profile profile = userManager.addProfile(user.getId(), "Profile");
        Optional<Profile> profileReturned = userManager.getProfile(user.getId(), profile.getId());
        Assert.assertTrue("Profile was not added.", profileReturned.isPresent());
        Assert.assertEquals("Profile name was not saved properly.", "Profile", profileReturned.get().getName());
        profile = userManager.addProfile(user.getId());
        profileReturned = userManager.getProfile(user.getId(), profile.getId());
        Assert.assertTrue("Profile without name was not added.", profileReturned.isPresent());
        Assert.assertNull("Profile name for profile without name is not null.", profileReturned.get().getName());
        database.close();
    }
    @Test
    public void removeProfile() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        EntryManagerI entryManager = database.getEntryManager();
        User user = userManager.addUser(1,mail1, null, 0);
        userManager.addProfile(0);
        Profile profile1 = userManager.addProfile(user.getId());

        entryManager.addNewEntry(user.getId(), 0, "TASK");
        entryManager.addNewEntry(user.getId(), 0, "TASK");

        Assert.assertFalse(entryManager.getEntries(user.getId(), 0).isEmpty());

        userManager.removeProfile(user.getId(), 0);
        Optional<Profile> profile = userManager.getProfile(user.getId(), 0);
        Assert.assertTrue("Profile was not delete.", profile.isEmpty());

        Assert.assertTrue("Entries associated with profile were not deleted.", entryManager.getEntries(user.getId(), 0).isEmpty());

        try{
            userManager.removeProfile(0,0);
            Assert.fail("Deleting the default profile not triggering InvalidActionException");
        } catch (InvalidActionException _){}
        try{
            userManager.removeProfile(user.getId(), profile1.getId());
            Assert.fail("Deleting the last profile for user did not trigger InvalidActionException");
        }catch(InvalidActionException _){}
        database.close();

    }
@Test
    public void getUserByEmail() throws DuplicateIdException, InvalidActionException {
    Database database = DatabaseTestSuite.databaseSupplier.get();
    UserManagerI userManager = database.getUserManager();
    User user = userManager.addUser(1,mail1, null, 0);
    Optional<User> userReturned = userManager.getUserByEmail(mail1);
    Assert.assertTrue("User was not retrieved by mail.", userReturned.isPresent());
    Assert.assertEquals("User retrieved by mail has wrong id.", user.getId(), userReturned.get().getId());
    database.close();

    }

@Test
    public void enableUser() throws DuplicateIdException, InvalidActionException {
    Database database = DatabaseTestSuite.databaseSupplier.get();
    UserManagerI userManager = database.getUserManager();
    User user = userManager.addUser(1,mail1, null, 0);
    userManager.enableUser(user.getId());

    Optional<User> userReturned = userManager.getUser(user.getId());
    Assert.assertTrue(userReturned.isPresent());
    Assert.assertTrue("User was not enabled", userReturned.get().getEnabled());
    database.close();
    }

    @Test
    public void setDevice() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(1,mail1, null, 0);
        userManager.setDevice(user.getId(), 1);
        Optional<User> userReturned = userManager.getUser(user.getId());
        Assert.assertTrue(userReturned.isPresent());
        Assert.assertEquals("Device was not set for user.", 1, user.getDevice());
        database.close();

    }

    @Test
    public void setClock() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(1,mail1, null, 0);
        database.close();

    }

    @Test
    public void updateEmail() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(1,mail1, null, 0);
        database.close();

    }

    @Test
    public void updateUserName() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(1,mail1, null, 0);
        database.close();

    }
    @Test
    public void addToQueue() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(1,mail1, null, 0);
        database.close();

    }

    @Test
    public void clearQueue() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(1,mail1, null, 0);
        database.close();

    }

    @Test
    public void setToken() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(1,mail1, null, 0);
        database.close();

    }
    @Test
    public void getUsers() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(1,mail1, null, 0);
        database.close();

    }
    @Test
    public void getProfiles() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(1,mail1, null, 0);
        Profile profile0 = userManager.addProfile(user.getId());
        Profile profile1 = userManager.addProfile(user.getId());
        userManager.addProfile(0);
        List<Profile> profiles = userManager.getProfiles(user.getId());
        Assert.assertEquals("Get profiles returned the wrong number of profiles.", 3, profiles.size());
        Assert.assertFalse("Profiles from default user where included in the returned list.", profiles.stream().map(Profile::getUserId).anyMatch(l -> l==0));
        Assert.assertTrue("Profile missing from list gotten.", profiles.stream().map(Profile::getId).anyMatch(i -> i == 0));
        Assert.assertTrue("Profile missing from list gotten.", profiles.stream().map(Profile::getId).anyMatch(i -> i ==profile0.getId()));
        Assert.assertTrue("Profile missing from list gotten.", profiles.stream().map(Profile::getId).anyMatch(i -> i ==profile1.getId()));
        database.close();

    }
}
