package net.berndreiss.zentodo.persistence;

import jakarta.persistence.EntityManager;
import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.data.Profile;
import net.berndreiss.zentodo.data.ProfileId;
import net.berndreiss.zentodo.data.QueueItem;
import net.berndreiss.zentodo.data.User;
import net.berndreiss.zentodo.exceptions.DuplicateUserIdException;
import net.berndreiss.zentodo.exceptions.InvalidUserActionException;
import net.berndreiss.zentodo.util.VectorClock;
import net.berndreiss.zentodo.util.ZenServerMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserManager implements UserManagerI {

    private EntityManager em;
    private final String tokenPath;
    private String token;
    public UserManager(EntityManager em, String tokenPath){
        this.em = em;
        this.tokenPath = tokenPath;
    }

    void close(){
        em.close();
    }

    @Override
    public synchronized void updateUserName(Long userId, String name) {
        Optional<User> user = getUser(userId);

        if (user.isEmpty() ||
                user.get().getUserName() == null && name == null ||
                user.get().getUserName() != null && user.get().getUserName().equals(name))
            return;
        user.get().setUserName(name);
        em.getTransaction().begin();
        em.merge(user.get());
        em.getTransaction().commit();
    }

    @Override
    public synchronized void updateEmail(Long userId, String email) throws InvalidUserActionException {
        Optional<User> user = getUser(userId);

        if (user.isEmpty() || user.get().getEmail().equals(email))
            return;
        Optional<User> userWithMail = getUserByEmail(email);
        if (userWithMail.isPresent())
            throw new InvalidUserActionException("User with mail address already exists: mail " + email);
        user.get().setEmail(email);
        em.getTransaction().begin();
        em.merge(user.get());
        em.getTransaction().commit();
    }

    @Override
    public List<User> getUsers() {
        List<User> users = em.createQuery("SELECT u FROM User u", User.class)
                .getResultList();
        return users.stream().filter(u -> u.getId() != 0).toList();
    }

    @Override
    public List<Profile> getProfiles(long userId) {
        List<Profile> profiles;
            profiles = em.createQuery("SELECT p FROM Profile p WHERE p.profileId.user.id = :userId", Profile.class)
                    .setParameter("userId", userId)
                    .getResultList();
        return profiles;
    }

    @Override
    public synchronized User addUser(long id, String email, String userName, Integer device) throws DuplicateUserIdException, InvalidUserActionException{

        Optional<User> userOpt = getUser(id);
        if (userOpt.isPresent())
            throw new DuplicateUserIdException("User with id already exists: id " + id);
        userOpt = getUserByEmail(email);
        if (userOpt.isPresent())
            throw new InvalidUserActionException("User with mail already exists: mail " + email);
        em.getTransaction().begin();
        User user = new User(email, userName, device);
        user.setId(id);
        ProfileId profileId = new ProfileId(0, user);
        Profile profile = new Profile();
        profile.setProfileId(profileId);
        VectorClock clock = new VectorClock(device);
        user.setClock(clock.jsonify());
        //user.setProfile(profile.getId());
        //user.setProfile(profile.getId());
        em.persist(user);
        em.persist(profile);
        em.getTransaction().commit();

        return user;
    }

    @Override
    public synchronized Profile addProfile(long userId) throws InvalidUserActionException {
        return addProfile(userId, null);

    }

    @Override
    public synchronized Profile addProfile(long userId, String name) throws InvalidUserActionException {
        if (getUser(userId).isEmpty())
            throw new InvalidUserActionException("User does not exist");
        em.getTransaction().begin();
        User user = em.createQuery("SELECT u FROM User u WHERE u.id = :userId", User.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
        long count = em.createQuery("SELECT COUNT(p) FROM Profile p WHERE p.profileId.user.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        Profile profile = new Profile();
        ProfileId profileId = new ProfileId((int) count, user);
        profile.setProfileId(profileId);
        profile.setName(name);

        em.persist(profile);
        em.getTransaction().commit();
        return profile;
    }

    @Override
    public synchronized void removeUser(long userId) throws InvalidUserActionException {
        if (userId == 0)
            throw new InvalidUserActionException("Cannot remove default user.");
        em.getTransaction().begin();
        em.createQuery("DELETE FROM Profile p WHERE p.profileId.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        em.createQuery("DELETE FROM User u WHERE u.id= :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        em.getTransaction().commit();

    }

    @Override
    public synchronized void removeProfile(long userId, int profileId) throws InvalidUserActionException {

        if (userId == 0 && profileId == 0)
            throw new InvalidUserActionException("Cannot remove the default profile for user");

        List<Profile> profiles = getProfiles(userId);
        if (profiles.size() == 1)
            throw new InvalidUserActionException("Cannot remove last profile for user " + userId);

        em.getTransaction().begin();
        em.createQuery("DELETE FROM Entry e WHERE e.userId = :userId AND e.profile = :profileId")
                .setParameter("userId", userId)
                .setParameter("profileId", profileId)
                .executeUpdate();
        em.createQuery("DELETE FROM Profile p WHERE p.profileId.user.id = :userId AND p.profileId.id= :profileId")
                .setParameter("userId", userId)
                .setParameter("profileId", profileId)
                .executeUpdate();
        em.getTransaction().commit();

    }

    @Override
    public Optional<User> getUserByEmail(String email) {

        List<User> results = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getResultList();

        if (results.isEmpty())
            return Optional.empty();
        else
            return Optional.of(results.getFirst());
    }

    @Override
    public Optional<User> getUser(long id) {

        List<User> users = em.createQuery("SELECT u FROM User u WHERE u.id = :id", User.class)
                .setParameter("id", id)
                .getResultList();

        return users.isEmpty() ? Optional.empty() : Optional.of(users.getFirst());
    }

    @Override
    public Optional<Profile> getProfile(long userId, long id) {

        List<Profile> results = em.createQuery("SELECT p FROM Profile p WHERE p.profileId.user.id = :userId AND p.profileId.id = :id", Profile.class)
                    .setParameter("userId", userId)
                    .setParameter("id", id)
                    .getResultList();

        if (results.isEmpty())
            return Optional.empty();
        else
            return Optional.of(results.getFirst());

    }

    @Override
    public synchronized void enableUser(long userId) {
        Optional<User> user = getUser(userId);
        if (user.isEmpty())
            return;
        user.get().setEnabled(true);
        em.getTransaction().begin();
        em.merge(user.get());
        em.getTransaction().commit();
    }
    @Override
    public synchronized void setDevice(long userId, int id) {
        Optional<User> user = getUser(userId);
        if (user.isEmpty())
            return;
        user.get().setDevice(id);
        em.getTransaction().begin();
        em.merge(user.get());
        em.getTransaction().commit();

    }
    @Override
    public synchronized void setClock(long userId, VectorClock clock) {

        Optional<User> user = getUser(userId);
        if (user.isEmpty())
            return;
        user.get().setClock(clock.jsonify());
        em.getTransaction().begin();
        em.merge(user.get());
        em.getTransaction().commit();

    }
    @Override
    public synchronized void addToQueue(User user, ZenServerMessage message) {

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
        //TODO DO NOT RETURN ITEMS OF OTHER USERS
        List<ZenServerMessage> result = new ArrayList<>();
        em.createQuery("SELECT qi FROM QueueItem qi", QueueItem.class).getResultStream().forEach(qi -> {
            List<Object> args = new ArrayList<>(qi.getArguments());
            ZenServerMessage zm = new ZenServerMessage(qi.getType(), args, new VectorClock(qi.getClock()), qi.getTimeStamp());
            result.add(zm);
        });
        return result;
    }

    @Override
    public synchronized void clearQueue(long userId) {

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
    public synchronized void setToken(long user, String token) {
        this.token = token;
        try {
            Files.write(Path.of((tokenPath == null ? "" : tokenPath + "/") + user + "_token"), token.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
