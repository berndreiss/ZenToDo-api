package net.berndreiss.zentodo.data;

/**
 * Blueprint for a database class. Can be extended for additional functionality.
 */
public class Database {

    private final TaskManagerI taskManager;
    private final UserManagerI userManager;
    private final MetadataManagerI metadataManager;
    private final ListManagerI listManager;

    /**
     * Crate a new instance of the database.
     *
     * @param taskManager manages tasks
     * @param userManager manages users
     * @param listManager manages lists
     * @param metadataManager manages user independent data
     */
    public Database(TaskManagerI taskManager, UserManagerI userManager, ListManagerI listManager, MetadataManagerI metadataManager) {
        this.taskManager = taskManager;
        this.userManager = userManager;
        this.metadataManager = metadataManager;
        this.listManager = listManager;
    }

    public TaskManagerI getTaskManager() {
        return taskManager;
    }

    public UserManagerI getUserManager() {
        return userManager;
    }

    public ListManagerI getListManager() {
        return listManager;
    }

    public MetadataManagerI getMetadataManager() {
        return metadataManager;
    }

    public void close() {
    }
}
