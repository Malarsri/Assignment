package com.fulfilment.application.monolith.warehouse.api;

import com.fulfilment.application.monolith.warehouse.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouse.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouse.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouse.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouse.domain.ports.ReplaceWarehouseOperation;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;

import java.time.LocalDateTime;
import java.util.List;


@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

    @Inject
    private WarehouseRepository warehouseRepository;

    @Inject
    private CreateWarehouseOperation createWarehouseOperation;

    @Inject
    private ReplaceWarehouseOperation replaceWarehouseOperation;

    @Inject
    private ArchiveWarehouseOperation archiveWarehouseOperation;

    @Override
    public List<com.fulfilment.application.monolith.warehouse.api.beans.Warehouse> listAllWarehousesUnits() {
        return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
    }

    @Override
    @Transactional
    public com.fulfilment.application.monolith.warehouse.api.beans.Warehouse createANewWarehouseUnit(@NotNull com.fulfilment.application.monolith.warehouse.api.beans.Warehouse data) {
        var domainWarehouse = new Warehouse();
        domainWarehouse.businessUnitCode = data.getBusinessUnitCode();
        domainWarehouse.location = data.getLocation();
        domainWarehouse.capacity = data.getCapacity();
        domainWarehouse.stock = data.getStock();
        domainWarehouse.createdAt = LocalDateTime.now();

        createWarehouseOperation.create(domainWarehouse);

        return toWarehouseResponse(domainWarehouse);
    }

    @Override
    public com.fulfilment.application.monolith.warehouse.api.beans.Warehouse getAWarehouseUnitByID(String id) {
        Warehouse warehouse = warehouseRepository.findByBusinessUnitCode(id);
        if (warehouse == null) {
            throw new WebApplicationException("Warehouse with id " + id + " does not exist.", 404);
        }
        return toWarehouseResponse(warehouse);
    }

    @Override
    @Transactional
    public void archiveAWarehouseUnitByID(String id) {
        Warehouse warehouse = warehouseRepository.findByBusinessUnitCode(id);
        if (warehouse == null) {
            throw new WebApplicationException("Warehouse with id " + id + " does not exist.", 404);
        }
        warehouse.archivedAt = LocalDateTime.now();
        archiveWarehouseOperation.archive(warehouse);
    }

    @Override
    @Transactional
    public com.fulfilment.application.monolith.warehouse.api.beans.Warehouse replaceTheCurrentActiveWarehouse(
            String businessUnitCode, @NotNull com.fulfilment.application.monolith.warehouse.api.beans.Warehouse data) {
        var newWarehouse = new Warehouse();
        newWarehouse.businessUnitCode = data.getBusinessUnitCode();
        newWarehouse.location = data.getLocation();
        newWarehouse.capacity = data.getCapacity();
        newWarehouse.stock = data.getStock();
        newWarehouse.createdAt = LocalDateTime.now();

        replaceWarehouseOperation.replace(newWarehouse);

        return toWarehouseResponse(newWarehouse);
    }

    private com.fulfilment.application.monolith.warehouse.api.beans.Warehouse toWarehouseResponse(Warehouse warehouse) {
        var response = new com.fulfilment.application.monolith.warehouse.api.beans.Warehouse();
        response.setBusinessUnitCode(warehouse.businessUnitCode);
        response.setLocation(warehouse.location);
        response.setCapacity(warehouse.capacity);
        response.setStock(warehouse.stock);

        return response;
    }
}
