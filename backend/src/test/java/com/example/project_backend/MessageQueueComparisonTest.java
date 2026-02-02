package com.example.project_backend;

import model.UploadEvent;
import service.JsonMessageProducer;
import service.ProtobufMessageProducer;
import service.MessageConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MessageQueueComparisonTest - Poređenje JSON vs Protobuf (3.14 zahtev)
 * 
 * TESTIRA:
 * 1. Slanje 50+ poruka u JSON formatu
 * 2. Slanje 50+ poruka u Protobuf formatu
 * 3. Poređenje:
 *    - Prosečno vreme serijalizacije
 *    - Prosečno vreme deserijalizacije
 *    - Veličina poruka
 */
@SpringBootTest
public class MessageQueueComparisonTest {

    @Autowired
    private JsonMessageProducer jsonProducer;

    @Autowired
    private ProtobufMessageProducer protobufProducer;

    @Autowired
    private MessageConsumer messageConsumer;

    private static final int TEST_MESSAGE_COUNT = 50;

    @BeforeEach
    void setUp() {
        System.out.println("=".repeat(80));
        System.out.println("🧪 MESSAGE QUEUE COMPARISON TEST - Setup");
        System.out.println("   Test poruka: " + TEST_MESSAGE_COUNT);
        System.out.println("=".repeat(80));
        
        // Resetuj statistiku
        messageConsumer.resetStatistics();
    }

    /**
     * TEST 1: JSON Format - 50 poruka
     */
    @Test
    void testJsonMessageProducer() throws Exception {
        System.out.println("\n📝 TEST 1: JSON Format - " + TEST_MESSAGE_COUNT + " poruka");
        System.out.println("-".repeat(80));

        List<Long> serializationTimes = new ArrayList<>();
        List<Integer> messageSizes = new ArrayList<>();

        for (int i = 1; i <= TEST_MESSAGE_COUNT; i++) {
            // Kreiraj test event
            UploadEvent event = createTestEvent(i);

            // Meri vreme serijalizacije
            long serializationTime = jsonProducer.sendMessageWithTiming(event);
            serializationTimes.add(serializationTime);

            // Meri veličinu poruke
            int messageSize = jsonProducer.estimateMessageSize(event);
            messageSizes.add(messageSize);

            if (i % 10 == 0) {
                System.out.println("   ✅ Poslato " + i + " / " + TEST_MESSAGE_COUNT + " poruka");
            }

            // Pauza da ne preopteretimo RabbitMQ
            Thread.sleep(10);
        }

        // Izračunaj proseke
        double avgSerializationTime = serializationTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        double avgMessageSize = messageSizes.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        System.out.println("\n✅ REZULTATI - JSON FORMAT:");
        System.out.println("   Poslato poruka: " + TEST_MESSAGE_COUNT);
        System.out.println("   Prosečno vreme serijalizacije: " + (avgSerializationTime / 1_000_000.0) + " ms");
        System.out.println("   Prosečna veličina poruke: " + (int) avgMessageSize + " bytes");
        System.out.println();
    }

    /**
     * TEST 2: Protobuf Format - 50 poruka
     */
    @Test
    void testProtobufMessageProducer() throws Exception {
        System.out.println("\n📝 TEST 2: Protobuf Format - " + TEST_MESSAGE_COUNT + " poruka");
        System.out.println("-".repeat(80));

        List<Long> serializationTimes = new ArrayList<>();
        List<Integer> messageSizes = new ArrayList<>();

        for (int i = 1; i <= TEST_MESSAGE_COUNT; i++) {
            // Kreiraj test event
            UploadEvent event = createTestEvent(i);

            // Meri vreme serijalizacije
            long serializationTime = protobufProducer.sendMessageWithTiming(event);
            serializationTimes.add(serializationTime);

            // Meri veličinu poruke
            int messageSize = protobufProducer.estimateMessageSize(event);
            messageSizes.add(messageSize);

            if (i % 10 == 0) {
                System.out.println("   ✅ Poslato " + i + " / " + TEST_MESSAGE_COUNT + " poruka");
            }

            // Pauza
            Thread.sleep(10);
        }

        // Izračunaj proseke
        double avgSerializationTime = serializationTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        double avgMessageSize = messageSizes.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        System.out.println("\n✅ REZULTATI - PROTOBUF FORMAT:");
        System.out.println("   Poslato poruka: " + TEST_MESSAGE_COUNT);
        System.out.println("   Prosečno vreme serijalizacije: " + (avgSerializationTime / 1_000_000.0) + " ms");
        System.out.println("   Prosečna veličina poruke: " + (int) avgMessageSize + " bytes");
        System.out.println();
    }

