package com.fulfilment.application.monolith.warehouse.domain.ports;

import com.fulfilment.application.monolith.warehouse.domain.models.Warehouse;

public interface ReplaceWarehouseOperation {
  void replace(Warehouse warehouse);
}
