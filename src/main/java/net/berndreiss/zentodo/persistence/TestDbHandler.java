package net.berndreiss.zentodo.persistence;

import net.berndreiss.zentodo.data.*;

import java.util.Optional;

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

        Optional<User> user = getUserManager().getUser(0L);
        System.out.println(user.isPresent());

        if (user.isPresent())
            return;
        getUserManager().addUser(0L, "default@default.net", "Default User", 0);
    }

    public void close(){
    }



}
