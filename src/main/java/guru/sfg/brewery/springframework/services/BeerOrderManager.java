package guru.sfg.brewery.springframework.services;

import guru.sfg.brewery.springframework.domain.BeerOrder;

public interface BeerOrderManager {
    BeerOrder newBeerOrder(BeerOrder beerOrder);

}
