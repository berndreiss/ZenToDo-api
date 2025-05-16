package net.berndreiss.zentodo.data;

public class Database {

    private final EntryManagerI entryManager;

    private final UserManagerI userManager;

    private final DatabaseOpsI databaseOps;

    private final ListManagerI listManager;

    public Database(EntryManagerI entryManager, UserManagerI userManager, DatabaseOpsI databaseOps, ListManagerI listManager){
        this.entryManager = entryManager;
        this.userManager = userManager;
        this.databaseOps = databaseOps;
        this.listManager = listManager;
    }

    public EntryManagerI getEntryManager(){return entryManager;};
    public UserManagerI getUserManager(){return userManager;};
    public DatabaseOpsI getDatabaseOps(){return databaseOps;};
    public ListManagerI getListManager(){return listManager;}

    public void close(){};
}
