package com.fulfilment.application.monolith.warehouse.adapters.database;

import com.fulfilment.application.monolith.warehouse.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouse.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse dbWarehouse = toDatabaseWarehouse(warehouse);
    this.persist(dbWarehouse);
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse dbWarehouse = this.find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (dbWarehouse != null) {
      dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
      dbWarehouse.location = warehouse.location;
      dbWarehouse.capacity = warehouse.capacity;
      dbWarehouse.stock = warehouse.stock;
      dbWarehouse.createdAt = warehouse.createdAt;
      dbWarehouse.archivedAt = warehouse.archivedAt;
      this.persist(dbWarehouse);
    }
  }

  @Override
  public void remove(Warehouse warehouse) {
    DbWarehouse dbWarehouse = this.find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (dbWarehouse != null) {
      this.delete(dbWarehouse);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse dbWarehouse = this.find("businessUnitCode", buCode).firstResult();
    if (dbWarehouse != null) {
      return dbWarehouse.toWarehouse();
    }
    return null;
  }

  private DbWarehouse toDatabaseWarehouse(Warehouse warehouse) {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    return dbWarehouse;
  }
}
