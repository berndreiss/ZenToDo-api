package net.berndreiss.zentodo.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import net.berndreiss.zentodo.data.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Optional;

/**
 * TODO DESCRIBE
 */
public class TestDbHandler implements Database {
    public String tokenPath;
    String token;
    public String persistenceUnit;

    /**
     * TODO DESCRIBE
     * @param persistenceUnit
     */
    public TestDbHandler (String persistenceUnit){
        this.persistenceUnit = persistenceUnit;
        addProfile(null);
    }

    public void close(){
    }

    @Override
    public void post(List<Entry> entries) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (Entry e : entries) {
            em.merge(e);
        }

        em.getTransaction().commit();
        em.close();
        emf.close();

    }

    @Override
    public Optional<Entry> getEntry(Long userId, long profile, long id) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        Optional<Entry> entry;
        if (userId == null){
            entry = em.createQuery("SELECT e FROM Entry e WHERE e.userId IS NULL AND profile = :profile AND e.id = :id", Entry.class)
                    .setParameter("profile", profile)
                    .setParameter("id", id)
                    .getResultStream().findFirst();
        } else {
            entry = em.createQuery("SELECT e FROM Entry e WHERE e.userId = :userId  AND profile = :profile AND e.id = :id", Entry.class)
                    .setParameter("userId", userId)
                    .setParameter("profile", profile)
                    .setParameter("id", id)
                    .getResultStream().findFirst();
        }
        em.close();
        emf.close();
        return entry;
    }

    @Override
    public List<Entry> getEntries(Long userId, long profile) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        List<Entry> list;
        if (userId == null){
            list = em.createQuery("SELECT e FROM Entry e WHERE e.userId IS NULL AND e.profile = :profile", Entry.class)
                    .setParameter("profile", profile)
                    .getResultList();
        } else {
            list = em.createQuery("SELECT e FROM Entry e WHERE e.userId = :userId AND e.profile = :profile", Entry.class)
                    .setParameter("userId", userId)
                    .setParameter("profile", profile)
                    .getResultList();
        }
        em.close();
        emf.close();
        return  list;
    }

    @Override
    public Entry addNewEntry (Long userId, long profile, String task) {
        long position = 0;

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();

        if (userId == null){
            position = em.createQuery("SELECT COUNT (e) FROM Entry e WHERE e.userId IS NULL AND e.profile = :profile", Long.class)
                    .setParameter("profile", profile)
                    .getSingleResult();

        } else {
            position = em.createQuery("SELECT COUNT (e) FROM Entry e WHERE e.userId = :userId AND e.profile = :profile", Long.class)
                    .setParameter("userId", userId)
                    .setParameter("profile", profile)
                    .getSingleResult();
        }
        em.close();
        emf.close();
        return addNewEntry(userId, profile, task, (int) position);
    }
    @Override
    public Entry addNewEntry (Long userId, long profile, String task,int position) {

        long id;
        Random random = new Random();
        boolean idExists;
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        do {
            id = random.nextLong();
            Long count;
            if (userId == null){
                count = em.createQuery(
                                "SELECT COUNT(e) FROM Entry e WHERE e.userId IS NULL AND e.profile = :profile AND e.id = :id", Long.class)
                        .setParameter("profile", profile)
                        .setParameter("id", id)
                        .getSingleResult();
            }else {
                count = em.createQuery(
                                "SELECT COUNT(e) FROM Entry e WHERE e.userId = :userId AND e.profile = :profile AND e.id = :id", Long.class)
                        .setParameter("userId", userId)
                        .setParameter("profile", profile)
                        .setParameter("id", id)
                        .getSingleResult();
            }
            idExists = count > 0;

        }
        while (idExists);
        em.close();
        emf.close();
        return addNewEntry(userId, profile, id, task, position);
    }


    @Override
    public Entry addNewEntry(Long userId, long profile, long id, String task, int position) {

        Entry entry = new Entry(userId, profile, id, task, position);
        System.out.println(entry.getUserId());
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        if (userId == null){
            em.createQuery("UPDATE Entry e SET e.position = e.position + 1 WHERE e.position >= :pos AND e.userId IS NULL AND e.profile = :profile")
                    .setParameter("pos", position)
                    .setParameter("profile", profile)
                    .executeUpdate();
        } else {
            em.createQuery("UPDATE Entry e SET e.position = e.position + 1 WHERE e.position >= :pos AND e.userId = :userId AND e.profile = :profile")
                    .setParameter("pos", position)
                    .setParameter("userId", userId)
                    .setParameter("profile", profile)
                    .executeUpdate();
        }
        em.merge(entry);
        em.getTransaction().commit();
        em.close();
        emf.close();
        return entry;
    }

    @Override
    public void removeEntry(Long userId, long profile, long id) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        if (userId == null){
            em.createQuery("DELETE FROM Entry e WHERE e.id = :id AND e.userId IS NULL AND e.profile = :profile")
                    .setParameter("profile", profile)
                    .setParameter("id", id)
                    .executeUpdate();
        }else {
            em.createQuery("DELETE FROM Entry e WHERE e.id = :id AND e.userId = :userId AND e.profile = :profile")
                    .setParameter("userId", userId)
                    .setParameter("profile", profile)
                    .setParameter("id", id)
                    .executeUpdate();
        }
        em.getTransaction().commit();
        em.close();
        emf.close();
    }

    @Override
    public void swapEntries(Long userId, long profile, long id, int position) {

    }

    @Override
    public void swapListEntries(Long userId, long profile, long id, int position) {

    }

    @Override
    public void updateTask(Long userId, long profile, long id, String value) {

    }

    @Override
    public void updateFocus(Long userId, long profile, long id, int value) {

    }

    @Override
    public void updateDropped(Long userId, long profile, long id, int value) {

    }

    @Override
    public void updateList(Long userId, long profile, long id, String value, int position) {

    }

    @Override
    public void updateReminderDate(Long userId, long profile, long id, Long value) {

    }

    @Override
    public void updateRecurrence(Long userId, long profile, long id, Long reminderDate, String value) {

    }

    @Override
    public void updateListColor(Long userId, long profile, String list, String color) {

    }

    @Override
    public void updateUserName(Long userId, String name) {

    }

    @Override
    public boolean updateEmail(Long userId, String email) {
        return false;
    }

    @Override
    public List<User> getUsers() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        List<User> users = em.createQuery("SELECT u FROM User u", User.class)
                .getResultList();
        em.close();
        emf.close();
        return users;
    }

    @Override
    public List<Profile> getProfiles(Long userId) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        List<Profile> profiles;
        if (userId == null) {
            profiles = em.createQuery("SELECT p FROM Profile p WHERE p.profileId.userId = -1", Profile.class).getResultList();
        } else {
            profiles = em.createQuery("SELECT p FROM Profile p WHERE p.profileId.userId = :userId", Profile.class)
                    .setParameter("userId", userId)
                    .getResultList();
        }
        return profiles;
    }

    @Override
    public void updateId(Long userId, long profile, long entry, long id) {

    }

    @Override
    public void setTimeDelay(long delay) {

    }

    @Override
    public void addToQueue(User user, ZenServerMessage message) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        QueueItem item = new QueueItem();
        item.setClock(message.clock.jsonify());
        item.setUser(user);
        item.setType(message.type);
        item.setTimeStamp(message.timeStamp);
        item.setArguments(message.arguments);
        em.persist(item);

        em.getTransaction().commit();
        em.close();
        emf.close();
    }

    @Override
    public List<ZenServerMessage> getQueued(long userId) {
        List<ZenServerMessage> result = new ArrayList<>();
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.createQuery("SELECT qi FROM QueueItem qi", QueueItem.class).getResultStream().forEach(qi -> {
            List<Object> args = new ArrayList<>(qi.getArguments());
            ZenServerMessage zm = new ZenServerMessage(qi.getType(), args, new VectorClock(qi.getClock()), qi.getTimeStamp());
            result.add(zm);
        });
        em.close();
        emf.close();
        return result;
    }

    @Override
    public void clearQueue(long userId) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("DELETE FROM QueueItem qi WHERE qi.user.id = :userId").setParameter("userId", userId).executeUpdate();
        em.getTransaction().commit();
        em.close();
        emf.close();
    }

    @Override
    public String getToken(long user) {
        try {
            return Files.readString(Path.of((tokenPath == null ? "" : tokenPath + "/") + user + "_token"));
        } catch(IOException e){
            return null;
        }
    }

    @Override
    public void setToken(long user, String token) {
         this.token = token;
         System.out.println("TEST");
         System.out.println((tokenPath == null ? "" : tokenPath + "/") + user + "_token");
        try {
            Files.write(Path.of((tokenPath == null ? "" : tokenPath + "/") + user + "_token"), token.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public User addUser(long id, String email, String userName, long device) {

        System.out.println("ADD USER " + id);
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Profile profile = new Profile(0, null, id);
        User user = new User(email, userName, device);
        user.setId(id);
        VectorClock clock = new VectorClock(device);
        user.setClock(clock.jsonify());
        //user.setProfile(profile.getId());
        //user.setProfile(profile.getId());
        em.persist(user);
        em.persist(profile);
        em.getTransaction().commit();
        em.close();
        emf.close();

        return user;
    }

    @Override
    public Profile addProfile(Long userId) {
        return addProfile(userId, null);

    }

    @Override
    public Profile addProfile(Long userId, String name) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        User user = null;
        long count;
        if (userId != null) {
            user = em.createQuery("SELECT u FROM User u WHERE u.id = :userId", User.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
            count = em.createQuery("SELECT COUNT(p) FROM Profile p WHERE p.profileId.userId = userId", Long.class).setParameter("userId", userId).getSingleResult();
        }
        else
            count = em.createQuery("SELECT COUNT(p) FROM Profile p WHERE p.profileId.userId = -1", Long.class).getSingleResult();
        System.out.println("ADDING PROFILE FOR " + userId + " COUNT " + count);
        Profile profile = new Profile((int) count, name, user == null ? null : user.getId());
        System.out.println("PROFILE " + profile.getId() + " USER " + profile.getUser());
        em.persist(profile);
        em.getTransaction().commit();
        em.close();
        emf.close();
        return profile;
    }

    @Override
    public void removeUser(long userId) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("DELETE FROM User u WHERE u.id= :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        em.getTransaction().commit();
        em.close();
        emf.close();

    }

    @Override
    public void removeProfile(long profileId) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("DELETE FROM Profile p WHERE p.profileId.id= :profileId")
                .setParameter("profileId", profileId)
                .executeUpdate();
        em.getTransaction().commit();
        em.close();
        emf.close();

    }

    @Override
    public Optional<User> getUserByEmail(String email) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        List<User> results = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getResultList();

        em.close();
        emf.close();
        if (results.isEmpty())
            return Optional.empty();
        else
            return Optional.of(results.getFirst());
    }

    @Override
    public Optional<User> getUser(long id) {
        return Optional.empty();
    }

    @Override
    public Optional<Profile> getProfile(Long userId, long id) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();

        List<Profile> results;
        if (userId == null) {
            results = em.createQuery("SELECT p FROM Profile p WHERE p.profileId.userId = -1 IS NULL AND p.profileId.id = :id", Profile.class)
                    .setParameter("id", id)
                    .getResultList();
        } else {
            results = em.createQuery("SELECT p FROM Profile p WHERE p.profileId.userId = :userId AND p.profileId.id = :id", Profile.class)
                    .setParameter("userId", userId)
                    .setParameter("id", id)
                    .getResultList();
        }
        em.close();
        emf.close();

        if (results.isEmpty())
            return Optional.empty();
        else
            return Optional.of(results.getFirst());

    }

    @Override
    public boolean userExists(long userId) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        Long count = em.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();

        em.close();
        emf.close();
        return count > 0;
    }

    @Override
    public boolean isEnabled(long userId) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        Boolean enabled = em.createQuery("SELECT enabled FROM User u WHERE u.id = :userId", Boolean.class)
                .setParameter("userId", userId)
                .getSingleResult();
        em.close();
        emf.close();
        return enabled != null && enabled;
    }
    @Override
    public void enableUser(long userId) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("UPDATE User SET enabled=true WHERE id= :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        em.getTransaction().commit();
        em.close();
        emf.close();
    }
    @Override
    public void setDevice(long userId, long id) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("UPDATE User SET device= :device WHERE id= :userId")
                .setParameter("userId", userId)
                .setParameter("device", id)
                .executeUpdate();
        em.getTransaction().commit();
        em.close();
        emf.close();

    }
    @Override
    public void setClock(long userId, VectorClock clock) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("UPDATE User SET clock= :clock WHERE id= :userId")
                .setParameter("userId", userId)
                .setParameter("clock", clock.jsonify())
                .executeUpdate();
        em.getTransaction().commit();
        em.close();
        emf.close();

    }
}
