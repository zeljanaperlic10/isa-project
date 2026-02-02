package com.example.project_backend;

import model.UploadEvent;
import service.ProtobufMessageConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MessageQueueComparisonTest - Poređenje JSON vs Protobuf (3.14 zahtev)
 */
@SpringBootTest
public class MessageQueueComparisonTest {

    @Autowired
    private ProtobufMessageConverter protobufConverter;

    private ObjectMapper jsonMapper;
    private static final int TEST_MESSAGE_COUNT = 50;

    @BeforeEach
    void setUp() {
        jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🧪 MESSAGE QUEUE COMPARISON TEST");
        System.out.println("   Broj poruka: " + TEST_MESSAGE_COUNT);
        System.out.println("=".repeat(80));
    }

    @Test
    void testComparisonJsonVsProtobuf() throws Exception {
        System.out.println("\n📊 POREĐENJE - JSON vs Protobuf");
        System.out.println("=".repeat(80));

        List<Long> jsonSerTimes = new ArrayList<>();
        List<Long> jsonDeserTimes = new ArrayList<>();
        List<Integer> jsonSizes = new ArrayList<>();
        List<Long> protobufSerTimes = new ArrayList<>();
        List<Long> protobufDeserTimes = new ArrayList<>();
        List<Integer> protobufSizes = new ArrayList<>();

        System.out.println("⚙️  Procesiranje " + TEST_MESSAGE_COUNT + " poruka...\n");

        for (int i = 1; i <= TEST_MESSAGE_COUNT; i++) {
            UploadEvent event = createTestEvent(i);

            // JSON
            long jsonSerStart = System.nanoTime();
            String json = jsonMapper.writeValueAsString(event);
            long jsonSerEnd = System.nanoTime();
            jsonSerTimes.add(jsonSerEnd - jsonSerStart);
            jsonSizes.add(json.getBytes("UTF-8").length);

            long jsonDeserStart = System.nanoTime();
            jsonMapper.readValue(json, UploadEvent.class);
            long jsonDeserEnd = System.nanoTime();
            jsonDeserTimes.add(jsonDeserEnd - jsonDeserStart);

            // Protobuf
            long protobufSerStart = System.nanoTime();
            byte[] protobufData = protobufConverter.toProtobuf(event);
            long protobufSerEnd = System.nanoTime();
            protobufSerTimes.add(protobufSerEnd - protobufSerStart);
            protobufSizes.add(protobufData.length);

            long protobufDeserStart = System.nanoTime();
            protobufConverter.fromProtobuf(protobufData);
            long protobufDeserEnd = System.nanoTime();
            protobufDeserTimes.add(protobufDeserEnd - protobufDeserStart);

            if (i % 10 == 0) {
                System.out.println("   ✅ Procesuirano " + i + " / " + TEST_MESSAGE_COUNT);
            }
        }

        // PROSECI
        double avgJsonSer = jsonSerTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgJsonDeser = jsonDeserTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgJsonSize = jsonSizes.stream().mapToInt(Integer::intValue).average().orElse(0);
        double avgProtobufSer = protobufSerTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgProtobufDeser = protobufDeserTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgProtobufSize = protobufSizes.stream().mapToInt(Integer::intValue).average().orElse(0);

        // PRIKAZ
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 FINALNI REZULTATI - JSON vs PROTOBUF");
        System.out.println("   (preko " + TEST_MESSAGE_COUNT + " poruka)");
        System.out.println("=".repeat(80));
        System.out.println();

        System.out.println("⏱️  VREME SERIJALIZACIJE:");
        System.out.println("   JSON:     " + String.format("%.3f ms", avgJsonSer / 1_000_000.0));
        System.out.println("   Protobuf: " + String.format("%.3f ms", avgProtobufSer / 1_000_000.0));
        if (avgProtobufSer > 0) {
            System.out.println("   Razlika:  " + String.format("%.1fx brže", avgJsonSer / avgProtobufSer) + " (Protobuf)");
        }
        System.out.println();

        System.out.println("⏱️  VREME DESERIJALIZACIJE:");
        System.out.println("   JSON:     " + String.format("%.3f ms", avgJsonDeser / 1_000_000.0));
        System.out.println("   Protobuf: " + String.format("%.3f ms", avgProtobufDeser / 1_000_000.0));
        if (avgProtobufDeser > 0) {
            System.out.println("   Razlika:  " + String.format("%.1fx brže", avgJsonDeser / avgProtobufDeser) + " (Protobuf)");
        }
        System.out.println();

        System.out.println("📦 VELIČINA PORUKE:");
        System.out.println("   JSON:     " + (int) avgJsonSize + " bytes");
        System.out.println("   Protobuf: " + (int) avgProtobufSize + " bytes");
        if (avgJsonSize > 0) {
            System.out.println("   Razlika:  " + String.format("%.1f%% manji", (1 - avgProtobufSize / avgJsonSize) * 100) + " (Protobuf)");
        }
        System.out.println();

        System.out.println("✅ ZAKLJUČAK:");
        if (avgProtobufSer < avgJsonSer && avgProtobufDeser < avgJsonDeser) {
            System.out.println("   ⚡ Protobuf je BRŽI za serijalizaciju i deserijalizaciju!");
        }
        if (avgProtobufSize < avgJsonSize) {
            System.out.println("   📦 Protobuf ima MANJU veličinu poruke!");
        }
        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("🎉 Test završen! Zahtev 3.14 ispunjen!");
        System.out.println("=".repeat(80));
    }

    private UploadEvent createTestEvent(int index) {
        UploadEvent event = new UploadEvent();
        event.setPostId((long) index);
        event.setTitle("Test Video #" + index);
        event.setDescription("Test opis za video broj " + index + ". Lorem ipsum dolor sit amet.");
        event.setAuthor("testuser" + (index % 10));
        event.setAuthorEmail("testuser" + (index % 10) + "@example.com");
        event.setVideoUrl("http://localhost:9090/api/videos/video_" + index + ".mp4");
        event.setThumbnailUrl("http://localhost:9090/api/thumbnails/thumb_" + index + ".jpg");
        event.setFileSize(10_000_000L + (index * 100_000L));
        event.setDuration(180 + (index * 10));
        event.setTimestamp(LocalDateTime.now());
        event.setEventType("VIDEO_UPLOADED");
        return event;
    }
}