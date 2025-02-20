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

    /**
     *
     * @param id
     * @param position
     */
    void updatePosition(int id, int position);

    /**
     *
     * @param id
     * @param position
     */
    void updateListPosition(int id, int position);
}
