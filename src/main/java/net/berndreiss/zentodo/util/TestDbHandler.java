package net.berndreiss.zentodo.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import net.berndreiss.zentodo.OperationType;
import net.berndreiss.zentodo.data.ClientOperationHandler;
import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * TODO DESCRIBE
 */
public class TestDbHandler implements ClientOperationHandler {
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
    @Override
    public void post(List<Entry> entries) {

        em.getTransaction().begin();
        for (Entry e: entries){
            em.merge(e);
        }

        em.getTransaction().commit();

    }

    @Override
    public void addNewEntry(long id, String task, Long userId) {

        Integer maxPosition = em.createQuery("SELECT MAX(e.position) FROM Entry e", Integer.class).getSingleResult();
        if (maxPosition == null)
            maxPosition = -1;
        Entry entry = new Entry(id, maxPosition+1, task, userId);
        em.getTransaction().begin();
        em.merge(entry);
        em.getTransaction().commit();

    }

    @Override
    public void delete(long id) {

    }

    @Override
    public void swapEntries(long id, int position) {

    }

    @Override
    public void swapListEntries(long id, int position) {

    }

    @Override
    public void updateTask(long id, String value) {

    }

    @Override
    public void updateFocus(long id, int value) {

    }

    @Override
    public void updateDropped(long id, int value) {

    }

    @Override
    public void updateList(long id, String value, int position) {

    }

    @Override
    public void updateReminderDate(long id, Long value) {

    }

    @Override
    public void updateRecurrence(long id, Long reminderDate, String value) {

    }

    @Override
    public void updateListColor(String list, String color) {

    }

    @Override
    public void updateId(long entry, long id) {

    }

    @Override
    public void setTimeDelay(long delay) {

    }

    @Override
    public void addToQueue(OperationType type, List<Object> arguments) {

    }

    @Override
    public List<ZenMessage> geQueued() {
        return List.of();
    }

    @Override
    public String getToken(long user) {
        try {
            return Files.readString(Path.of(user + "_token"));
        } catch(IOException e){
            return null;
        }
    }

    @Override
    public void setToken(long user, String token) {
         this.token = token;
        try {
            Files.write(Path.of(user + "_token"), token.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addUser(long id, String email, String userName) {

        User user = new User(id, email, userName);

        em.getTransaction().begin();
        em.persist(user);
        em.getTransaction().commit();

    }

    @Override
    public User getUserByEmail(String email) {

        if (!userExists(email))
            return null;

        User user = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getSingleResult();

        return user;
    }

    @Override
    public boolean userExists(String email) {
        Long count = em.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();

        return count > 0;
    }

    @Override
    public boolean isEnabled(String email) {
        Boolean enabled = em.createQuery("SELECT enabled FROM User u WHERE u.email = :email", Boolean.class)
                .setParameter("email", email)
                .getSingleResult();
        return enabled != null && enabled;
    }

    @Override
    public void enableUser(String email) {
        em.getTransaction().begin();
        em.createQuery("UPDATE User SET enabled=true WHERE email= :email")
                .setParameter("email", email)
                .executeUpdate();
        em.getTransaction().commit();
    }
}
