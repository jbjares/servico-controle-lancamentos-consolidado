package br.com.exemplo.lancamentos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class MensageriaConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MensageriaConfig.class);

    @Bean
    public TopicExchange lancamentosExchange(@Value("${app.mensageria.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public DirectExchange lancamentosDeadLetterExchange(
            @Value("${app.mensageria.dead-letter-exchange}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    public Queue lancamentosFila(@Value("${app.mensageria.fila}") String fila,
                                 @Value("${app.mensageria.dead-letter-exchange}") String deadLetterExchange,
                                 @Value("${app.mensageria.dead-letter-routing-key}") String deadLetterRoutingKey) {
        return QueueBuilder.durable(fila)
                .withArgument("x-dead-letter-exchange", deadLetterExchange)
                .withArgument("x-dead-letter-routing-key", deadLetterRoutingKey)
                .build();
    }

    @Bean
    public Queue lancamentosDeadLetterFila(@Value("${app.mensageria.dead-letter-fila}") String fila) {
        return QueueBuilder.durable(fila).build();
    }

    @Bean
    public Binding lancamentosBinding(@Qualifier("lancamentosFila") Queue lancamentosFila,
                                      TopicExchange lancamentosExchange,
                                      @Value("${app.mensageria.routing-key}") String routingKey) {
        return BindingBuilder.bind(lancamentosFila).to(lancamentosExchange).with(routingKey);
    }

    @Bean
    public Binding lancamentosDeadLetterBinding(
            @Qualifier("lancamentosDeadLetterFila") Queue lancamentosDeadLetterFila,
            DirectExchange lancamentosDeadLetterExchange,
            @Value("${app.mensageria.dead-letter-routing-key}") String deadLetterRoutingKey) {
        return BindingBuilder.bind(lancamentosDeadLetterFila)
                .to(lancamentosDeadLetterExchange)
                .with(deadLetterRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter,
                                         @Value("${app.mensageria.retry.max-attempts:3}") int maxAttempts,
                                         @Value("${app.mensageria.retry.initial-interval-ms:1000}") long initialIntervalMs,
                                         @Value("${app.mensageria.retry.multiplier:2.0}") double multiplier,
                                         @Value("${app.mensageria.retry.max-interval-ms:10000}") long maxIntervalMs) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        template.setRetryTemplate(retryTemplate(maxAttempts, initialIntervalMs, multiplier, maxIntervalMs));
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                String correlationId = correlationData == null ? "sem-correlation-id" : correlationData.getId();
                LOGGER.error("Publisher confirm negativo do RabbitMQ para correlationId={}: {}",
                        correlationId,
                        cause == null ? "sem detalhe informado" : cause);
            }
        });
        template.setReturnsCallback(returned -> LOGGER.error(
                "Mensagem retornada pelo RabbitMQ: exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText()));
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }

    private RetryTemplate retryTemplate(int maxAttempts,
                                        long initialIntervalMs,
                                        double multiplier,
                                        long maxIntervalMs) {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(maxAttempts));

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialIntervalMs);
        backOffPolicy.setMultiplier(multiplier);
        backOffPolicy.setMaxInterval(maxIntervalMs);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
}
