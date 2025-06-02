package net.berndreiss.zentodo.tests;

import jakarta.persistence.criteria.CriteriaBuilder;
import net.berndreiss.zentodo.OperationType;
import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.util.VectorClock;
import net.berndreiss.zentodo.util.ZenMessage;
import net.berndreiss.zentodo.util.ZenServerMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class UserTest {
    static final String mail0 = "adfaefawe@apvfoafap098aadfaafhaweihuaf.asfihuawefiuh000";
    static final String mail1 = "adfaefawe@apvfoafap098aadfaafhaweihuaf.asfihuawefiui111";
    static final String mail2 = "adfaefawe@apvfoafap098aadfaafhaweihuaf.asfihuawefiuj222";
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
        Assert.assertTrue("User still in list of users after deletion.", users.isEmpty());
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

        try{
            userManager.addProfile(42);
            Assert.fail("Profile without user was added.");
        }catch (InvalidActionException _){}
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
        TaskList list = database.getListManager().addList(0, "LIST", null);
        database.getListManager().addUserProfileToList(user.getId(), 0, list.getId());
        entryManager.addNewEntry(user.getId(), 0, "TASK");
        entryManager.addNewEntry(user.getId(), 0, "TASK");

        Assert.assertFalse(entryManager.getEntries(user.getId(), 0).isEmpty());

        userManager.removeProfile(user.getId(), 0);
        Optional<Profile> profile = userManager.getProfile(user.getId(), 0);
        Assert.assertTrue("Profile was not delete.", profile.isEmpty());
        Assert.assertTrue("Entries associated with profile were not deleted.", entryManager.getEntries(user.getId(), 0).isEmpty());
        List<TaskList> lists = database.getListManager().getListsForUser(user.getId(), 0);
        Assert.assertTrue("List associations for profile were not removed.", lists.isEmpty());

        lists = database.getListManager().getLists();
        Assert.assertFalse("List was delete too.", lists.isEmpty());
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
        Assert.assertEquals("Device was not set for user.", 1, userReturned.get().getDevice());
        database.close();

    }

    @Test
    public void setClock() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(1,mail1, null, 0);
        VectorClock clock = new VectorClock();
        userManager.setClock(user.getId(), clock);
        Optional<User> userReturned = userManager.getUser(user.getId());
        Assert.assertTrue(userReturned.isPresent());
        Assert.assertEquals("Clock was not saved properly", clock.jsonify(), userReturned.get().getClock());
        database.close();
    }

    @Test
    public void updateEmail() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(1,mail1, null, 0);
        userManager.updateEmail(user.getId(), mail2);
        Optional<User> userReturned = userManager.getUser(user.getId());
        Assert.assertTrue(userReturned.isPresent());
        Assert.assertEquals("Mail was not updated", mail2, userReturned.get().getEmail());
        userManager.addUser(2, mail1, null, 0);
        try{
            userManager.updateEmail(user.getId(), mail1);
            Assert.fail("Updating user with existing mail did not trigger exception.");
        } catch(InvalidActionException _){}
        database.close();
    }

    @Test
    public void updateUserName() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(1,mail1, null, 0);
        userManager.updateUserName(user.getId(), "NAME");
        Optional<User> userReturned = userManager.getUser(user.getId());
        Assert.assertTrue(userReturned.isPresent());
        Assert.assertEquals("User name was not updated", "NAME", userReturned.get().getUserName());
        database.close();

    }
    @Test
    public void addToQueue() throws DuplicateIdException, InvalidActionException {
        //TODO ADD TEST THAT QUEUE ITEM OF OTHER USERS ARE NOT RETURNED
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        EntryManagerI entryManager = database.getEntryManager();
        User user = userManager.addUser(1,mail1, null, 0);
        List<Object> entries = new ArrayList<>();
        entries.add(entryManager.addNewEntry(user.getId(), user.getProfile(), "TASK0"));
        entries.add(entryManager.addNewEntry(user.getId(), user.getProfile(), "TASK1"));
        ZenServerMessage message = new ZenServerMessage(OperationType.ADD_NEW_ENTRY, entries, new VectorClock(user.getClock()));
        userManager.addToQueue(user, message);
        userManager.addToQueue(user, message);
        List<ZenServerMessage> messages = userManager.getQueued(user.getId());
        Assert.assertEquals(2, messages.size());
        Assert.assertEquals("Queue was not updated properly", message.type, messages.get(0).type);
        Assert.assertEquals("Queue was not updated properly", message.clock.jsonify(), messages.get(0).clock.jsonify());
        Assert.assertEquals("Queue was not updated properly", message.timeStamp.getEpochSecond(), messages.get(0).timeStamp.getEpochSecond());
        Assert.assertEquals("Queue was not updated properly", ((Entry) message.arguments.get(0)).getTask(), "TASK0");
        Assert.assertEquals("Queue was not updated properly", ((Entry) message.arguments.get(1)).getTask(), "TASK1");
        database.close();

    }

    @Test
    public void clearQueue() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        EntryManagerI entryManager = database.getEntryManager();
        User user = userManager.addUser(1,mail1, null, 0);
        List<Object> entries = new ArrayList<>();
        entries.add(entryManager.addNewEntry(user.getId(), user.getProfile(), "TASK0"));
        entries.add(entryManager.addNewEntry(user.getId(), user.getProfile(), "TASK1"));
        ZenServerMessage message = new ZenServerMessage(OperationType.ADD_NEW_ENTRY, entries, new VectorClock(user.getClock()));
        userManager.addToQueue(user, message);
        userManager.clearQueue(user.getId());
        List<ZenServerMessage> messages = userManager.getQueued(user.getId());
        Assert.assertTrue("Queue has not been cleared.", messages.isEmpty());
        database.close();

    }

    @Test
    public void setToken() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user = userManager.addUser(1,mail1, null, 0);
        userManager.setToken(user.getId(),"TOKEN");
        String token = userManager.getToken(user.getId());
        Assert.assertEquals("Returned token was wrong.", "TOKEN", token);
        database.close();

    }
    @Test
    public void getUsers() throws DuplicateIdException, InvalidActionException {
        Database database = DatabaseTestSuite.databaseSupplier.get();
        UserManagerI userManager = database.getUserManager();
        User user0 = userManager.addUser(1,mail1, null, 0);
        User user1 = userManager.addUser(2,mail2, null, 0);
        List<User> users = userManager.getUsers();
        Assert.assertTrue("Default user was returned.", users.stream().noneMatch(u -> u.getId() == 0));
        Assert.assertEquals("Wrong number of users returned.", 2, users.size());
        Assert.assertTrue("User0 was not returned.", users.stream().anyMatch(u -> Objects.equals(u.getId(), user0.getId())));
        Assert.assertTrue("User1 was not returned.", users.stream().anyMatch(u -> Objects.equals(u.getId(), user1.getId())));
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
