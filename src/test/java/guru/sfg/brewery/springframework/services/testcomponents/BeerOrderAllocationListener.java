package guru.sfg.brewery.springframework.services.testcomponents;

import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import guru.sfg.brewery.springframework.config.JmsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderAllocationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(Message msg){

        AllocateOrderRequest request = (AllocateOrderRequest) msg.getPayload();

        request.getBeerOrder().getBeerOrderLines().forEach(beerOrderLineDto -> {
            beerOrderLineDto.setAllocatedQuantity(beerOrderLineDto.getOrderQuantity());
        });

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
                AllocateOrderResult.builder()
                        .beerOrder(request.getBeerOrder())
                        .allocationError(false)
                        .pendingInventory(false)
                        .build());


    }
}
