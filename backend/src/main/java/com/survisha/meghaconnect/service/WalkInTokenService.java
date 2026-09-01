package com.survisha.meghaconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalkInTokenService {

    private static final DateTimeFormatter TOKEN_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public String nextToken(LocalDate tokenDate) {
        LocalDate safeDate = tokenDate != null ? tokenDate : LocalDate.now();
        String tokenPrefix = "WALKIN-" + safeDate.format(TOKEN_DATE);

        jdbcTemplate.update("""
                INSERT IGNORE INTO walkin_token_sequence (token_date, last_token_value)
                VALUES (?, 0)
                """, safeDate);

        Integer currentSequence = jdbcTemplate.queryForObject("""
                SELECT last_token_value
                FROM walkin_token_sequence
                WHERE token_date = ?
                FOR UPDATE
                """, Integer.class, safeDate);

        Integer maxIssuedToken = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(CAST(SUBSTRING(token_number, ?) AS UNSIGNED)), 0)
                FROM walkins
                WHERE token_date = ?
                  AND token_number LIKE ?
                """, Integer.class, tokenPrefix.length() + 2, safeDate, tokenPrefix + "-%");

        int sequenceValue = currentSequence != null ? currentSequence : 0;
        int issuedValue = maxIssuedToken != null ? maxIssuedToken : 0;
        int next = Math.max(sequenceValue, issuedValue) + 1;

        jdbcTemplate.update("""
                UPDATE walkin_token_sequence
                SET last_token_value = ?
                WHERE token_date = ?
                """, next, safeDate);

        if (issuedValue > sequenceValue) {
            log.warn("Walk-in token sequence healed from existing walkins tokenDate={} sequenceValue={} maxIssuedToken={}",
                    safeDate, sequenceValue, issuedValue);
        }

        return tokenPrefix + "-" + String.format("%04d", next);
    }

    public List<LocalDate> availableTokenDates() {
        return jdbcTemplate.query("""
                SELECT token_date
                FROM walkin_token_sequence
                ORDER BY token_date DESC
                """, (rs, rowNum) -> rs.getDate("token_date").toLocalDate());
    }
}
