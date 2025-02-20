package net.berndreiss.zentodo.api;

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
public enum Operations {
    POST(new OperationFields[0]),
    ADD_NEW_ENTRY(new OperationFields[]{OperationFields.ID, OperationFields.TASK}),
    DELETE(new OperationFields[]{OperationFields.ID}),
    SWAP (new OperationFields[]{OperationFields.ID, OperationFields.POSITION}),
    SWAP_LIST (new OperationFields[]{OperationFields.ID, OperationFields.LIST_POSITION}),
    UPDATE_TASK (new OperationFields[]{OperationFields.ID, OperationFields.TASK}),
    UPDATE_FOCUS (new OperationFields[]{OperationFields.ID, OperationFields.FOCUS}),
    UPDATE_DROPPED (new OperationFields[]{OperationFields.ID, OperationFields.DROPPED}),
    UPDATE_LIST (new OperationFields[]{OperationFields.ID, OperationFields.LIST}),
    UPDATE_REMINDER_DATE (new OperationFields[]{OperationFields.ID, OperationFields.REMINDER_DATE}),
    UPDATE_RECURRENCE (new OperationFields[]{OperationFields.ID, OperationFields.RECURRENCE}),
    UPDATE_LIST_COLOR (new OperationFields[]{OperationFields.LIST, OperationFields.COLOR});

    public final OperationFields[] args;

    Operations(OperationFields[] args){
        this.args = args;
    }
}
