package net.berndreiss.zentodo.data;

public class Database {

    private final EntryManagerI entryManager;

    private final UserManagerI userManager;

    private final DatabaseOpsI databaseOps;

    public Database(EntryManagerI entryManager, UserManagerI userManager, DatabaseOpsI databaseOps){
        this.entryManager = entryManager;
        this.userManager = userManager;
        this.databaseOps = databaseOps;
    }

    public EntryManagerI getEntryManager(){return entryManager;};
    public UserManagerI getUserManager(){return userManager;};
    public DatabaseOpsI getDatabaseOps(){return databaseOps;};

}
