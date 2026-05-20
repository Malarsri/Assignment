package com.fulfilment.application.monolith.warehouse.domain.ports;

import com.fulfilment.application.monolith.warehouse.domain.models.Warehouse;

public interface ArchiveWarehouseOperation {
  void archive(Warehouse warehouse);
}
