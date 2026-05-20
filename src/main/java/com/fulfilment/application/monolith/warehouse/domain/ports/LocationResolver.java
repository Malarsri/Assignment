package com.fulfilment.application.monolith.warehouse.domain.ports;

import com.fulfilment.application.monolith.warehouse.domain.models.Location;

import java.util.Optional;

public interface LocationResolver {
  Optional<Location> resolveByIdentifier(String identifier);
}
