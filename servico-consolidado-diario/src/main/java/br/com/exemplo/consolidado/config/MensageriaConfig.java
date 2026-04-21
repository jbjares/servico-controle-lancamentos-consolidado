package br.com.exemplo.consolidado.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MensageriaConfig {

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
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
