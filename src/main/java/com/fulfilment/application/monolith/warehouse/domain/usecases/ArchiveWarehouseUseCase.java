package com.fulfilment.application.monolith.warehouse.domain.usecases;

import com.fulfilment.application.monolith.warehouse.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouse.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouse.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void archive(Warehouse warehouse) {
    // TODO implement this method

    warehouseStore.update(warehouse);
  }
}
