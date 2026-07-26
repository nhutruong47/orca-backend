package org.example.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * File upload với validation nghiêm ngặt.
 *
 * <p><b>Quick Win F1.9:</b> Trước đó chỉ chặn traversal path qua {@code contains("..")},
 * cho phép upload file .exe, .bat, .sh… Bổ sung:
 * <ul>
 *   <li>Whitelist extension (jpg, jpeg, png, gif, webp, pdf, doc, docx, xls, xlsx, csv)</li>
 *   <li>Whitelist MIME content-type</li>
 *   <li>Max 10MB (configurable qua property {@code app.upload.max-size-mb})</li>
 *   <li>Sniff magic bytes cho ảnh (PNG, JPEG, GIF, WEBP) — chống MIME spoofing</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/upload")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB default

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".csv", ".txt"
    );

    private static final Set<String> ALLOWED_MIMES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv", "text/plain"
    );

    private final Path fileStorageLocation;

    public FileController() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            log.error("Cannot create upload directory at {}", this.fileStorageLocation, ex);
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng chọn một file."));
        }

        // 1. Size check
        if (file.getSize() > MAX_SIZE_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("error", "File vượt quá 10MB. Giảm kích thước và thử lại.",
                            "maxSizeMB", 10));
        }

        // 2. Filename sanitization
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "");
        if (originalFileName.isBlank() || originalFileName.contains("..")) {
            log.warn("Upload rejected: bad filename '{}'", originalFileName);
            return ResponseEntity.badRequest().body(Map.of("error", "Tên file không hợp lệ."));
        }

        // 3. Extension whitelist
        String fileExtension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalFileName.length() - 1) {
            fileExtension = originalFileName.substring(dotIndex).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            log.warn("Upload rejected: extension '{}' not allowed (file='{}')", fileExtension, originalFileName);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Định dạng file không được phép. Chỉ chấp nhận: "
                            + String.join(", ", ALLOWED_EXTENSIONS)));
        }

        // 4. Content-type whitelist (chống MIME spoofing cơ bản)
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIMES.contains(contentType.toLowerCase())) {
            log.warn("Upload rejected: content-type '{}' not allowed (file='{}')", contentType, originalFileName);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Content-Type không hợp lệ hoặc không được phép."));
        }

        // 5. Magic-bytes sniff cho ảnh (chống upload .exe đổi extension thành .jpg)
        if (fileExtension.matches("\\.(jpe?g|png|gif|webp)")
                && !hasImageMagicBytes(file.getOriginalFilename(), file)) {
            log.warn("Upload rejected: image magic bytes mismatch (file='{}', type={})",
                    originalFileName, contentType);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Nội dung file không khớp với định dạng ảnh."));
        }

        try {
            String fileName = UUID.randomUUID().toString() + fileExtension;
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            // Phòng traversal phụ: đảm bảo target nằm trong fileStorageLocation
            if (!targetLocation.normalize().startsWith(this.fileStorageLocation)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Đường dẫn file không hợp lệ."));
            }
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("File uploaded: original='{}', stored='{}', size={} bytes, type={}",
                    originalFileName, fileName, file.getSize(), contentType);

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .toUriString();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("fileName", fileName);
            body.put("originalFileName", originalFileName);
            body.put("url", fileDownloadUri);
            body.put("type", contentType);
            body.put("size", file.getSize());
            return ResponseEntity.ok(body);
        } catch (IOException ex) {
            log.error("Failed to store uploaded file '{}'", originalFileName, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Không thể lưu file. Vui lòng thử lại."));
        }
    }

    /**
     * Kiểm tra magic bytes 4 byte đầu của file ảnh.
     * JPEG: FF D8 FF
     * PNG : 89 50 4E 47
     * GIF : 47 49 46 38
     * WEBP: 52 49 46 46 ... 57 45 42 50 (RIFF...WEBP)
     */
    private boolean hasImageMagicBytes(String originalName, MultipartFile file) {
        try (var in = file.getInputStream()) {
            byte[] head = new byte[12];
            int read = in.read(head);
            if (read < 4) return false;

            // JPEG
            if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
                return true;
            }
            // PNG
            if ((head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
                return true;
            }
            // GIF
            if (head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8') {
                return true;
            }
            // WEBP: RIFF????WEBP
            if (read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
                return true;
            }
        } catch (IOException ignored) {
            // Cannot read -> treat as not-an-image
        }
        log.debug("Magic bytes check failed for '{}'", originalName);
        return false;
    }
}
