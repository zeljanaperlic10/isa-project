package config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQConfig - Konfiguracija za Message Queue (3.14 zahtev)
 * 
 * KOMPONENTE:
 * - Queue: Red gde se čuvaju poruke
 * - Exchange: Rutira poruke ka queue-ovima
 * - Binding: Povezuje exchange i queue
 * - RabbitTemplate: Za slanje poruka
 * 
 * ŠEMA:
 * Producer → Exchange → Queue → Consumer
 */
@Configuration
public class RabbitMQConfig {

    // ============================================
    // KONSTANTE
    // ============================================
    
    /**
     * Ime queue-a za video upload event-e
     */
    public static final String UPLOAD_QUEUE = "video.upload.queue";
    
    /**
     * Ime exchange-a (Topic exchange - podržava wildcard routing)
     */
    public static final String UPLOAD_EXCHANGE = "video.upload.exchange";
    
    /**
     * Routing key - pattern za rutiranje poruka
     */
    public static final String UPLOAD_ROUTING_KEY = "video.upload";

    // ============================================
    // QUEUE - Red za poruke
    // ============================================
    
    /**
     * Kreira queue za video upload event-e.
     * 
     * @param durable - true: Queue ostaje nakon restarta RabbitMQ-a
     * @return Queue instanca
     */
    @Bean
    public Queue uploadQueue() {
        System.out.println("🔧 Kreiranje RabbitMQ Queue: " + UPLOAD_QUEUE);
        return new Queue(UPLOAD_QUEUE, true); // durable = true
    }

    // ============================================
    // EXCHANGE - Rutira poruke
    // ============================================
    
 
    @Bean
    public TopicExchange uploadExchange() {
        System.out.println("🔧 Kreiranje RabbitMQ Exchange: " + UPLOAD_EXCHANGE);
        return new TopicExchange(UPLOAD_EXCHANGE);
    }

    // ============================================
    // BINDING - Povezuje Exchange i Queue
    // ============================================
    
    
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        System.out.println("🔧 Kreiranje Binding: " + UPLOAD_ROUTING_KEY);
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(UPLOAD_ROUTING_KEY);
    }

    // ============================================
    // RABBIT TEMPLATE - Za slanje poruka
    // ============================================
    
   
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter());
        
        System.out.println("🔧 RabbitTemplate konfigurisan sa JSON converter-om");
        
        return template;
    }

   
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ============================================
    // LIFECYCLE
    // ============================================
    
    public RabbitMQConfig() {
        System.out.println("=".repeat(80));
        System.out.println("🐰 RabbitMQ Configuration - Inicijalizacija (3.14 zahtev)");
        System.out.println("=".repeat(80));
    }
}