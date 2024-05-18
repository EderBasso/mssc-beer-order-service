package guru.sfg.brewery.springframework.services;

import guru.sfg.brewery.springframework.domain.BeerOrder;

import java.util.UUID;

public interface BeerOrderManager {
    BeerOrder newBeerOrder(BeerOrder beerOrder);

    void processValidationResult(UUID orderId, Boolean isValid);

}
