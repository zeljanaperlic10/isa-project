package service;

import config.RabbitMQConfig;
import model.UploadEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ProtobufMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ProtobufMessageConverter protobufConverter;

    // ============================================
    // SLANJE PORUKA - PROTOBUF FORMAT
    // ============================================

   
    public void sendMessage(UploadEvent event) {
        try {
            System.out.println("📤 Protobuf Producer - Slanje poruke...");
            System.out.println("   Post ID: " + event.getPostId());
            System.out.println("   Title: " + event.getTitle());
            System.out.println("   Author: " + event.getAuthor());
            System.out.println("   File Size: " + event.getReadableFileSize());

            // Konverzija u Protobuf binary
            byte[] protobufData = protobufConverter.toProtobuf(event);

            // Kreiranje RabbitMQ poruke
            MessageProperties props = new MessageProperties();
            props.setContentType("application/x-protobuf");
            Message message = new Message(protobufData, props);

            // Slanje poruke
            rabbitTemplate.send(
                RabbitMQConfig.UPLOAD_EXCHANGE,
                RabbitMQConfig.UPLOAD_ROUTING_KEY,
                message
            );

            System.out.println("✅ Protobuf poruka poslata! (" + protobufData.length + " bytes)");

        } catch (Exception e) {
            System.err.println("❌ Greška pri slanju Protobuf poruke: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send Protobuf message", e);
        }
    }

    
    public long sendMessageWithTiming(UploadEvent event) {
        try {
            // Start timer
            long startTime = System.nanoTime();

            // Konverzija u Protobuf binary
            byte[] protobufData = protobufConverter.toProtobuf(event);

            // End timer (merimo samo serijalizaciju)
            long endTime = System.nanoTime();
            long duration = endTime - startTime;

            // Kreiranje RabbitMQ poruke
            MessageProperties props = new MessageProperties();
            props.setContentType("application/x-protobuf");
            Message message = new Message(protobufData, props);

            // Slanje poruke
            rabbitTemplate.send(
                RabbitMQConfig.UPLOAD_EXCHANGE,
                RabbitMQConfig.UPLOAD_ROUTING_KEY,
                message
            );

            System.out.println("⏱️ Protobuf serijalizacija: " + duration + " ns (" + (duration / 1_000_000.0) + " ms)");

            return duration;

        } catch (Exception e) {
            System.err.println("❌ Greška: " + e.getMessage());
            throw new RuntimeException("Failed to send message", e);
        }
    }

    
    public int estimateMessageSize(UploadEvent event) {
        try {
            // Konvertuj u Protobuf binary
            byte[] protobufData = protobufConverter.toProtobuf(event);
            
            int size = protobufData.length;
            
            System.out.println("📊 Protobuf veličina: " + size + " bytes");
            
            return size;

        } catch (Exception e) {
            System.err.println("❌ Greška pri proceni veličine: " + e.getMessage());
            return -1;
        }
    }

   
    public UploadEvent deserialize(byte[] data) {
        try {
            return protobufConverter.fromProtobuf(data);
        } catch (Exception e) {
            System.err.println("❌ Greška pri deserijalizaciji: " + e.getMessage());
            throw new RuntimeException("Failed to deserialize Protobuf message", e);
        }
    }

    // ============================================
    // STATISTIKA
    // ============================================

    
    public String getInfo() {
        return "ProtobufMessageProducer{" +
                "exchange='" + RabbitMQConfig.UPLOAD_EXCHANGE + '\'' +
                ", routingKey='" + RabbitMQConfig.UPLOAD_ROUTING_KEY + '\'' +
                ", format='Protobuf (Binary)'" +
                '}';
    }
}