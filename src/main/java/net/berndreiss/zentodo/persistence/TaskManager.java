package net.berndreiss.zentodo.persistence;

import jakarta.persistence.EntityManager;
import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.data.Task;
import net.berndreiss.zentodo.exceptions.DuplicateIdException;
import net.berndreiss.zentodo.exceptions.InvalidActionException;
import net.berndreiss.zentodo.exceptions.PositionOutOfBoundException;
import net.berndreiss.zentodo.util.ClientStub;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the TaskManagerI interface using JPA.
 */
public class TaskManager implements TaskManagerI {


    private final EntityManager em;

    /**
     * Create a new instance of the task manager.
     * @param em the entity manager for interacting with the database
     */
    public TaskManager(EntityManager em){
        this.em = em;
    }

    void close(){
        em.close();
    }

    @Override
    public Optional<Task> getTask(long userId, int profile, long id) {
        Optional<Task> entry;
        entry = em.createQuery("SELECT e FROM Task e WHERE e.userId = :userId  AND profile = :profile AND e.id = :id", Task.class)
                .setParameter("userId", userId)
                .setParameter("profile", profile)
                .setParameter("id", id)
                .getResultStream().findFirst();
        return entry;
    }

    @Override
    public List<Task> getTasks(long userId, int profile) {
        List<Task> list;
        list = em.createQuery("SELECT e FROM Task e WHERE e.userId = :userId AND e.profile = :profile", Task.class)
                .setParameter("userId", userId)
                .setParameter("profile", profile)
                .getResultList();
        return list.stream().sorted(Comparator.comparing(Task::getPosition)).toList();
    }

    @Override
    public List<Task> loadFocus(long userId, int profile) {
        return em.createQuery("SELECT e FROM Task e WHERE e.userId = :userId AND e.profile = :profile AND e.focus = true", Task.class)
                .setParameter("userId", userId)
                .setParameter("profile", profile)
                .getResultList();
    }

    @Override
    public List<Task> loadDropped(long userId, int profile) {
        return em.createQuery("SELECT e FROM Task e WHERE e.userId = :userId AND e.profile = :profile AND e.dropped = true", Task.class)
                .setParameter("userId", userId)
                .setParameter("profile", profile)
                .getResultList();
    }

    @Override
    public synchronized Task addNewTask(long userId, int profile, String task) {
        List<Task> entries = getTasks(userId, profile);
        Task entry = null;
        try {
            entry = addNewTask(userId, profile, task, entries.size());
        } catch (PositionOutOfBoundException e) {
            ClientStub.logger.error("Out of bounds error when adding task.", e);
            throw new RuntimeException(e);
        }
        return entry;
    }
    @Override
    public synchronized Task addNewTask(long userId, int profile, String task, int position) throws PositionOutOfBoundException{

        List<Task> entries = getTasks(userId, profile);
        if (position > entries.size())
            throw new PositionOutOfBoundException("Position is out of bounds: position " + position);
        Set<Long> ids = entries.stream().map(Task::getId).collect(Collectors.toSet());
        long id;
        Random random = new Random();
        do {
            id = random.nextLong();
        }
        while (ids.contains(id) || id == 0);
        Task entry = null;
        try {
            entry = addNewTask(userId, profile, id, task, position);
        } catch(DuplicateIdException | InvalidActionException e){
            ClientStub.logger.error("Error when adding task.", e);
            throw new RuntimeException(e);
        }
        return entry;
    }


    @Override
    public synchronized Task addNewTask(long userId, int profile, long id, String task, int position) throws DuplicateIdException, PositionOutOfBoundException, InvalidActionException {
        if (id == 0)

            throw new InvalidActionException("Id must not be 0.");

        List<Task> entries = getTasks(userId, profile);
        if (position > entries.size())
            throw new PositionOutOfBoundException("Position is out of bounds: position " + position);

        Optional<Task> existingEntry = getTask(userId, profile, id);
        if (existingEntry.isPresent())
            throw new DuplicateIdException("Task with id already exists: id " + id);
        Task entry = new Task(userId, profile, id, task, position);
        try {
            em.getTransaction().begin();
            for (Task e : entries) {
                if (e.getPosition() >= position) {
                    e.setPosition(e.getPosition() + 1);
                    em.merge(e);
                }
            }
            em.merge(entry);
            em.getTransaction().commit();
        } catch(RuntimeException e){
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
        }
        return entry;
    }

