package net.berndreiss.zentodo.persistence;

import jakarta.persistence.EntityManagerFactory;
import net.berndreiss.zentodo.data.Database;
import net.berndreiss.zentodo.data.Profile;
import net.berndreiss.zentodo.data.User;
import net.berndreiss.zentodo.exceptions.DuplicateIdException;
import net.berndreiss.zentodo.exceptions.InvalidActionException;
import net.berndreiss.zentodo.util.ClientStub;

import java.util.List;
import java.util.Optional;


/**
 * Implementation of the Database interface.
 */
public class DbHandler extends Database implements AutoCloseable {

    public EntityManagerFactory emf;

    /**
     * Create a new instance of the database handler.
     *
     * @param emf       the entity manager factory for retrieving entity manager for interacting with the database
     * @param tokenPath the path where tokens are stored
     */
    public DbHandler(EntityManagerFactory emf, String tokenPath) {
        super(new TaskManager(emf.createEntityManager()), new UserManager(emf.createEntityManager(), tokenPath), new ListManager(emf.createEntityManager()), new MetadataManager(emf.createEntityManager()));
        this.emf = emf;

        //Get the default user and create it if it does not exist
        Optional<User> user = getUserManager().getUser(0L);
        if (user.isPresent()) {
            List<Profile> profiles = getUserManager().getProfiles(0);
            if (profiles.isEmpty()) {
                try {
                    getUserManager().addProfile(0);
                } catch (InvalidActionException _) {
                }//should never occur
            }
            return;
        }
        try {
            getUserManager().addUser(0L, "default213498jfpq0r9u3@deqaowroiqur2urfault.net", "Default User", 0);
        } catch (DuplicateIdException | InvalidActionException e) {
            ClientStub.logger.error("There was a problem creating the default user.", e);
        }
    }

    @Override
    public void close() {
        ((TaskManager) getTaskManager()).close();
        ((UserManager) getUserManager()).close();
        ((MetadataManager) getMetadataManager()).close();
        ((ListManager) getListManager()).close();
        emf.close();
    }
}
