package net.berndreiss.zentodo.data;

import jakarta.persistence.criteria.CriteriaBuilder;

import java.lang.reflect.Type;
import java.time.LocalDate;

public enum OperationField {
    USER(Long.class),
    ID(long.class),
    TASK(String.class),
    FOCUS(boolean.class),
    DROPPED(boolean.class),
    LIST(String.class),
    LIST_POSITION(Integer.class),
    REMINDER_DATE(LocalDate.class),
    RECURRENCE(String.class),
    POSITION(int.class),
    COLOR(String.class),
    MAIL(String.class),
    USERNAME(String.class);

    public final Type type;

    OperationField(Type type){
        this.type = type;
    }

}
