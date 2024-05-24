package guru.sfg.brewery.springframework.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import guru.sfg.brewery.model.BeerDto;
import guru.sfg.brewery.springframework.domain.BeerOrder;
import guru.sfg.brewery.springframework.domain.BeerOrderLine;
import guru.sfg.brewery.springframework.domain.BeerOrderStatusEnum;
import guru.sfg.brewery.springframework.domain.Customer;
import guru.sfg.brewery.springframework.repositories.BeerOrderRepository;
import guru.sfg.brewery.springframework.repositories.CustomerRepository;
import guru.sfg.brewery.springframework.services.beer.BeerServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;


@ExtendWith(WireMockExtension.class)
@SpringBootTest
public class BeerOrderManagerImplIT {

    @Autowired
    BeerOrderManager beerOrderManager;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    WireMockServer wireMockServer;

    @Autowired
    ObjectMapper objectMapper;

    Customer testCustomer;

    UUID beerId = UUID.randomUUID();

    @TestConfiguration
    static class RestTemplateBuilderProvider{

        @Bean(destroyMethod = "stop")
        public WireMockServer wireMockServer(){
            WireMockServer wireMockServer = with(wireMockConfig().port(8083));
            wireMockServer.start();
            return wireMockServer;
        }

    }

    @BeforeEach
    void setUp() {
        testCustomer = customerRepository.save(Customer.builder()
                .customerName("Test Customer")
                .build());
    }

    @Test
    void newToAllocated() throws JsonProcessingException, InterruptedException {
        BeerDto beerDto = BeerDto.builder()
                        .id(beerId)
                        .upc("12345")
                        .build();

        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
        wireMockConfig();

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        Assertions.assertNotNull(savedBeerOrder);

        //TODO: teste nao funciona, mas se for por passo a passo no debugger funciona
/*        await().untilAsserted( () -> {
            Thread.sleep(2000);
            BeerOrder foundBeer = beerOrderRepository.findById(beerOrder.getId()).get();

            Assertions.assertEquals(BeerOrderStatusEnum.ALLOCATED, foundBeer.getOrderStatus());
        });*/


        BeerOrder savedBeerOrder2 = beerOrderRepository.findById(beerOrder.getId()).get();
        Assertions.assertEquals(BeerOrderStatusEnum.ALLOCATED, savedBeerOrder2.getOrderStatus());

    }

    public BeerOrder createBeerOrder() {
        BeerOrder beerOrder = BeerOrder.builder()
                .customer(testCustomer)
                .build();

        Set<BeerOrderLine> beerOrderLines = new HashSet<>();
        beerOrderLines.add(BeerOrderLine.builder()
                .beerId(beerId)
                .upc("12345")
                .orderQuantity(1)
                .beerOrder(beerOrder)
                .build());

        beerOrder.setBeerOrderLines(beerOrderLines);

        return beerOrder;
    }
}
