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

/**
 * TODO DESCRIBE
 */
public class TestDbHandler implements net.berndreiss.zentodo.data.Persistence {
    public String tokenPath;
    EntityManagerFactory emf;
    EntityManager em;
    String token;

    /**
     * TODO DESCRIBE
     * @param persistenceUnit
     */
    public TestDbHandler (String persistenceUnit){

        emf =  Persistence.createEntityManagerFactory(persistenceUnit);
        em = emf.createEntityManager();
    }

    public void close(){
        em.close();
        emf.close();
    }

    public List<Entry> getAllEntries(Long userId){
        return em.createQuery("SELECT e FROM Entry e WHERE e.id = :userId", Entry.class).setParameter("userId", userId).getResultList();
    }

    @Override
    public void post(List<Entry> entries) {

        em.getTransaction().begin();
        for (Entry e: entries){
            em.merge(e);
        }

        em.getTransaction().commit();

    }

    @Override
    public Entry addNewEntry(Long userId, long id, String task, int position) {

        Entry entry = new Entry(id, task, userId, position);
        em.getTransaction().begin();
        em.createQuery(
                        "UPDATE Entry e SET e.position = e.position + 1 WHERE e.position >= :pos AND e.userId = :userId")
                .setParameter("pos", position)
                .setParameter("userId", userId)
                .executeUpdate();
        em.merge(entry);
        em.getTransaction().commit();
        return entry;

    }

    @Override
    public void delete(Long userId, long id) {

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
    public void updateUserName(Long userId, long id, String name) {

    }

    @Override
    public boolean updateEmail(Long userId, long id, String email) {
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

        em.getTransaction().begin();

        QueueItem item = new QueueItem();
        item.setClock(message.clock.jsonify());
        item.setUser(user);
        item.setType(message.type);
        item.setTimeStamp(message.timeStamp);
        item.setArguments(message.arguments);
        em.persist(item);

        em.getTransaction().commit();
    }

    @Override
    public List<ZenServerMessage> getQueued(long userId) {
        List<ZenServerMessage> result = new ArrayList<>();
        em.createQuery("SELECT qi FROM QueueItem qi", QueueItem.class).getResultStream().forEach(qi -> {
            List<Object> args = new ArrayList<>(qi.getArguments());
            ZenServerMessage zm = new ZenServerMessage(qi.getType(), args, new VectorClock(qi.getClock()), qi.getTimeStamp());
            result.add(zm);
        });
        return result;
    }

    @Override
    public void clearQueue(long userId) {

        em.getTransaction().begin();
        em.createQuery("DELETE FROM QueueItem qi WHERE qi.user.id = :userId").setParameter("userId", userId).executeUpdate();
        em.getTransaction().commit();
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

        em.getTransaction().begin();
        em.persist(user);
        em.getTransaction().commit();

        return user;
    }

    @Override
    public void removeUser(long userId) {
        em.getTransaction().begin();
        em.createQuery("DELETE FROM User u WHERE u.id= :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        em.getTransaction().commit();

    }

    @Override
    public User getUserByEmail(String email) {

        List<User> results = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getResultList();

        if (results.isEmpty())
            return null;
        else
            return results.getFirst();
    }

    @Override
    public boolean userExists(long userId) {
        Long count = em.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();

        return count > 0;
    }

    @Override
    public boolean isEnabled(long userId) {
        Boolean enabled = em.createQuery("SELECT enabled FROM User u WHERE u.id = :userId", Boolean.class)
                .setParameter("userId", userId)
                .getSingleResult();
        return enabled != null && enabled;
    }

    @Override
    public void enableUser(long userId) {
        em.getTransaction().begin();
        em.createQuery("UPDATE User SET enabled=true WHERE id= :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        em.getTransaction().commit();
    }

    @Override
    public void setDevice(long userId, long id) {
        em.getTransaction().begin();
        em.createQuery("UPDATE User SET device= :device WHERE id= :userId")
                .setParameter("userId", userId)
                .setParameter("device", id)
                .executeUpdate();
        em.getTransaction().commit();

    }

    @Override
    public void setClock(long userId, VectorClock clock) {

        em.getTransaction().begin();
        em.createQuery("UPDATE User SET clock= :clock WHERE id= :userId")
                .setParameter("userId", userId)
                .setParameter("clock", clock.jsonify())
                .executeUpdate();
        em.getTransaction().commit();

    }

}
