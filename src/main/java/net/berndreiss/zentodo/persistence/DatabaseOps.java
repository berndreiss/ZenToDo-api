package net.berndreiss.zentodo.persistence;

import jakarta.persistence.EntityManager;
import net.berndreiss.zentodo.data.DatabaseOpsI;

public class DatabaseOps implements DatabaseOpsI {

    private EntityManager em;

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
