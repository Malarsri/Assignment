package com.fulfilment.application.monolith.warehouse.domain.models;

import java.time.LocalDateTime;

public class Warehouse {

  // unique identifier
  public String businessUnitCode;

  public String location;

    public void setBusinessUnitCode(String businessUnitCode) {
        this.businessUnitCode = businessUnitCode;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer capacity;

  public Integer stock;

  public LocalDateTime createdAt;

  public LocalDateTime archivedAt;
}
