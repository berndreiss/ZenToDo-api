package net.berndreiss.zentodo.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.exceptions.InvalidActionException;
import net.berndreiss.zentodo.exceptions.PositionOutOfBoundException;

import java.util.*;

public class ListManager implements ListManagerI {
    private EntityManager em;

    public ListManager(EntityManager em){
        this.em = em;
    }
    public void close(){
        em.close();
    }

    @Override
    public synchronized TaskList addList(long id, String name, String color) throws InvalidActionException {
        if (name == null)
            throw new InvalidActionException("List name must not be null.");
        if (getList(id).isPresent())
            return null;
        TaskList list = new TaskList(id, name, color);
        em.getTransaction().begin();
        em.persist(list);
        em.getTransaction().commit();
        return list;
    }

    @Override
    public synchronized void addUserProfileToList(long userId, int profileId, long listId) throws InvalidActionException {

        TaskList list = em.find(TaskList.class, listId);
        if (list == null)
            return;
        User user = em.find(User.class, userId);
        if (user == null)
            return;
        Profile profile = em.find(Profile.class, new ProfileId(profileId, user));
        if (profile == null)
            return;
        if (getListByName(userId, profileId, list.getName()).isPresent())
            throw new InvalidActionException("List with the same name already exists for user");
        profile.getLists().add(list);
        list.getProfiles().add(profile);

        em.getTransaction().begin();
        em.persist(profile);
        em.persist(list);
        em.getTransaction().commit();
    }

    @Override
    public synchronized void removeUserProfileFromList(long userId, int profileId, long listId) {

        TaskList list = em.find(TaskList.class, listId);
        if (list == null)
            return;
        User user = em.find(User.class, userId);
        if (user == null)
            return;
        Profile profile = em.find(Profile.class, new ProfileId(profileId, user));
        if (profile == null)
            return;
        profile.getLists().remove(list);
        list.getProfiles().remove(profile);

        em.getTransaction().begin();
        em.persist(profile);
        em.persist(list);
        em.createNativeQuery("UPDATE tasks SET list = NULL, listposition = NULL WHERE list = :listId")
                .setParameter("listId", listId)
                .executeUpdate();
        em.getTransaction().commit();
    }

