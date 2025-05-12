package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.data.Database;

import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.User;
import net.berndreiss.zentodo.data.Profile;
import org.junit.*;

import java.util.List;
import java.util.Optional;


public abstract class DatabaseTest {
    protected abstract Database createDatabase();
    private User user;

    public static void clearDatabase(Database database){
        for (User u: database.getUsers()) {
            for (Profile p : database.getProfiles(u.getId())) {
                for (Entry e : database.getEntries(u.getId(), p.getId()))
                    database.removeEntry(e.getUserId(), p.getId(), e.getId());
                Assert.assertTrue(database.getEntries(u.getId(), p.getId()).isEmpty());
                database.removeProfile(p.getId());
            }
            database.removeUser(u.getId());
        }
        for (Profile p: database.getProfiles(null)){
            for (Entry e: database.getEntries(null, p.getId()))
                database.removeEntry(null, p.getId(), e.getId());
            database.removeProfile(p.getId());
        }

        for (Entry e: database.getEntries(null, 0L))
            database.removeEntry(null,0L, e.getId());
        Assert.assertTrue(database.getUsers().isEmpty());

    }
    @Before
    public void prepare(){
        Database database = createDatabase();
        clearDatabase(database);
        user = database.addUser(0, "test@gmail.com", null, 0);

    }
    @After
    public void cleanup(){
        clearDatabase(createDatabase());
    }

    @Test
    public void setup(){
        Database database = createDatabase();
        List<Profile> profiles = database.getProfiles(null);
        Assert.assertFalse("Default profile for no user was not created.", profiles.isEmpty());
        Assert.assertEquals("Default profile does not have id 0.", 0, profiles.get(0).getId());
    }
    @Test
    public void addUser(){
        Database database = createDatabase();
        User user = database.addUser(1, "testAdd@gmail.com", "TEST", 2);
        Assert.assertEquals("Users profile id is not 0.", 0, user.getProfile());
        Optional<User> userReturned = database.getUserByEmail(user.getEmail());
        Assert.assertTrue("User was not created.", userReturned.isPresent());
        Assert.assertEquals("User was created with wrong ID.", userReturned.get().getId(), user.getId());
        Assert.assertEquals("User was created with wrong email.", userReturned.get().getEmail(), user.getEmail());
        Assert.assertEquals("User was created with wrong username.", userReturned.get().getUserName(), user.getUserName());
        Assert.assertEquals("User was created with wrong device.", userReturned.get().getDevice(), user.getDevice());
        Assert.assertEquals("User was created with wrong profile.", userReturned.get().getProfile(), user.getProfile());
        Assert.assertFalse("User was automatically enabled.", userReturned.get().isEnabled());

        Optional<Profile> profile = database.getProfile(user.getId(), user.getProfile());

        Assert.assertTrue("Default profile for user was not created.", profile.isPresent());
        Assert.assertEquals("Default profile does not have id 0.", 0, profile.get().getId());

        User userNull = database.addUser(2, "testNull@gmail.com", null, 3);
        Optional<User> userNullReturned = database.getUserByEmail(userNull.getEmail());
        Assert.assertTrue("User was not created for userName == null.", userNullReturned.isPresent());
        Assert.assertEquals("User was created with wrong ID for userName == null.", userNullReturned.get().getId(), userNull.getId());
        Assert.assertEquals("User was created with wrong email for userName == null.", userNullReturned.get().getEmail(), userNull.getEmail());
        Assert.assertEquals("User was created with wrong username for userName == null.", userNullReturned.get().getUserName(), userNull.getUserName());
        Assert.assertEquals("User was created with wrong device for userName == null.", userNullReturned.get().getDevice(), userNull.getDevice());
        Assert.assertEquals("User was created with wrong profile.", userNullReturned.get().getProfile(), userNull.getProfile());
        Assert.assertFalse("User was automatically enabled.", userNullReturned.get().isEnabled());

        Optional<Profile> profileNull = database.getProfile(userNull.getId(), userNull.getProfile());
        Assert.assertTrue("Default profile for user was not created for userName == null.", profileNull.isPresent());
    }

