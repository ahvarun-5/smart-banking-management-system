package com.smartbank.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token blacklist service for immediate server-side revocation on logout.
 * Periodically purges expired tokens to maintain optimal memory consumption.
 */
@Service
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);

    // Map storing blacklisted token hash/signature to its expiry timestamp
    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Instant expiry) {
        if (token != null && !token.isBlank()) {
            blacklistedTokens.put(token, expiry != null ? expiry : Instant.now().plusSeconds(86400));
            logger.info("Token blacklisted successfully until {}", expiry);
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Instant expiry = blacklistedTokens.get(token);
        if (expiry == null) {
            return false;
        }
        if (Instant.now().isAfter(expiry)) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }

    @Scheduled(fixedRate = 3600000) // Clean up every hour
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
        logger.debug("Token blacklist cleanup executed. Active entries: {}", blacklistedTokens.size());
    }
}