    @Override
    public synchronized void removeTask(long userId, int profile, long id) {

        em.getTransaction().begin();
        em.createQuery("DELETE FROM Task e WHERE e.id = :id AND e.userId = :userId AND e.profile = :profile")
                .setParameter("userId", userId)
                .setParameter("profile", profile)
                .setParameter("id", id)
                .executeUpdate();
        em.getTransaction().commit();
    }
    @Override
    public synchronized void updateId(long userId, int profile, long id, long newId) throws DuplicateIdException {
        Optional<Task> entry = getTask(userId, profile, newId);
        if (entry.isPresent())
            throw new DuplicateIdException("Id for task already exists: id " + newId);
        em.getTransaction().begin();
        em.createQuery("UPDATE Task e SET e.id = :newId WHERE e.id = :id AND e.userId = :userId AND e.profile = :profile")
                .setParameter("newId", newId)
                .setParameter("id", id)
                .setParameter("profile", profile)
                .setParameter("userId", userId)
                .executeUpdate();
        em.getTransaction().commit();
    }

    @Override
    public void postTask(Task task) {
        em.getTransaction().begin();
        em.persist(task);
        em.getTransaction().commit();
    }

    @Override
    public synchronized void swapTasks(long userId, int profile, long id, int position) throws PositionOutOfBoundException {


        List<Task> entries = getTasks(userId, profile);
        if (entries.size() <= position)
            throw new PositionOutOfBoundException("Cannot insert into position because list of entries is too small: number of items " + entries.size());
        Optional<Task> entry0Opt = entries.stream().filter(e -> e.getId() == id).findFirst();
        if (entry0Opt.isEmpty())
            return;
        Task task0 = entry0Opt.get();
        Optional<Task> entry1Opt = entries.stream().findFirst().filter(e -> e.getPosition() == position);
        if (entry1Opt.isEmpty())
            return;
        Task task1 = entry1Opt.get();
        em.getTransaction().begin();

        task1.setPosition(task0.getPosition());
        task0.setPosition(position);

        em.merge(task0);
        em.merge(task1);
        em.getTransaction().commit();
    }


    @Override
    public synchronized void updateTask(long userId, int profile, long id, String value) {
        if (value == null)
            return;
        Optional<Task> entry = getTask(userId, profile, id);
        if (entry.isEmpty())
            return;
        entry.get().setTask(value);
        em.getTransaction().begin();
        em.merge(entry.get());
        em.getTransaction().commit();
    }

    @Override
    public synchronized void updateFocus(long userId, int profile, long id, boolean value) {

        Optional<Task> entry = getTask(userId, profile, id);
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
        Optional<Task> entry = getTask(userId, profile, id);
        if (entry.isEmpty())
            return;
        entry.get().setDropped(value);
        em.getTransaction().begin();
        em.merge(entry.get());
        em.getTransaction().commit();
    }

    @Override
    public synchronized void updateReminderDate(long userId, int profile, long id, Instant value) {
        Optional<Task> entry = getTask(userId, profile, id);
        if (entry.isEmpty())
            return;
        entry.get().setReminderDate(value);
        em.getTransaction().begin();
        em.merge(entry.get());
        em.getTransaction().commit();
    }

    @Override
    public synchronized void updateRecurrence(long userId, int profile, long id, String value) {

        Optional<Task> entry = getTask(userId, profile, id);
        if (entry.isEmpty())
            return;
        entry.get().setRecurrence(value);
        em.getTransaction().begin();
        em.merge(entry.get());
        em.getTransaction().commit();
    }

}
