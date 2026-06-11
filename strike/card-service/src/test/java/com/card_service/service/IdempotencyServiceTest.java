package com.card_service.service;

import com.card_service.common.entity.IdempotencyRecord;
import com.card_service.common.repository.IdempotencyRepository;
import com.card_service.common.service.IdempotencyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock IdempotencyRepository idempotencyRepository;
    @Mock ObjectMapper objectMapper;

    @InjectMocks IdempotencyService idempotencyService;

    // ── check ────────────────────────────────────────────────────────────────

    @Test
    void check_nullKey_returnsEmpty() {
        assertThat(idempotencyService.check(null)).isEmpty();
        verifyNoInteractions(idempotencyRepository);
    }

    @Test
    void check_blankKey_returnsEmpty() {
        assertThat(idempotencyService.check("  ")).isEmpty();
        verifyNoInteractions(idempotencyRepository);
    }

    @Test
    void check_newKey_returnsEmpty() {
        when(idempotencyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());

        assertThat(idempotencyService.check("key-1")).isEmpty();
    }

    @Test
    void check_completedRecord_returnsCachedResponse() throws JsonProcessingException {
        IdempotencyRecord completed = IdempotencyRecord.builder()
                .idempotencyKey("key-1")
                .responseBody("{\"message\":\"ok\"}")
                .httpStatus(201)
                .createdAt(LocalDateTime.now())
                .build();
        when(idempotencyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(completed));
        when(objectMapper.readValue("{\"message\":\"ok\"}", Object.class))
                .thenReturn(Map.of("message", "ok"));

        Optional<ResponseEntity<?>> result = idempotencyService.check("key-1");

        assertThat(result).isPresent();
        assertThat(result.get().getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void check_inFlightRecord_returns409() {
        IdempotencyRecord inFlight = IdempotencyRecord.builder()
                .idempotencyKey("key-1")
                .responseBody(null) // null = reserved, not complete
                .createdAt(LocalDateTime.now())
                .build();
        when(idempotencyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(inFlight));

        Optional<ResponseEntity<?>> result = idempotencyService.check("key-1");

        assertThat(result).isPresent();
        assertThat(result.get().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ── reserve ──────────────────────────────────────────────────────────────

    @Test
    void reserve_nullKey_returnsTrue() {
        assertThat(idempotencyService.reserve(null)).isTrue();
        verifyNoInteractions(idempotencyRepository);
    }

    @Test
    void reserve_newKey_savesAndReturnsTrue() {
        when(idempotencyRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(idempotencyService.reserve("key-1")).isTrue();
        verify(idempotencyRepository).saveAndFlush(any());
    }

    @Test
    void reserve_duplicateKey_returnsFalse() {
        when(idempotencyRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThat(idempotencyService.reserve("key-1")).isFalse();
    }

    // ── complete ─────────────────────────────────────────────────────────────

    @Test
    void complete_nullKey_doesNothing() throws JsonProcessingException {
        idempotencyService.complete(null, Map.of("message", "ok"), 201);
        verifyNoInteractions(idempotencyRepository);
    }

    @Test
    void complete_validKey_updatesRecord() throws JsonProcessingException {
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey("key-1")
                .createdAt(LocalDateTime.now())
                .build();
        when(idempotencyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(record));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"message\":\"ok\"}");

        idempotencyService.complete("key-1", Map.of("message", "ok"), 201);

        assertThat(record.getResponseBody()).isEqualTo("{\"message\":\"ok\"}");
        assertThat(record.getHttpStatus()).isEqualTo(201);
        verify(idempotencyRepository).save(record);
    }

    @Test
    void complete_keyNotFound_doesNothing() {
        when(idempotencyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());

        idempotencyService.complete("key-1", Map.of(), 200);
        verify(idempotencyRepository, never()).save(any());
    }

    // ── cancel ───────────────────────────────────────────────────────────────

    @Test
    void cancel_nullKey_doesNothing() {
        idempotencyService.cancel(null);
        verifyNoInteractions(idempotencyRepository);
    }

    @Test
    void cancel_validKey_deletesRecord() {
        idempotencyService.cancel("key-1");
        verify(idempotencyRepository).deleteByIdempotencyKey("key-1");
    }
}