    @Override
    public synchronized void removeList(long id) {
        TaskList list = em.find(TaskList.class, id);

        if (list != null) {
            em.getTransaction().begin();
            for (Profile profile : new ArrayList<>(list.getProfiles())) {
                profile.getLists().remove(list);
                em.merge(profile);
            }
            list.getProfiles().clear();
            em.remove(list);
            em.createNativeQuery("UPDATE tasks SET list = NULL, listposition = NULL WHERE list = :listId")
                    .setParameter("listId", id)
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }

    @Override
    public synchronized void updateList(long userId, int profile, long entryId, Long id) {
        if (id != null) {
            Optional<TaskList> list = getList(id);
            if (list.isEmpty())
                return;
        }
        Optional<Task> entry;
        entry = em.createQuery("SELECT t FROM Task t WHERE t.userId = :userId  AND profile = :profile AND t.id = :id", Task.class)
                .setParameter("userId", userId)
                .setParameter("profile", profile)
                .setParameter("id", entryId)
                .getResultStream().findFirst();
        if (entry.isEmpty())
            return;

        Integer oldPosition = entry.get().getListPosition();
        List<Task> listEntriesOld = new ArrayList<>();
        if (entry.get().getList() != null) {
            listEntriesOld = getListEntries(userId, profile, entry.get().getList());
        }
        List<Task> listEntries = getListEntries(userId, profile, id);
        entry.get().setList(id);
        if (id != null)
            entry.get().setListPosition(listEntries.size());
        else
            entry.get().setListPosition(null);

        em.getTransaction().begin();
        em.merge(entry.get());
        for (Task e : listEntriesOld) {
            if (e.getId() == entryId)
                continue;
            if (oldPosition != null && e.getListPosition() > oldPosition) {
                e.setListPosition(e.getListPosition() - 1);
                em.merge(e);
            }
        }
        em.getTransaction().commit();


    }

    @Override
    public synchronized Long updateId(long listId, long id) {
        Optional<TaskList> list = getList(listId);
        if (list.isEmpty())
            return null;
        Optional<TaskList> existingList = getList(id);
        Long newId = null;
        if (existingList.isPresent()) {
            newId = getUniqueUserId();
            updateId(existingList.get().getId(), newId);
        }

        //THIS IS VERY DIRTY NEED A BETTER WAY TO DO THIS
        //STILL WE NEED TO BE ABLE TO UPDATE THE ID ON LISTS
        em.getTransaction().begin();
        em.createNativeQuery("ALTER TABLE profile_list DISABLE TRIGGER ALL").executeUpdate();
        em.createNativeQuery("UPDATE profile_list SET list_id = :id WHERE list_id = :listId")
                .setParameter("id", id)
                .setParameter("listId", listId)
                .executeUpdate();
        em.createNativeQuery("ALTER TABLE profile_list ENABLE TRIGGER ALL");
        em.createNativeQuery("UPDATE tasks SET list = :id WHERE list = :listId")
                .setParameter("id", id)
                .setParameter("listId", listId)
                .executeUpdate();
        em.createNativeQuery("UPDATE lists SET id = :id WHERE id = :listId")
                .setParameter("id", id)
                .setParameter("listId", listId)
                .executeUpdate();
        em.getTransaction().commit();
        em.clear();
        return newId;

    }

    @Override
    public synchronized void updateListName(long listId, String name) throws InvalidActionException {
        if (name == null)
            throw new InvalidActionException("List name must not be null.");
        Optional<TaskList> list = getList(listId);
        if (list.isEmpty())
            return;
        list.get().setName(name);
        em.getTransaction().begin();
        em.merge(list.get());
        em.getTransaction().commit();
    }

    @Override
    public synchronized void updateListColor(long listId, String color) {
        Optional<TaskList> list = getList(listId);
        if (list.isEmpty())
            return;
        list.get().setColor(color);
        em.getTransaction().begin();
        em.merge(list.get());
        em.getTransaction().commit();
    }

    @Override
    public List<Task> getListEntries(long userId, int profile, Long list) {
        List<Task> entries;
        if (list == null){
            entries= em.createQuery("SELECT t FROM Task t WHERE t.list IS NULL AND t.userId = :userId AND t.profile = :profile", Task.class)
                    .setParameter("userId", userId)
                    .setParameter("profile", profile)
                    .getResultList();
        }else {
            entries = em.createQuery("SELECT t FROM Task t WHERE t.list = :list AND t.userId = :userId AND t.profile = :profile", Task.class)
                    .setParameter("list", list)
                    .setParameter("userId", userId)
                    .setParameter("profile", profile)
                    .getResultList();
        }
        if (list == null)
            return  entries.stream().sorted(Comparator.comparing(Task::getPosition)).toList();
        return entries.stream().sorted(Comparator.comparing(Task::getListPosition)).toList();
    }

    @Override
    public Optional<TaskList> getList(long id) {
        TaskList result = em.find(TaskList.class, id);
        return result == null ? Optional.empty() : Optional.of(result);
    }

    @Override
    public Optional<TaskList> getListByName(long userId, int profile, String name) {
        try {
            long id = (long) em.createNativeQuery("SELECT id FROM lists JOIN profile_list ON list_id = id WHERE profile_id = :profile AND profile_user_id = :userId AND name = :name")
                    .setParameter("profile", profile)
                    .setParameter("userId", userId)
                    .setParameter("name", name)
                    .getSingleResult();

            return getList(id);
        } catch (NoResultException _){
            return Optional.empty();
        }

    }

    @Override
    public List<TaskList> getListsForUser(long userId, int profile) {

        return em.createQuery("SELECT l FROM TaskList l JOIN l.profiles p WHERE p.profileId.id = :profile AND p.profileId.user.id = :userId", TaskList.class)
                .setParameter("profile", profile)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public List<TaskList> getLists() {
       return em.createQuery("SELECT l FROM TaskList l", TaskList.class).getResultList();
    }

    @Override
    public synchronized void swapListEntries(long userId, int profile, long list, long entryId, int position) throws PositionOutOfBoundException {
        List<Task> entries = getListEntries(userId, profile, list);
        if (position >= entries.size())
            throw new PositionOutOfBoundException("List position is too big.");
        Optional<Task> entry = entries.stream().filter(e -> e.getId() == entryId).findFirst();
        if (entry.isEmpty())
            return;
        Task other = entries.get(position);

        other.setListPosition(entry.get().getListPosition());
        entry.get().setListPosition(position);

        em.getTransaction().begin();
        em.merge(entry.get());
        em.merge(other);
        em.getTransaction().commit();

    }

    public long getUniqueUserId(){
        Random random = new Random();
        long id = random.nextLong();
        while (getList(id).isPresent())
            id++;
        return id;
    }
}
