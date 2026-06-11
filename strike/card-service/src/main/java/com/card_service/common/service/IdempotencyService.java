package com.card_service.common.service;

import com.card_service.common.entity.IdempotencyRecord;
import com.card_service.common.repository.IdempotencyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    /**
     * Checks for an existing idempotency record.
     * Returns a cached ResponseEntity if the request already completed,
     * a 409 if it is still in-flight, or empty if this is a brand-new key.
     */
    public Optional<ResponseEntity<?>> check(String key) {
        if (key == null || key.isBlank()) return Optional.empty();

        return idempotencyRepository.findByIdempotencyKey(key)
                .map(record -> {
                    if (record.getResponseBody() != null) {
                        // Already completed — replay cached response
                        try {
                            Object body = objectMapper.readValue(record.getResponseBody(), Object.class);
                            return ResponseEntity.status(record.getHttpStatus()).body(body);
                        } catch (JsonProcessingException e) {
                            log.warn("Could not deserialize cached idempotency response for key={}", key);
                            return ResponseEntity.status(record.getHttpStatus()).build();
                        }
                    } else {
                        // Reserved but not yet complete — another request is in-flight
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                                (Object) Map.of("error",
                                        "A request with this Idempotency-Key is already being processed. Retry in a moment."));
                    }
                });
    }

    /**
     * Reserves the idempotency key by inserting a placeholder record.
     * Uses REQUIRES_NEW so the INSERT commits immediately before processing begins.
     * Returns false if the key is already taken (race condition — caller should return 409).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean reserve(String key) {
        if (key == null || key.isBlank()) return true;
        try {
            idempotencyRepository.saveAndFlush(IdempotencyRecord.builder()
                    .idempotencyKey(key)
                    .createdAt(LocalDateTime.now())
                    .build());
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    /**
     * Saves the response body for a reserved key, marking it complete.
     * Uses REQUIRES_NEW so the UPDATE commits independently of any outer transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String key, Object responseBody, int httpStatus) {
        if (key == null || key.isBlank()) return;
        idempotencyRepository.findByIdempotencyKey(key).ifPresent(record -> {
            try {
                record.setResponseBody(objectMapper.writeValueAsString(responseBody));
                record.setHttpStatus(httpStatus);
                idempotencyRepository.save(record);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize idempotency response for key={}: {}", key, e.getMessage());
            }
        });
    }

    /**
     * Cancels a reserved key so the client can retry after a processing error.
     * Uses REQUIRES_NEW so the DELETE commits even if the outer transaction is rolling back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancel(String key) {
        if (key != null && !key.isBlank()) {
            idempotencyRepository.deleteByIdempotencyKey(key);
        }
    }
}