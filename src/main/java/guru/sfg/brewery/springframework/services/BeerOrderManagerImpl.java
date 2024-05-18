package guru.sfg.brewery.springframework.services;

import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.springframework.domain.BeerOrder;
import guru.sfg.brewery.springframework.domain.BeerOrderEventEnum;
import guru.sfg.brewery.springframework.domain.BeerOrderStatusEnum;
import guru.sfg.brewery.springframework.repositories.BeerOrderRepository;
import guru.sfg.brewery.springframework.statemachine.BeerOrderStateChangeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderStateChangeInterceptor stateChangeInterceptor;

    public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";
    private final BeerOrderRepository beerOrderRepository;

    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);

        sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum event) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(beerOrder);

        Message msg = MessageBuilder.withPayload(event)
                .setHeader(ORDER_ID_HEADER, beerOrder.getId().toString())
                .build();

        sm.sendEvent(msg);
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine = stateMachineFactory.getStateMachine(beerOrder.getId());

        stateMachine.stop();

        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.addStateMachineInterceptor(stateChangeInterceptor);
                    sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null));
                });

        stateMachine.start();

        return stateMachine;
    }

    @Override
    public void processValidationResult(UUID orderId, Boolean isValid) {
        BeerOrder beerOrder = beerOrderRepository.findById(orderId).orElse(null);

        if (isValid){
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);

            BeerOrder validatedOrder = beerOrderRepository.findById(orderId).orElse(null);
            sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_ORDER);

        }else{
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
        }
    }

    @Override
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
        updateAllocatedQuantity(beerOrderDto, beerOrder);
    }

    @Override
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);
        updateAllocatedQuantity(beerOrderDto, beerOrder);

    }

    private void updateAllocatedQuantity(BeerOrderDto beerOrderDto, BeerOrder beerOrder) {
        BeerOrder allocatedOrder = beerOrderRepository.getOne(beerOrderDto.getId());

        allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
            beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                if(beerOrderLine.getId().equals(beerOrderLineDto.getId())){
                    beerOrderLine.setQuantityAllocated(beerOrderLineDto.getAllocatedQuantity());
                }
            });
        });

        beerOrderRepository.saveAndFlush(beerOrder);
    }

    @Override
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED);
    }
}
