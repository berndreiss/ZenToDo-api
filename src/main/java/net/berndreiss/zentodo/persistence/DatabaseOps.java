package net.berndreiss.zentodo.persistence;

import jakarta.persistence.EntityManager;
import net.berndreiss.zentodo.data.DatabaseOpsI;

/**
 * Implementation of the DatabaseOpsI interface using JPA.
 */
public class DatabaseOps implements DatabaseOpsI {

    private final EntityManager em;

    /**
     * Create new instance of the database operations class.
     * @param em the entity manager for interacting with the database
     */
    public DatabaseOps(EntityManager em){
        this.em = em;
    }

    void close(){
        em.close();
    }
    @Override
    public void setTimeDelay(long delay) {

    }
}
