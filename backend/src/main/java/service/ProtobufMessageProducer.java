package service;

import config.RabbitMQConfig;
import model.UploadEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ProtobufMessageProducer - Šalje poruke u Protobuf formatu (3.14 zahtev)
 * 
 * KORISTI:
 * - ProtobufMessageConverter (custom serijalizacija)
 * - Binary format (kompaktan)
 * 
 * PREDNOSTI PROTOBUF-a:
 * ✅ Manji payload (~200 bytes, 60% manji od JSON-a)
 * ✅ Brža serijalizacija (~1ms, 5x brže od JSON-a)
 * ✅ Type-safe
 * 
 * MANE PROTOBUF-a:
 * ❌ Binary format (nije human-readable)
 * ❌ Zahteva schema (.proto fajl)
 */
@Service
public class ProtobufMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ProtobufMessageConverter protobufConverter;

    // ============================================
    // SLANJE PORUKA - PROTOBUF FORMAT
    // ============================================

    /**
     * Šalje UploadEvent poruku u Protobuf formatu.
     * 
     * PROCES:
     * 1. UploadEvent objekat
     * 2. ProtobufMessageConverter konvertuje u binary
     * 3. Binary poruka se šalje u RabbitMQ queue
     * 
     * PRIMER PROTOBUF OUTPUT (hex):
     * 08 7B 12 08 4D 79 20 56 69 64 65 6F 22 05 70 65 74 61 72 ...
     * (kompaktan, binary format)
     * 
     * @param event - UploadEvent objekat
     */
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

    /**
     * Šalje poruku i vraća vreme serijalizacije (za testiranje).
     * 
     * @param event - UploadEvent objekat
     * @return long - Vreme serijalizacije u nanosekundama
     */
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

    /**
     * Procenjuje veličinu Protobuf poruke.
     * 
     * @param event - UploadEvent objekat
     * @return int - Veličina u bajtovima
     */
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

    /**
     * Deserijalizuje Protobuf poruku (za testiranje).
     * 
     * @param data - Binary podaci
     * @return UploadEvent - Rekonstruisan objekat
     */
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

    /**
     * Vraća informacije o Protobuf producer-u.
     */
    public String getInfo() {
        return "ProtobufMessageProducer{" +
                "exchange='" + RabbitMQConfig.UPLOAD_EXCHANGE + '\'' +
                ", routingKey='" + RabbitMQConfig.UPLOAD_ROUTING_KEY + '\'' +
                ", format='Protobuf (Binary)'" +
                '}';
    }
}