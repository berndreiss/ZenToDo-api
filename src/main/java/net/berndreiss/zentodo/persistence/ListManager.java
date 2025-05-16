package net.berndreiss.zentodo.persistence;

import jakarta.persistence.EntityManager;
import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.ListManagerI;

import java.util.List;

public class ListManager implements ListManagerI {
    private EntityManager em;

    public ListManager(EntityManager em){
        this.em = em;
    }
    public void close(){
        em.close();
    }
    @Override
    public synchronized void updateList(long userId, long profile, long entryId, String value, int position) {

    }

    @Override
    public List<Entry> getList(long userId, long profile, String list) {
        return List.of();
    }

    @Override
    public synchronized void swapListEntries(long userId, long profile, long entryId, int position) {

    }

    @Override
    public synchronized void updateListColor(long userId, long profile, String list, String color) {

    }
}
