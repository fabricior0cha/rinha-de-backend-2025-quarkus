package br.com.fabricio.model;

import java.time.Instant;

public class Payment {
    private String correlationId;
    private Float amount;
    private Instant requestedAt;
    private Integer processor;

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Float getAmount() {
        return amount;
    }

    public void setAmount(Float amount) {
        this.amount = amount;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Integer getProcessor() {
        return processor;
    }
    public void setProcessor(Integer processor) {
        this.processor = processor;
    }
}
