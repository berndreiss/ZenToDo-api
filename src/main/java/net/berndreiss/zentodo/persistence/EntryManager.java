package net.berndreiss.zentodo.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.EntryManagerI;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class EntryManager implements EntryManagerI {


    private final String persistenceUnit;

    public EntryManager(String persistenceUnit){
        this.persistenceUnit = persistenceUnit;
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
    public void updateId(Long userId, long profile, long entry, long id) {

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
}
