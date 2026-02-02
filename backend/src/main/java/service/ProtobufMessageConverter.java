package service;

import model.UploadEvent;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ProtobufMessageConverter - Konverter za Protobuf serijalizaciju (3.14 zahtev)
 * 
 * UMESTO kompajliranja .proto fajla sa protoc kompajlerom,
 * ovde ručno serijalizujemo podatke u Protobuf binary format.
 * 
 * OVO JE JEDNOSTAVNIJA IMPLEMENTACIJA jer ne zahteva:
 * - Instalaciju protoc kompajlera
 * - Generisanje Java klasa iz .proto fajla
 * - Build proces kompajliranja
 * 
 * KORISTI:
 * - DataOutputStream za pisanje binary podataka
 * - Protobuf wire format (tag + value)
 */
@Component
public class ProtobufMessageConverter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ============================================
    // SERIJALIZACIJA: Java → Protobuf Binary
    // ============================================

    /**
     * Serijalizuje UploadEvent u Protobuf binary format.
     * 
     * PROTOBUF WIRE FORMAT:
     * Svako polje: [tag][value]
     * - tag = (field_number << 3) | wire_type
     * - wire_type: 0=varint, 2=length-delimited
     * 
     * @param event - UploadEvent objekat
     * @return byte[] - Binary reprezentacija
     * @throws IOException - Ako serijalizacija ne uspe
     */
    public byte[] toProtobuf(UploadEvent event) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        // Field 1: post_id (int64)
        if (event.getPostId() != null) {
            writeTag(out, 1, 0); // wire_type=0 (varint)
            writeVarInt(out, event.getPostId());
        }

        // Field 2: title (string)
        if (event.getTitle() != null) {
            writeTag(out, 2, 2); // wire_type=2 (length-delimited)
            writeString(out, event.getTitle());
        }

        // Field 3: description (string)
        if (event.getDescription() != null) {
            writeTag(out, 3, 2);
            writeString(out, event.getDescription());
        }

        // Field 4: author (string)
        if (event.getAuthor() != null) {
            writeTag(out, 4, 2);
            writeString(out, event.getAuthor());
        }

        // Field 5: author_email (string)
        if (event.getAuthorEmail() != null) {
            writeTag(out, 5, 2);
            writeString(out, event.getAuthorEmail());
        }

        // Field 6: video_url (string)
        if (event.getVideoUrl() != null) {
            writeTag(out, 6, 2);
            writeString(out, event.getVideoUrl());
        }

        // Field 7: thumbnail_url (string)
        if (event.getThumbnailUrl() != null) {
            writeTag(out, 7, 2);
            writeString(out, event.getThumbnailUrl());
        }

        // Field 8: file_size (int64)
        if (event.getFileSize() != null) {
            writeTag(out, 8, 0);
            writeVarInt(out, event.getFileSize());
        }

        // Field 9: duration (int32)
        if (event.getDuration() != null) {
            writeTag(out, 9, 0);
            writeVarInt(out, event.getDuration());
        }

        // Field 10: timestamp (string)
        if (event.getTimestamp() != null) {
            writeTag(out, 10, 2);
            writeString(out, event.getTimestamp().format(FORMATTER));
        }

        // Field 11: event_type (string)
        if (event.getEventType() != null) {
            writeTag(out, 11, 2);
            writeString(out, event.getEventType());
        }

        return baos.toByteArray();
    }

    // ============================================
    // DESERIJALIZACIJA: Protobuf Binary → Java
    // ============================================

    /**
     * Deserijalizuje Protobuf binary u UploadEvent objekat.
     * 
     * @param data - Binary podaci
     * @return UploadEvent - Rekonstruisan objekat
     * @throws IOException - Ako deserijalizacija ne uspe
     */
    public UploadEvent fromProtobuf(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);

        UploadEvent event = new UploadEvent();

        while (bais.available() > 0) {
            int tag = (int) readVarInt(in);
            int fieldNumber = tag >> 3;
            int wireType = tag & 0x07;

            switch (fieldNumber) {
                case 1: // post_id
                    event.setPostId(readVarInt(in));
                    break;
                case 2: // title
                    event.setTitle(readString(in));
                    break;
                case 3: // description
                    event.setDescription(readString(in));
                    break;
                case 4: // author
                    event.setAuthor(readString(in));
                    break;
                case 5: // author_email
                    event.setAuthorEmail(readString(in));
                    break;
                case 6: // video_url
                    event.setVideoUrl(readString(in));
                    break;
                case 7: // thumbnail_url
                    event.setThumbnailUrl(readString(in));
                    break;
                case 8: // file_size
                    event.setFileSize(readVarInt(in));
                    break;
                case 9: // duration
                    event.setDuration((int) readVarInt(in));
                    break;
                case 10: // timestamp
                    String timestampStr = readString(in);
                    event.setTimestamp(LocalDateTime.parse(timestampStr, FORMATTER));
                    break;
                case 11: // event_type
                    event.setEventType(readString(in));
                    break;
                default:
                    // Skip unknown field
                    skipField(in, wireType);
                    break;
            }
        }

        return event;
    }

    // ============================================
    // POMOĆNE METODE - Protobuf Wire Format
    // ============================================

    /**
     * Piše Protobuf tag (field_number + wire_type)
     */
    private void writeTag(DataOutputStream out, int fieldNumber, int wireType) throws IOException {
        int tag = (fieldNumber << 3) | wireType;
        writeVarInt(out, tag);
    }

    /**
     * Piše varint (variable-length integer)
     * Protobuf koristi varint za efikasnije čuvanje brojeva
     */
    private void writeVarInt(DataOutputStream out, long value) throws IOException {
        while (true) {
            if ((value & ~0x7FL) == 0) {
                out.writeByte((int) value);
                return;
            } else {
                out.writeByte(((int) value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    /**
     * Piše string (length + bytes)
     */
    private void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes("UTF-8");
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    /**
     * Čita varint
     */
    private long readVarInt(DataInputStream in) throws IOException {
        long result = 0;
        int shift = 0;
        while (true) {
            byte b = in.readByte();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
    }

    /**
     * Čita string
     */
    private String readString(DataInputStream in) throws IOException {
        int length = (int) readVarInt(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    /**
     * Preskače nepoznato polje
     */
    private void skipField(DataInputStream in, int wireType) throws IOException {
        switch (wireType) {
            case 0: // varint
                readVarInt(in);
                break;
            case 2: // length-delimited
                int length = (int) readVarInt(in);
                in.skipBytes(length);
                break;
            default:
                throw new IOException("Unknown wire type: " + wireType);
        }
    }
}