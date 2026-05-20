package com.fulfilment.application.monolith.warehouse.domain.usecases;

import com.fulfilment.application.monolith.warehouse.domain.models.Location;
import com.fulfilment.application.monolith.warehouse.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouse.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouse.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouse.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    // Find the old warehouse with the same business unit code
    Warehouse oldWarehouse = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (oldWarehouse == null) {
      throw new WebApplicationException(
          "Warehouse with business unit code '" + newWarehouse.businessUnitCode
              + "' does not exist.",
          404);
    }

    // Location Validation: Confirm that the new warehouse location is valid
    Optional<Location> location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location.isEmpty()) {
      throw new WebApplicationException(
          "Location '" + newWarehouse.location + "' does not exist.", 400);
    }

    Location resolvedLocation = location.get();

    // Capacity Accommodation: Ensure the new warehouse's capacity can accommodate the stock
    if (newWarehouse.capacity < oldWarehouse.stock) {
      throw new WebApplicationException(
          "New warehouse capacity (" + newWarehouse.capacity
              + ") cannot accommodate the existing stock (" + oldWarehouse.stock + ").",
          400);
    }

    // Stock Matching: Confirm that the stock of the new warehouse matches the stock of the previous warehouse
    if (!newWarehouse.stock.equals(oldWarehouse.stock)) {
      throw new WebApplicationException(
          "New warehouse stock (" + newWarehouse.stock + ") must match the existing warehouse stock ("
              + oldWarehouse.stock + ").",
          400);
    }

    // Additional validation: Check new warehouse capacity against location's max capacity
    int totalCapacityAtLocationWithoutOld =
        warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(newWarehouse.location)
                && w.archivedAt == null
                && !w.businessUnitCode.equals(newWarehouse.businessUnitCode))
            .mapToInt(w -> w.capacity)
            .sum();

    if (newWarehouse.capacity > resolvedLocation.maxCapacity - totalCapacityAtLocationWithoutOld) {
      throw new WebApplicationException(
          "Warehouse capacity (" + newWarehouse.capacity
              + ") exceeds maximum available capacity at location '" + newWarehouse.location + "'.",
          400);
    }

    // Archive the old warehouse
    oldWarehouse.archivedAt = LocalDateTime.now();
    warehouseStore.update(oldWarehouse);

    // Create the new warehouse
    warehouseStore.create(newWarehouse);
  }
}
