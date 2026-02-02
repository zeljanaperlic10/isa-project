package com.example.consumer_app.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQConfig - Consumer konfiguracija (3.14 zahtev)
 * 
 * NAPOMENA:
 * Consumer NE kreira queue/exchange - samo se konektuje!
 * Producer (backend app) kreira queue/exchange.
 * 
 * Consumer MORA znati:
 * - Queue name (mora biti ISTI kao u producer app-u!)
 */
@Configuration
public class RabbitMQConfig {

    // ============================================
    // KONSTANTE - MORAJU SE POKLAPATI SA PRODUCER APP!
    // ============================================
    
    /**
     * Ime queue-a (MORA biti isto kao u backend app-u!)
     */
    public static final String UPLOAD_QUEUE = "video.upload.queue";

    // ============================================
    // QUEUE - Samo referenca (ne kreira!)
    // ============================================
    
    /**
     * Referenca na postojeći queue.
     * 
     * VAŽNO:
     * - durable = true: Queue ostaje nakon restarta
     * - Queue već postoji (kreirao ga producer)
     * - Ovo je samo deklaracija za consumer
     * 
     * @return Queue instanca
     */
    @Bean
    public Queue uploadQueue() {
        System.out.println("🔧 [CONSUMER] Referenca na RabbitMQ Queue: " + UPLOAD_QUEUE);
        return new Queue(UPLOAD_QUEUE, true); // durable = true
    }

    // ============================================
    // RABBIT TEMPLATE - Za JSON deserijalizaciju
    // ============================================
    
    /**
     * RabbitTemplate sa JSON message converter-om.
     * 
     * Automatski konvertuje JSON → Java objekte.
     * 
     * @param connectionFactory - Auto-injected
     * @return RabbitTemplate sa JSON converter-om
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter());
        
        System.out.println("🔧 [CONSUMER] RabbitTemplate konfigurisan sa JSON converter-om");
        
        return template;
    }

    /**
     * JSON message converter (koristi Jackson library).
     * 
     * @return Jackson2JsonMessageConverter
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ============================================
    // LIFECYCLE
    // ============================================
    
    public RabbitMQConfig() {
        System.out.println("=".repeat(80));
        System.out.println("🐰 [CONSUMER] RabbitMQ Configuration - Inicijalizacija");
        System.out.println("   Queue: " + UPLOAD_QUEUE);
        System.out.println("=".repeat(80));
    }
}
