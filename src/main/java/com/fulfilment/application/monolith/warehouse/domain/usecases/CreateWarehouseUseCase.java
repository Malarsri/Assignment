package com.fulfilment.application.monolith.warehouse.domain.usecases;

import com.fulfilment.application.monolith.warehouse.domain.models.Location;
import com.fulfilment.application.monolith.warehouse.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouse.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouse.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouse.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.util.Optional;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    // Business Unit Code Verification: Ensure that the specified business unit code doesn't already exist
    Warehouse existingWarehouse = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existingWarehouse != null) {
      throw new WebApplicationException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' already exists.",
          400);
    }

    // Location Validation: Confirm that the warehouse location is valid
    Optional<Location> location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location.isEmpty()) {
      throw new WebApplicationException(
          "Location '" + warehouse.location + "' does not exist.", 400);
    }

    Location resolvedLocation = location.get();

    // Warehouse Creation Feasibility: Check if a new warehouse can be created at the specified location
    long warehouseCountAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(warehouse.location) && w.archivedAt == null)
            .count();
    if (warehouseCountAtLocation >= resolvedLocation.maxNumberOfWarehouses) {
      throw new WebApplicationException(
          "Maximum number of warehouses (" + resolvedLocation.maxNumberOfWarehouses
              + ") has been reached for location '" + warehouse.location + "'.",
          400);
    }

    // Capacity and Stock Validation: Validate the warehouse capacity
    int totalCapacityAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(warehouse.location) && w.archivedAt == null)
            .mapToInt(w -> w.capacity)
            .sum();

    if (warehouse.capacity > resolvedLocation.maxCapacity - totalCapacityAtLocation) {
      throw new WebApplicationException(
          "Warehouse capacity (" + warehouse.capacity
              + ") exceeds maximum available capacity at location '" + warehouse.location + "'.",
          400);
    }

    if (warehouse.stock > warehouse.capacity) {
      throw new WebApplicationException(
          "Warehouse stock (" + warehouse.stock + ") exceeds its capacity ("
              + warehouse.capacity + ").",
          400);
    }

    // if all validations passed, create the warehouse
    warehouseStore.create(warehouse);
  }
}
