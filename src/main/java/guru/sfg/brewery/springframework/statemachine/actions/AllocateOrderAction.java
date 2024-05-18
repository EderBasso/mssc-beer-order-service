package guru.sfg.brewery.springframework.statemachine.actions;

import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.springframework.config.JmsConfig;
import guru.sfg.brewery.springframework.domain.BeerOrder;
import guru.sfg.brewery.springframework.domain.BeerOrderEventEnum;
import guru.sfg.brewery.springframework.domain.BeerOrderStatusEnum;
import guru.sfg.brewery.springframework.repositories.BeerOrderRepository;
import guru.sfg.brewery.springframework.services.BeerOrderManagerImpl;
import guru.sfg.brewery.springframework.web.mappers.BeerOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;
    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {
        String beerOrderId = (String) stateContext.getMessage().getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER);
        BeerOrder beerOrder = beerOrderRepository.findOneById(UUID.fromString(beerOrderId));

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_QUEUE, AllocateOrderRequest.builder()
                .beerOrder(beerOrderMapper.beerOrderToDto(beerOrder))
                .build());

        log.debug("Sent allocation request to queue for order id {}", beerOrderId);
    }
}