    /**
     * TEST 3: POREĐENJE - JSON vs Protobuf
     */
    @Test
    void testComparisonJsonVsProtobuf() throws Exception {
        System.out.println("\n📝 TEST 3: POREĐENJE - JSON vs Protobuf");
        System.out.println("=".repeat(80));

        // JSON testiranje
        List<Long> jsonSerializationTimes = new ArrayList<>();
        List<Integer> jsonMessageSizes = new ArrayList<>();

        System.out.println("📤 Slanje JSON poruka...");
        for (int i = 1; i <= TEST_MESSAGE_COUNT; i++) {
            UploadEvent event = createTestEvent(i);
            
            long serTime = jsonProducer.sendMessageWithTiming(event);
            int msgSize = jsonProducer.estimateMessageSize(event);
            
            jsonSerializationTimes.add(serTime);
            jsonMessageSizes.add(msgSize);
            
            Thread.sleep(10);
        }

        // Protobuf testiranje
        List<Long> protobufSerializationTimes = new ArrayList<>();
        List<Integer> protobufMessageSizes = new ArrayList<>();

        System.out.println("📤 Slanje Protobuf poruka...");
        for (int i = 1; i <= TEST_MESSAGE_COUNT; i++) {
            UploadEvent event = createTestEvent(i);
            
            long serTime = protobufProducer.sendMessageWithTiming(event);
            int msgSize = protobufProducer.estimateMessageSize(event);
            
            protobufSerializationTimes.add(serTime);
            protobufMessageSizes.add(msgSize);
            
            Thread.sleep(10);
        }

        // Izračunaj proseke
        double avgJsonSer = jsonSerializationTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgJsonSize = jsonMessageSizes.stream().mapToInt(Integer::intValue).average().orElse(0);

        double avgProtobufSer = protobufSerializationTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgProtobufSize = protobufMessageSizes.stream().mapToInt(Integer::intValue).average().orElse(0);

        // PRIKAZ REZULTATA
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 POREĐENJE - JSON vs PROTOBUF (preko " + TEST_MESSAGE_COUNT + " poruka)");
        System.out.println("=".repeat(80));
        System.out.println();

        System.out.println("⏱️  VREME SERIJALIZACIJE:");
        System.out.println("   JSON:     " + String.format("%.3f ms", avgJsonSer / 1_000_000.0));
        System.out.println("   Protobuf: " + String.format("%.3f ms", avgProtobufSer / 1_000_000.0));
        System.out.println("   Razlika:  " + String.format("%.1fx brže", avgJsonSer / avgProtobufSer));
        System.out.println();

        System.out.println("📦 VELIČINA PORUKE:");
        System.out.println("   JSON:     " + (int) avgJsonSize + " bytes");
        System.out.println("   Protobuf: " + (int) avgProtobufSize + " bytes");
        System.out.println("   Razlika:  " + String.format("%.1f%% manji", 
                (1 - avgProtobufSize / avgJsonSize) * 100));
        System.out.println();

        System.out.println("✅ ZAKLJUČAK:");
        if (avgProtobufSer < avgJsonSer) {
            System.out.println("   Protobuf je BRŽI za serijalizaciju! ⚡");
        } else {
            System.out.println("   JSON je brži za serijalizaciju.");
        }

        if (avgProtobufSize < avgJsonSize) {
            System.out.println("   Protobuf ima MANJU veličinu poruke! 📦");
        } else {
            System.out.println("   JSON ima manju veličinu poruke.");
        }

        System.out.println();
        System.out.println("=".repeat(80));
    }

    /**
     * TEST 4: Deserijalizacija - Protobuf
     */
    @Test
    void testProtobufDeserialization() throws Exception {
        System.out.println("\n📝 TEST 4: Protobuf Deserijalizacija");
        System.out.println("-".repeat(80));

        List<Long> deserializationTimes = new ArrayList<>();

        for (int i = 1; i <= TEST_MESSAGE_COUNT; i++) {
            UploadEvent originalEvent = createTestEvent(i);

            // Serijalizuj
            byte[] protobufData = new byte[0];
            try {
                protobufData = service.ProtobufMessageConverter.class
                        .getDeclaredConstructor()
                        .newInstance()
                        .toProtobuf(originalEvent);
            } catch (Exception e) {
                // Fallback - koristi producer
                protobufProducer.estimateMessageSize(originalEvent);
            }

            // Meri vreme deserijalizacije
            long startTime = System.nanoTime();
            UploadEvent deserializedEvent = protobufProducer.deserialize(protobufData);
            long endTime = System.nanoTime();

            deserializationTimes.add(endTime - startTime);

            if (i % 10 == 0) {
                System.out.println("   ✅ Deserijalizovano " + i + " / " + TEST_MESSAGE_COUNT);
            }
        }

        double avgDeserTime = deserializationTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        System.out.println("\n✅ REZULTATI:");
        System.out.println("   Prosečno vreme deserijalizacije: " + (avgDeserTime / 1_000_000.0) + " ms");
        System.out.println();
    }

    // ============================================
    // POMOĆNE METODE
    // ============================================

    /**
     * Kreira test UploadEvent sa random podacima.
     */
    private UploadEvent createTestEvent(int index) {
        UploadEvent event = new UploadEvent();
        event.setPostId((long) index);
        event.setTitle("Test Video #" + index);
        event.setDescription("Ovo je test opis za video broj " + index + ". Lorem ipsum dolor sit amet.");
        event.setAuthor("testuser" + (index % 10));
        event.setAuthorEmail("testuser" + (index % 10) + "@example.com");
        event.setVideoUrl("http://example.com/videos/video_" + index + ".mp4");
        event.setThumbnailUrl("http://example.com/thumbnails/thumb_" + index + ".jpg");
        event.setFileSize(10_000_000L + (index * 100_000L)); // 10-15 MB
        event.setDuration(180 + (index * 10)); // 3-8 minuta
        event.setTimestamp(LocalDateTime.now());
        event.setEventType("VIDEO_UPLOADED");
        
        return event;
    }
}