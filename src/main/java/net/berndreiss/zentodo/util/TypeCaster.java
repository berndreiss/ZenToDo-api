package net.berndreiss.zentodo.util;


public class TypeCaster<T> {
    private final Class<T> type;

    public TypeCaster(Class<T> type) {
        this.type = type;
    }

    public T cast(Object obj) {
        return type.cast(obj);
    }

    public boolean isInstance(Object obj) {
        return type.isInstance(obj);
    }

}
