package com.survisha.meghaconnect.captcha;

import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.util.DateTimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaptchaServiceTest {
    private CaptchaStoreService store;
    private CaptchaService service;

    @BeforeEach
    void setUp() {
        CaptchaGenerator generator = mock(CaptchaGenerator.class);
        when(generator.generateText()).thenReturn("ABC234");
        when(generator.generateImage("ABC234")).thenReturn("png-base64");
        store = new CaptchaStoreService();
        service = new CaptchaService(generator, store, 5, true);
    }

    @Test
    void generateDoesNotExposeAnswerAndSuccessfulValidationCannotBeReplayed() {
        var captcha = service.generate();
        assertEquals("png-base64", captcha.captchaImage());
        assertEquals(null, captcha.captchaText());
        assertTrue(service.validate(captcha.captchaId(), "abc234").valid());
        assertEquals("INVALID_CAPTCHA", assertThrows(MeghaConnectException.class,
                () -> service.validate(captcha.captchaId(), "ABC234")).getErrorCode());
    }

    @Test
    void invalidAnswerConsumesCaptcha() {
        var captcha = service.generate();
        assertEquals("INVALID_CAPTCHA", assertThrows(MeghaConnectException.class,
                () -> service.validate(captcha.captchaId(), "WRONG1")).getErrorCode());
        assertThrows(MeghaConnectException.class,
                () -> service.validate(captcha.captchaId(), "ABC234"));
    }

    @Test
    void expiredCaptchaIsRejected() {
        store.save("expired", "ABC234", DateTimeUtil.nowISTInstant().minus(1, ChronoUnit.SECONDS));
        // Expired entries may be eagerly purged and are intentionally indistinguishable from unknown IDs.
        assertThrows(MeghaConnectException.class, () -> service.validate("expired", "ABC234"));
    }

    @Test
    void concurrentValidationAllowsOnlyOneSuccess() throws Exception {
        var captcha = service.generate();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        Runnable validate = () -> {
            try {
                start.await();
                service.validate(captcha.captchaId(), "ABC234");
                successes.incrementAndGet();
            } catch (Exception ignored) {
                // One request must lose the atomic remove race.
            }
        };
        executor.submit(validate);
        executor.submit(validate);
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(1, successes.get());
    }
}
