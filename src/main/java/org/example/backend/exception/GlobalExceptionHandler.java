package org.example.backend.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // === Lỗi logic nghiệp vụ (RuntimeException) ===
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Lỗi nghiệp vụ", ex.getMessage());
    }

    // === Lỗi trùng dữ liệu (UNIQUE constraint) ===
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause().getMessage();
        String friendlyMsg;

        if (detail.contains("Duplicate") || detail.contains("UNIQUE") || detail.contains("unique")) {
            friendlyMsg = "Dữ liệu bị trùng! Kiểm tra lại các trường có giá trị duy nhất (SKU, username, mã...)";
        } else if (detail.contains("FOREIGN KEY") || detail.contains("foreign key")) {
            friendlyMsg = "Không thể thực hiện vì có dữ liệu liên kết. Xóa dữ liệu liên quan trước.";
        } else if (detail.contains("NULL") || detail.contains("cannot be null")) {
            friendlyMsg = "Thiếu trường bắt buộc! Vui lòng điền đầy đủ thông tin.";
        } else {
            friendlyMsg = "Lỗi dữ liệu: " + detail;
        }

        return buildResponse(HttpStatus.CONFLICT, "Lỗi dữ liệu", friendlyMsg);
    }

    // === Lỗi thiếu/sai tham số ===
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        String msg = "Thiếu tham số bắt buộc: " + ex.getParameterName() + " (kiểu: " + ex.getParameterType() + ")";
        return buildResponse(HttpStatus.BAD_REQUEST, "Thiếu tham số", msg);
    }

    // === Lỗi kiểu dữ liệu sai (VD: truyền string cho UUID) ===
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "Tham số '" + ex.getName() + "' có giá trị không hợp lệ: '" + ex.getValue()
                + "'. Kiểu yêu cầu: "
                + (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "không xác định");
        return buildResponse(HttpStatus.BAD_REQUEST, "Sai kiểu dữ liệu", msg);
    }

    // === Lỗi body JSON không đọc được ===
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBadJson(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Lỗi định dạng",
                "Dữ liệu gửi lên không đúng định dạng JSON. Kiểm tra lại body request.");
    }

    // === Lỗi validation (@Valid) ===
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 400);
        body.put("error", "Lỗi validation");

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        body.put("fields", fieldErrors);
        body.put("message", "Có " + fieldErrors.size() + " trường không hợp lệ");

        return ResponseEntity.badRequest().body(body);
    }

    // === Lỗi chung ===
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống",
                ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
