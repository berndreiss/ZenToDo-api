package net.berndreiss.zentodo.persistence;

import jakarta.persistence.EntityManager;
import net.berndreiss.zentodo.data.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class EntryManager implements EntryManagerI {


    private final EntityManager em;

    public EntryManager(EntityManager entityManager){
        this.em = entityManager;
    }

    void close(){
        em.close();
    }

    @Override
    public Optional<Entry> getEntry(long userId, int profile, long id) {
        Optional<Entry> entry;
        entry = em.createQuery("SELECT e FROM Entry e WHERE e.userId = :userId  AND profile = :profile AND e.id = :id", Entry.class)
                .setParameter("userId", userId)
                .setParameter("profile", profile)
                .setParameter("id", id)
                .getResultStream().findFirst();
        return entry;
    }

    @Override
    public List<Entry> getEntries(long userId, int profile) {
        List<Entry> list;
        list = em.createQuery("SELECT e FROM Entry e WHERE e.userId = :userId AND e.profile = :profile", Entry.class)
                .setParameter("userId", userId)
                .setParameter("profile", profile)
                .getResultList();
        return list.stream().sorted(Comparator.comparing(Entry::getPosition)).toList();
    }

    @Override
    public List<Entry> loadFocus(long userId, int profile) {
        return em.createQuery("SELECT e FROM Entry e WHERE e.userId = :userId AND e.profile = :profile AND e.focus = true", Entry.class)
                .setParameter("userId", userId)
                .setParameter("profile", profile)
                .getResultList();
    }

    @Override
    public List<Entry> loadDropped(long userId, int profile) {
        return em.createQuery("SELECT e FROM Entry e WHERE e.userId = :userId AND e.profile = :profile AND e.dropped = true", Entry.class)
                .setParameter("userId", userId)
                .setParameter("profile", profile)
                .getResultList();
    }

    @Override
    public synchronized Entry addNewEntry (long userId, int profile, String task) {
        List<Entry> entries = getEntries(userId, profile);
        Entry entry = null;
        try {
            entry = addNewEntry(userId, profile, task, entries.size());
        } catch (PositionOutOfBoundException _) {}
        return entry;
    }
    @Override
    public synchronized Entry addNewEntry (long userId, int profile, String task,int position) throws PositionOutOfBoundException{

        List<Entry> entries = getEntries(userId, profile);
        if (position > entries.size())
            throw new PositionOutOfBoundException("Position is out of bounds: position " + position);
        Set<Long> ids = entries.stream().map(Entry::getId).collect(Collectors.toSet());
        long id;
        Random random = new Random();
        do {
            id = random.nextLong();
        }
        while (ids.contains(id) || id == 0);
        Entry entry = null;
        try {
            entry = addNewEntry(userId, profile, id, task, position);
        } catch(DuplicateIdException | InvalidActionException _){}
        return entry;
    }


    @Override
    public synchronized Entry addNewEntry(long userId, int profile, long id, String task, int position) throws DuplicateIdException, PositionOutOfBoundException, InvalidActionException {
        if (id == 0)
            throw new InvalidActionException("Id must not be 0.");

        List<Entry> entries = getEntries(userId, profile);
        if (position > entries.size())
            throw new PositionOutOfBoundException("Position is out of bounds: position " + position);

        Optional<Entry> existingEntry = getEntry(userId, profile, id);
        if (existingEntry.isPresent())
            throw new DuplicateIdException("Entry with id already exists: id " + id);
        Entry entry = new Entry(userId, profile, id, task, position);
        em.getTransaction().begin();
        for (Entry e: entries){
            if (e.getPosition() >= position) {
                e.setPosition(e.getPosition() + 1);
                em.merge(e);
            }
        }
        em.merge(entry);
        em.getTransaction().commit();
        return entry;
    }

    @Override
    public synchronized void removeEntry(long userId, int profile, long id) {

        em.getTransaction().begin();
        em.createQuery("DELETE FROM Entry e WHERE e.id = :id AND e.userId = :userId AND e.profile = :profile")
                .setParameter("userId", userId)
                .setParameter("profile", profile)
                .setParameter("id", id)
                .executeUpdate();
        em.getTransaction().commit();
    }
    @Override
    public synchronized void updateId(long userId, int profile, long id, long newId) throws DuplicateIdException {
        Optional<Entry> entry = getEntry(userId, profile, newId);
        if (entry.isPresent())
            throw new DuplicateIdException("Id for entry already exists: id " + newId);
        em.getTransaction().begin();
        em.createQuery("UPDATE Entry e SET e.id = :newId WHERE e.id = :id AND e.userId = :userId AND e.profile = :profile")
                .setParameter("newId", newId)
                .setParameter("id", id)
                .setParameter("profile", profile)
                .setParameter("userId", userId)
                .executeUpdate();
        em.getTransaction().commit();
    }

    @Override
    public synchronized void swapEntries(long userId, int profile, long id, int position) throws PositionOutOfBoundException {


        List<Entry> entries = getEntries(userId, profile);
        if (entries.size() <= position)
            throw new PositionOutOfBoundException("Cannot insert into position because list of entries is too small: number of items " + entries.size());
        Optional<Entry> entry0Opt = entries.stream().filter(e -> e.getId() == id).findFirst();
        if (entry0Opt.isEmpty())
            return;
        Entry entry0 = entry0Opt.get();
        Optional<Entry> entry1Opt = entries.stream().findFirst().filter(e -> e.getPosition() == position);
        if (entry1Opt.isEmpty())
            return;
        Entry entry1 = entry1Opt.get();
        em.getTransaction().begin();

        entry1.setPosition(entry0.getPosition());
        entry0.setPosition(position);

        em.merge(entry0);
        em.merge(entry1);
        em.getTransaction().commit();
    }


    @Override
    public synchronized void updateTask(long userId, int profile, long id, String value) {
        if (value == null)
            return;
        Optional<Entry> entry = getEntry(userId, profile, id);
        if (entry.isEmpty())
            return;
        entry.get().setTask(value);
        em.getTransaction().begin();
        em.merge(entry.get());
        em.getTransaction().commit();
    }

    @Override
    public synchronized void updateFocus(long userId, int profile, long id, boolean value) {

        Optional<Entry> entry = getEntry(userId, profile, id);
        if (entry.isEmpty())
            return;
        entry.get().setFocus(value);
        entry.get().setDropped(false);
        em.getTransaction().begin();
        em.merge(entry.get());
        em.getTransaction().commit();
    }

    @Override
    public synchronized void updateDropped(long userId, int profile, long id, boolean value) {
        Optional<Entry> entry = getEntry(userId, profile, id);
        if (entry.isEmpty())
            return;
        entry.get().setDropped(value);
        em.getTransaction().begin();
        em.merge(entry.get());
        em.getTransaction().commit();
    }

    @Override
    public synchronized void updateReminderDate(long userId, int profile, long id, Instant value) {
        Optional<Entry> entry = getEntry(userId, profile, id);
        if (entry.isEmpty())
            return;
        entry.get().setReminderDate(value);
        em.getTransaction().begin();
        em.merge(entry.get());
        em.getTransaction().commit();
    }

    @Override
    public synchronized void updateRecurrence(long userId, int profile, long id, String value) {

        Optional<Entry> entry = getEntry(userId, profile, id);
        if (entry.isEmpty())
            return;
        entry.get().setRecurrence(value);
        em.getTransaction().begin();
        em.merge(entry.get());
        em.getTransaction().commit();
    }

}
