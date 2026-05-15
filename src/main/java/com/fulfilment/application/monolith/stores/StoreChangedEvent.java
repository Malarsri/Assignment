package com.fulfilment.application.monolith.stores;

public class StoreChangedEvent {
    private Store store;
    private ChangeType changeType;

    public StoreChangedEvent(Store store, ChangeType changeType) {
        this.store = store;
        this.changeType = changeType;
    }

    public Store getStore() {
        return store;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public enum ChangeType {
        CREATED,
        UPDATED,
        DELETED
    }
}
