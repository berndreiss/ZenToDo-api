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
    }

    public void close(){
    }

    public List<Entry> getAllEntries(Long userId) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        List<Entry> list = em.createQuery("SELECT e FROM Entry e WHERE e.id = :userId", Entry.class).setParameter("userId", userId).getResultList();
        em.close();
        emf.close();
        return list;
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
    public Optional<Entry> getEntry(Long userId, long id) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        Optional<Entry> entry = em.createQuery("SELECT e FROM Entry e WHERE e.userId = :userId AND e.id = :id", Entry.class)
                .setParameter("userId", userId)
                .setParameter("id", id)
                .getResultStream().findFirst();
        em.close();
        emf.close();
        return entry;
    }

    @Override
    public List<Entry> getEntries(Long userId) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        List<Entry> list =  em.createQuery("SELECT e FROM Entry e WHERE e.userId = :userId", Entry.class)
                .setParameter("userId", userId)
                .getResultList();

        em.close();
        emf.close();
        return  list;
    }

    @Override
    public Entry addNewEntry (Long userId, String task) {
        long position = 0;

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();

        position = em.createQuery("SELECT COUNT (e) FROM Entry e WHERE e.userId = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        em.close();
        emf.close();
        return addNewEntry(userId, task, (int) position);
    }
    @Override
    public Entry addNewEntry (Long userId, String task,int position) {

        long id;
        Random random = new Random();
        boolean idExists;
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        do {
            id = random.nextLong();
            Long count = em.createQuery(
                            "SELECT COUNT(e) FROM Entry e WHERE e.userId = :userId AND e.id = :id", Long.class)
                    .setParameter("userId", userId)
                    .setParameter("id", id)
                    .getSingleResult();
            idExists = count > 0;

        }
        while (idExists);
        em.close();
        emf.close();
        return addNewEntry(userId, id, task, position);
    }


    @Override
    public Entry addNewEntry(Long userId, long id, String task, int position) {

        Entry entry = new Entry(userId, id, task, position);
        System.out.println(entry.getUserId());
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("UPDATE Entry e SET e.position = e.position + 1 WHERE e.position >= :pos AND e.userId = :userId")
                .setParameter("pos", position)
                .setParameter("userId", userId)
                .executeUpdate();
        em.merge(entry);
        em.getTransaction().commit();
        em.close();
        emf.close();
        return entry;
    }

    @Override
    public void delete(Long userId, long id) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("DELETE FROM Entry e WHERE e.id = :id AND e.userId = :userId")
                .setParameter("userId", userId)
                .setParameter("id", id)
                .executeUpdate();
        em.getTransaction().commit();
        em.close();
        emf.close();
    }

    @Override
    public void swapEntries(Long userId, long id, int position) {

    }

    @Override
    public void swapListEntries(Long userId, long id, int position) {

    }

    @Override
    public void updateTask(Long userId, long id, String value) {

    }

    @Override
    public void updateFocus(Long userId, long id, int value) {

    }

    @Override
    public void updateDropped(Long userId, long id, int value) {

    }

    @Override
    public void updateList(Long userId, long id, String value, int position) {

    }

    @Override
    public void updateReminderDate(Long userId, long id, Long value) {

    }

    @Override
    public void updateRecurrence(Long userId, long id, Long reminderDate, String value) {

    }

    @Override
    public void updateListColor(Long userId, String list, String color) {

    }

    @Override
    public void updateUserName(Long userId, String name) {

    }

    @Override
    public boolean updateEmail(Long userId, String email) {
        return false;
    }

    @Override
    public void updateId(Long userId, long entry, long id) {

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

        User user = new User(email, userName, device);

        user.setId(id);

        VectorClock clock = new VectorClock(device);
        user.setClock(clock.jsonify());

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(user);
        em.getTransaction().commit();
        em.close();
        emf.close();

        return user;
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
    public User getUserByEmail(String email) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        EntityManager em = emf.createEntityManager();
        List<User> results = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getResultList();

        em.close();
        emf.close();
        if (results.isEmpty())
            return null;
        else
            return results.getFirst();
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
