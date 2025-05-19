package net.berndreiss.zentodo.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import net.berndreiss.zentodo.data.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ListManager implements ListManagerI {
    private EntityManager em;

    public ListManager(EntityManager em){
        this.em = em;
    }
    public void close(){
        em.close();
    }

    @Override
    public synchronized TaskList addList(String name, String color) {
        TaskList list = new TaskList(name, color);
        em.getTransaction().begin();
        em.persist(list);
        em.getTransaction().commit();
        return list;
    }

    @Override
    public synchronized void addUserProfileToList(long userId, int profileId, long listId) {

        TaskList list = em.find(TaskList.class, listId);
        if (list == null)
            return;
        User user = em.find(User.class, userId);
        if (user == null)
            return;
        Profile profile = em.find(Profile.class, new ProfileId(profileId, user));
        if (profile == null)
            return;
        profile.getLists().add(list);
        list.getProfiles().add(profile);

        em.getTransaction().begin();
        em.persist(profile);
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
        Optional<Entry> entry;
        entry = em.createQuery("SELECT e FROM Entry e WHERE e.userId = :userId  AND profile = :profile AND e.id = :id", Entry.class)
                .setParameter("userId", userId)
                .setParameter("profile", profile)
                .setParameter("id", entryId)
                .getResultStream().findFirst();
        if (entry.isEmpty())
            return;

        Integer oldPosition = entry.get().getListPosition();
        List<Entry> listEntriesOld = new ArrayList<>();
        if (entry.get().getList() != null) {
            listEntriesOld = getListEntries(userId, profile, entry.get().getList());
        }
        List<Entry> listEntries = getListEntries(userId, profile, id);
        entry.get().setList(id);
        if (id != null)
            entry.get().setListPosition(listEntries.size());
        else
            entry.get().setListPosition(null);

        em.getTransaction().begin();
        em.merge(entry.get());
        for (Entry e : listEntriesOld) {
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
    public synchronized void updateListName(long listId, String name) {
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
    public List<Entry> getListEntries(long userId, int profile, Long list) {
        List<Entry> entries;
        if (list == null){
            entries= em.createQuery("SELECT e FROM Entry e WHERE e.list IS NULL AND e.userId = :userId AND e.profile = :profile", Entry.class)
                    .setParameter("userId", userId)
                    .setParameter("profile", profile)
                    .getResultList();
        }else {
            entries = em.createQuery("SELECT e FROM Entry e WHERE e.list = :list AND e.userId = :userId AND e.profile = :profile", Entry.class)
                    .setParameter("list", list)
                    .setParameter("userId", userId)
                    .setParameter("profile", profile)
                    .getResultList();
        }
        return entries;
    }

    @Override
    public Optional<TaskList> getList(long id) {

        try {
            TaskList result = em.createQuery(
                            "SELECT l FROM TaskList l WHERE l.id = :id", TaskList.class)
                    .setParameter("id", id)
                    .getSingleResult();
            return Optional.of(result);
        } catch (NoResultException e) {
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
    public synchronized void swapListEntries(long userId, long profile, long entryId, int position) {

    }

}
