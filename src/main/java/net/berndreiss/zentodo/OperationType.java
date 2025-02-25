package net.berndreiss.zentodo;

import net.berndreiss.zentodo.data.OperationField;

/*

0 ID
1 TASK
2 FOCUS
3 DROPPED
4 LIST
5 LIST_POSITION
6 REMINDER_DATE
7 RECURRENCE
8 POSITION
9 COLOR

 */
public enum OperationType {
    POST(new OperationField[]{OperationField.ID, OperationField.TASK, OperationField.FOCUS, OperationField.DROPPED,
            OperationField.LIST, OperationField.LIST_POSITION, OperationField.REMINDER_DATE, OperationField.RECURRENCE, OperationField.RECURRENCE}),
    ADD_NEW_ENTRY(new OperationField[]{OperationField.ID, OperationField.TASK}),
    DELETE(new OperationField[]{OperationField.ID}),
    SWAP (new OperationField[]{OperationField.ID, OperationField.POSITION}),
    SWAP_LIST (new OperationField[]{OperationField.ID, OperationField.LIST_POSITION}),
    UPDATE_TASK (new OperationField[]{OperationField.ID, OperationField.TASK}),
    UPDATE_FOCUS (new OperationField[]{OperationField.ID, OperationField.FOCUS}),
    UPDATE_DROPPED (new OperationField[]{OperationField.ID, OperationField.DROPPED}),
    UPDATE_LIST (new OperationField[]{OperationField.ID, OperationField.LIST}),
    UPDATE_REMINDER_DATE (new OperationField[]{OperationField.ID, OperationField.REMINDER_DATE}),
    UPDATE_RECURRENCE (new OperationField[]{OperationField.ID, OperationField.RECURRENCE}),
    UPDATE_LIST_COLOR (new OperationField[]{OperationField.LIST, OperationField.COLOR}),
    UPDATE_USER_NAME(new OperationField[]{OperationField.ID, OperationField.USERNAME}),
    UPDATE_MAIL(new OperationField[]{OperationField.ID, OperationField.MAIL}),
    ;

    public final OperationField[] args;

    OperationType(OperationField[] args){
        this.args = args;
    }
}
