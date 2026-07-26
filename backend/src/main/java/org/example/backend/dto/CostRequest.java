package org.example.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CostRequest {
    @NotBlank(message = "Cost name is required")
    @Size(max = 200)
    private String name;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Pattern(regexp = "VND|USD|EUR",
            message = "Currency must be VND, USD, or EUR")
    private String currency;

    @NotNull(message = "Date is required")
    private LocalDateTime date;

    @Size(max = 100)
    private String payer;

    @Size(max = 1000)
    private String description;

    @Size(max = 500)
    @Pattern(regexp = "^(https?://.*)?$",
            message = "Invoice URL must be a valid http(s) URL")
    private String invoiceUrl;

    @Pattern(regexp = "PENDING|APPROVED|REJECTED|PAID",
            message = "Status must be PENDING, APPROVED, REJECTED, or PAID")
    private String status;

    public CostRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getPayer() { return payer; }
    public void setPayer(String payer) { this.payer = payer; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInvoiceUrl() { return invoiceUrl; }
    public void setInvoiceUrl(String invoiceUrl) { this.invoiceUrl = invoiceUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
