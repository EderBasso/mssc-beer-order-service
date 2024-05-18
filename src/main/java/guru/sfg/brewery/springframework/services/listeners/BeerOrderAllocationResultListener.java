package guru.sfg.brewery.springframework.services.listeners;

import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import guru.sfg.brewery.springframework.config.JmsConfig;
import guru.sfg.brewery.springframework.services.BeerOrderManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class BeerOrderAllocationResultListener {
    private final JmsTemplate jmsTemplate;
    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE)
    public void listen(AllocateOrderResult result) {
        final BeerOrderDto orderDto = result.getBeerOrder();
        Boolean allocationError = result.getAllocationError();
        Boolean pendingInventory = result.getPendingInventory();

        if(allocationError) {
            beerOrderManager.beerOrderAllocationFailed(orderDto);
        } else if (pendingInventory) {
            beerOrderManager.beerOrderAllocationPendingInventory(orderDto);
        }else{
            beerOrderManager.beerOrderAllocationPassed(orderDto);
        }
    }
}
