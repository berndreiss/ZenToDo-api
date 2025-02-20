package net.berndreiss.zentodo.api;

/**
 * TODO IMPLEMENT DESCRIPTION
 */
public interface DatabaseClient extends Database{

    /**
     *
     * @param entry
     * @param id
     */
    void updateId(int entry, int id);

}
