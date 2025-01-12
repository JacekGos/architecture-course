package com.food.ordering.system.order.service.messaging.publisher.kafka;

import com.food.ordering.system.kafka.order.avro.model.PaymentRequestAvroModel;
import com.food.ordering.system.kafka.producer.service.KafkaProduer;
import com.food.ordering.system.order.service.domain.config.OrderServiceConfigData;
import com.food.ordering.system.order.service.domain.event.OrderCreatedEvent;
import com.food.ordering.system.order.service.domain.ports.output.message.publisher.payment.OrderCreatedPaymentRequestMessagePublisher;
import com.food.ordering.system.order.service.messaging.mapper.OrderMessagingDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Slf4j
@Component
public class CreateOrderKafkaMessagePublisher implements OrderCreatedPaymentRequestMessagePublisher {

    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final OrderServiceConfigData orderServiceConfigData;
    private final KafkaProduer<String, PaymentRequestAvroModel> kafkaProduer;
    private final OrderKafkaMessageHandler orderKafkaMessageHandler;

    public CreateOrderKafkaMessagePublisher(OrderMessagingDataMapper orderMessagingDataMapper,
                                            OrderServiceConfigData orderServiceConfigData,
                                            KafkaProduer<String, PaymentRequestAvroModel> kafkaProduer,
                                            OrderKafkaMessageHandler orderKafkaMessageHandler) {
        this.orderMessagingDataMapper = orderMessagingDataMapper;
        this.orderServiceConfigData = orderServiceConfigData;
        this.kafkaProduer = kafkaProduer;
        this.orderKafkaMessageHandler = orderKafkaMessageHandler;
    }

    @Override
    public void publish(OrderCreatedEvent domainEvent) {

        String orderId = domainEvent.getOrder().getId().getValue().toString();
        log.info("Received OrderCreatedEvent for orderId: {}", orderId);

        try {
            PaymentRequestAvroModel paymentRequestAvroModel =
                    orderMessagingDataMapper.orderCreatedEventToPaymentRequestAvroModel(domainEvent);

            kafkaProduer.send(
                    orderServiceConfigData.getPaymentRequestTopicName(),
                    orderId,
                    paymentRequestAvroModel,
                    orderKafkaMessageHandler.getKafkaCallback(orderServiceConfigData.getPaymentResponseTopicName(), paymentRequestAvroModel));

            log.info("PaymentRequestAvroModel sent to kafka for order id: {}", paymentRequestAvroModel.getOrderId());
        } catch (Exception e) {
            log.error("Error while sending PaymentRequestAvroModel message to kafka with order id: {}, error: {}",
                    orderId, e.getMessage());
        }
    }
}