    @Test
    public void addNewEntry(){
        Database database = createDatabase();
        Entry entry0 = database.addNewEntry(user.getId(), user.getProfile(), "TASK0");
        Entry entry1 = database.addNewEntry(user.getId(), user.getProfile(), "TASK1");

        Optional<Entry> entry0Returned = database.getEntry(user.getId(), user.getProfile(), entry0.getId());

        Assert.assertTrue("entry0 should be present", entry0Returned.isPresent());
        Assert.assertEquals("Returned entry has wrong id.", entry0.getId(), entry0Returned.get().getId());
        Assert.assertEquals("Returned entry has wrong user id.", user.getId(), entry0Returned.get().getUserId());
        Assert.assertEquals("Returned entry has wrong profile.", user.getProfile(), entry0Returned.get().getProfile());
        Assert.assertEquals("Position for first added entry is not 0.", 0, entry0Returned.get().getPosition());
        Assert.assertEquals("Wrong task saved for new entry.", "TASK0", entry0Returned.get().getTask());
        Assert.assertEquals("Focus needs to be 0 for new entries.", 0, entry0Returned.get().getFocus() ? 1 : 0);
        Assert.assertEquals("Dropped needs to be 1 for new entries.", 1, entry0Returned.get().getDropped() ? 1 : 0);
        Assert.assertNull("List needs to be initialized with null.", entry0Returned.get().getList());
        Assert.assertNull("List position needs to be initialized with null.", entry0Returned.get().getListPosition());
        Assert.assertNull("Recurrence needs to be initialized with null.", entry0Returned.get().getRecurrence());
        Assert.assertNull("Reminder date needs to be initialized with null.", entry0Returned.get().getReminderDate());

        Optional<Entry> entry1Returned = database.getEntry(user.getId(), user.getProfile(), entry1.getId());
        Assert.assertTrue("entry1 should be present", entry1Returned.isPresent());
        Assert.assertEquals(
                entry1.getPosition() == 0 ? "Positions for added entries is not incremented." : "Position for added entry is arbitrary.",
                1, entry1Returned.get().getPosition()
        );

        Entry entry2 = database.addNewEntry(user.getId(), user.getProfile(), "TASK2", 1);
        Optional<Entry> entry2Returned = database.getEntry(user.getId(), user.getProfile(),  entry2.getId());
        Assert.assertTrue("entry2 should be present", entry2Returned.isPresent());
        Assert.assertEquals("Returned entry has wrong id for custom position.", entry2.getId(), entry2Returned.get().getId());
        Assert.assertEquals("Returned entry has wrong user id for custom position.", user.getId(), entry2Returned.get().getUserId());
        Assert.assertEquals("Returned entry has wrong profile for custom position.", user.getProfile(), entry2Returned.get().getProfile());
        Assert.assertEquals("Wrong task saved for new entry for custom position.", "TASK2", entry2Returned.get().getTask());
        Assert.assertEquals("Focus needs to be 0 for new entries for custom position.", 0, entry2Returned.get().getFocus() ? 1 : 0);
        Assert.assertEquals("Dropped needs to be 1 for new entries for custom position.", 1, entry2Returned.get().getDropped() ? 1 : 0);
        Assert.assertNull("List needs to be initialized with null for custom position.", entry2Returned.get().getList());
        Assert.assertNull("List position needs to be initialized with null for custom position.", entry2Returned.get().getListPosition());
        Assert.assertNull("Recurrence needs to be initialized with null for custom position.", entry2Returned.get().getRecurrence());
        Assert.assertNull("Reminder date needs to be initialized with null for custom position.", entry2Returned.get().getReminderDate());
        Assert.assertEquals("New entry was not assigned custom position.", 1, entry2Returned.get().getPosition());

        entry0Returned = database.getEntry(user.getId(), user.getProfile(), entry0.getId());
        Assert.assertTrue("entry0 should still be present", entry0Returned.isPresent());
        Assert.assertEquals("Adding entry with custom position must not modify positions of entries before it.", 0, entry0Returned.get().getPosition());

        entry1Returned = database.getEntry(user.getId(), user.getProfile(), entry1.getId());
        Assert.assertTrue("entry1 should still be present", entry1Returned.isPresent());
        Assert.assertEquals("Adding entry with custom position needs to adjust the position of entries after it.", 2, entry1Returned.get().getPosition());

        Entry entry3 = database.addNewEntry(user.getId(), user.getProfile(), 42, "TASK3", 3);
        Optional<Entry> entry3Returned = database.getEntry(user.getId(), user.getProfile(), entry3.getId());
        Assert.assertTrue("entry3 should be present", entry3Returned.isPresent());
        Assert.assertEquals("Returned entry has wrong id for custom id.", 42, entry3Returned.get().getId());
        Assert.assertEquals("Returned entry has wrong user id for custom id.", user.getId(), entry3Returned.get().getUserId());
        Assert.assertEquals("Returned entry has wrong profile for custom id.", user.getProfile(), entry3Returned.get().getProfile());
        Assert.assertEquals("Wrong task saved for new entry for custom id.", "TASK3", entry3Returned.get().getTask());
        Assert.assertEquals("Focus needs to be 0 for new entries for custom id.", 0, entry3Returned.get().getFocus() ? 1 : 0);
        Assert.assertEquals("Dropped needs to be 1 for new entries for custom id.", 1, entry3Returned.get().getDropped() ? 1 : 0);
        Assert.assertNull("List needs to be initialized with null for custom id.", entry3Returned.get().getList());
        Assert.assertNull("List position needs to be initialized with null for custom id.", entry3Returned.get().getListPosition());
        Assert.assertNull("Recurrence needs to be initialized with null for custom id.", entry3Returned.get().getRecurrence());
        Assert.assertNull("Reminder date needs to be initialized with null for custom id.", entry3Returned.get().getReminderDate());
        Assert.assertEquals("New entry was not assigned right position for custom id.", 3, entry3Returned.get().getPosition());

        Entry entryNoUser = database.addNewEntry(null, 0L, "NULL");
        Assert.assertNull("User id is not null for entry without user.", entryNoUser.getUserId());
        Optional<Entry> entryNoUserReturned = database.getEntry(null, 0L, entryNoUser.getId());
        Assert.assertTrue("Entry without user was not added.", entryNoUserReturned.isPresent());
        Assert.assertNull("User id is not null for entry without user.", entryNoUserReturned.get().getUserId());
    }

    @Test
    public void getEntries(){
        Database database = createDatabase();
        database.getEntries(user.getId(), user.getProfile());
    }
}
