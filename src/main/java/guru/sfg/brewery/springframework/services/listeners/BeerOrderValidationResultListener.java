package guru.sfg.brewery.springframework.services.listeners;

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
public class BeerOrderValidationResultListener {
    private final JmsTemplate jmsTemplate;
    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE)
    public void listen(ValidateOrderResult result) {
        final UUID orderId = result.getOrderId();

        beerOrderManager.processValidationResult(orderId, result.getIsValid());
    }
}
