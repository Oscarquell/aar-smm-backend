package kg.azem.portfolio.admin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MediaStorageService {
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "video/mp4", ".mp4",
            "video/webm", ".webm"
    );

    private final Path storagePath;

    public MediaStorageService(@Value("${app.media.storage-path:storage/uploads}") String storagePath) {
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
    }

    public StoredMedia store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Файл пустой");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Файл пустой или превышает 50 МБ");
        }

        String contentType = file.getContentType();
        String extension = ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Поддерживаются JPG, PNG, WebP, MP4 и WebM");
        }

        if (!hasExpectedSignature(file, contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Содержимое файла не соответствует его формату");
        }

        String filename = UUID.randomUUID() + extension;
        Path destination = storagePath.resolve(filename).normalize();
        if (!destination.startsWith(storagePath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректное имя файла");
        }

        try {
            Files.createDirectories(storagePath);
            file.transferTo(destination);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сохранить медиафайл");
        }

        String mediaType = contentType.startsWith("video/") ? "VIDEO" : "IMAGE";
        return new StoredMedia("/uploads/" + filename, mediaType, destination);
    }

    private boolean hasExpectedSignature(MultipartFile file, String contentType) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            return switch (contentType) {
                case "image/jpeg" -> header.length >= 3
                        && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
                case "image/png" -> header.length >= 8
                        && (header[0] & 0xff) == 0x89 && header[1] == 0x50 && header[2] == 0x4e && header[3] == 0x47
                        && header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a;
                case "image/webp" -> header.length >= 12
                        && ascii(header, 0, 4).equals("RIFF") && ascii(header, 8, 4).equals("WEBP");
                case "video/mp4" -> header.length >= 8 && ascii(header, 4, 4).equals("ftyp");
                case "video/webm" -> header.length >= 4
                        && (header[0] & 0xff) == 0x1a && (header[1] & 0xff) == 0x45
                        && (header[2] & 0xff) == 0xdf && (header[3] & 0xff) == 0xa3;
                default -> false;
            };
        } catch (IOException exception) {
            return false;
        }
    }

    private String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, java.nio.charset.StandardCharsets.US_ASCII);
    }

    public void deleteIfManaged(String url) {
        if (url == null || !url.startsWith("/uploads/")) return;
        Path candidate = storagePath.resolve(url.substring("/uploads/".length())).normalize();
        if (!candidate.startsWith(storagePath)) return;
        try {
            Files.deleteIfExists(candidate);
        } catch (IOException ignored) {
            // A later cleanup can remove an orphaned upload; never expose storage details to the client.
        }
    }

    public record StoredMedia(String url, String type, Path path) {
    }
}
