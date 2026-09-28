package com.raya.payment_service.controller;

import com.raya.payment_service.dto.PaymentRequest;
import com.raya.payment_service.dto.PaymentResponse;
import com.raya.payment_service.model.IdempotencyRecord;
import com.raya.payment_service.repository.IdempotencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * PaymentController — Session 4 (original) + Session 22 (idempotency).
 *
 * Session 22 addition: checks the Idempotency-Key header:
 *   - Key already present with COMPLETED: return cached APPROVED response (200 OK)
 *   - Key already present with PROCESSING: return 202 Accepted
 *   - Key not present: register as PROCESSING, process, update to COMPLETED/FAILED
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @Value("${payment.failure-rate:0.5}")
    private double failureRate;

    @Value("${payment.delay-ms:0}")
    private long delayMs;

    private final Random random = new Random();
    private final IdempotencyRepository idempotencyRepository;

    public PaymentController(IdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestBody PaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) throws InterruptedException {

        // Idempotency check (Session 22 Lab 18 Task 2)
        if (idempotencyKey != null) {
            Optional<IdempotencyRecord> existing = idempotencyRepository.findById(idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();
                log.info("[IDEMPOTENCY] Duplicate request detected for key={} status={}",
                        idempotencyKey, record.getStatus());
                if ("COMPLETED".equals(record.getStatus())) {
                    return ResponseEntity.ok(new PaymentResponse(
                            record.getResponsePayload(),
                            "APPROVED",
                            request.amount()
                    ));
                }
                return ResponseEntity.accepted().body(new PaymentResponse(
                        null,
                        "PROCESSING",
                        request.amount()
                ));
            }
            idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, idempotencyKey));
        }

        if (delayMs > 0) {
            Thread.sleep(delayMs);
        }

        if (random.nextDouble() < failureRate) {
            if (idempotencyKey != null) {
                idempotencyRepository.findById(idempotencyKey)
                        .ifPresent(r -> {
                            r.fail("Payment gateway timeout");
                            idempotencyRepository.save(r);
                        });
            }
            throw new RuntimeException("Payment gateway timeout");
        }

        String transactionId = UUID.randomUUID().toString();
        if (idempotencyKey != null) {
            idempotencyRepository.findById(idempotencyKey)
                    .ifPresent(r -> {
                        r.complete(transactionId);
                        idempotencyRepository.save(r);
                    });
        }

        return ResponseEntity.ok(new PaymentResponse(
                transactionId,
                "APPROVED",
                request.amount()
        ));
    }
}