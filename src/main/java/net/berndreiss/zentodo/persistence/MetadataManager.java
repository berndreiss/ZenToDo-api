package net.berndreiss.zentodo.persistence;

import jakarta.persistence.EntityManager;
import net.berndreiss.zentodo.data.MetadataManagerI;

//TODO write TEST cases
/**
 * Implementation of the DatabaseOpsI interface using JPA.
 */
public class MetadataManager implements MetadataManagerI {

    private final EntityManager em;

    /**
     * Create new instance of the database operations class.
     *
     * @param em the entity manager for interacting with the database
     */
    public MetadataManager(EntityManager em) {
        this.em = em;
    }

    void close() {
        em.close();
    }

    @Override
    public void setTimeDelay(long delay) {
        //TODO implement

    }

    @Override
    public long getTimeDelay() {
        //TODO implement
        return 0;
    }

    @Override
    public void setLastUser(long user) {
        //TODO implement

    }

    @Override
    public long getLastUser() {
        //TODO implement
        return 0;
    }

    @Override
    public void setVersion(String version) {
        //TODO implement
    }

    @Override
    public String getVersion() {
        //TODO implement
        return "";
    }
}
