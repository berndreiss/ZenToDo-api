package net.berndreiss.zentodo.persistence;

import net.berndreiss.zentodo.data.*;

/**
 * TODO DESCRIBE
 */
public class TestDbHandler extends Database {
    public String persistenceUnit;

    /**
     * TODO DESCRIBE
     * @param persistenceUnit
     */
    public TestDbHandler (String persistenceUnit, String tokenPath){
        super(new EntryManager(persistenceUnit), new UserManager(persistenceUnit, tokenPath), new DatabaseOps());
        this.persistenceUnit = persistenceUnit;
        getUserManager().addProfile(null);
    }

    public void close(){
    }



}
