package com.fulfilment.application.monolith.warehouse.api;

import com.fulfilment.application.monolith.warehouse.api.beans.Warehouse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("warehouse")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface WarehouseResource {

  @GET
  public List<Warehouse> listAllWarehousesUnits();

  @POST
  public Warehouse createANewWarehouseUnit(Warehouse data);

  @GET
  @Path("{id}")
  public Warehouse getAWarehouseUnitByID(@PathParam("id") String id);

  @DELETE
  @Path("{id}")
  public void archiveAWarehouseUnitByID(@PathParam("id") String id);

  @POST
  @Path("{businessUnitCode}/replacement")
  public Warehouse replaceTheCurrentActiveWarehouse(
      @PathParam("businessUnitCode") String businessUnitCode, Warehouse data);
}
