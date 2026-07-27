package org.example.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public class DeliverOrderRequest {

    @Size(max = 500, message = "Delivery note must be at most 500 characters")
    private String deliveryNote;

    @Valid
    private List<DeliveryProofImageDTO> proofImages;

    public String getDeliveryNote() { return deliveryNote; }
    public void setDeliveryNote(String deliveryNote) { this.deliveryNote = deliveryNote; }

    public List<DeliveryProofImageDTO> getProofImages() { return proofImages; }
    public void setProofImages(List<DeliveryProofImageDTO> proofImages) { this.proofImages = proofImages; }
}