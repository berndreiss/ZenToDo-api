package net.berndreiss.zentodo.persistence;

import jakarta.persistence.EntityManagerFactory;
import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.data.Profile;
import net.berndreiss.zentodo.data.User;
import net.berndreiss.zentodo.exceptions.DuplicateIdException;
import net.berndreiss.zentodo.exceptions.InvalidActionException;

import java.util.List;
import java.util.Optional;

/**
 * TODO DESCRIBE
 */
public class DbHandler extends Database implements  AutoCloseable {

    public EntityManagerFactory emf;

    /**
     * TODO DESCRIBE
     *
     */
    public DbHandler(EntityManagerFactory emf, String tokenPath) {
        super(new EntryManager(emf.createEntityManager()), new UserManager(emf.createEntityManager(), tokenPath), new DatabaseOps(emf.createEntityManager()), new ListManager(emf.createEntityManager()));
        this.emf = emf;

        Optional<User> user = getUserManager().getUser(0L);
        if (user.isPresent()) {
            List<Profile> profiles = getUserManager().getProfiles(0);
            if (profiles.isEmpty()) {
                try{
                getUserManager().addProfile(0);}catch (InvalidActionException _){}//should never occur
            }
            return;
        }
        try {
            getUserManager().addUser(0L, "default213498jfpq0r9u3@deqaowroiqur2urfault.net", "Default User", 0);
        } catch (DuplicateIdException | InvalidActionException _) {}
    }

    @Override
    public void close() {

        ((EntryManager) getEntryManager()).close();
        ((UserManager) getUserManager()).close();
        ((DatabaseOps) getDatabaseOps()).close();
        ((ListManager) getListManager()).close();
        emf.close();
    }
}
