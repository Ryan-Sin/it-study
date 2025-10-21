package org.example.atomic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("[atomic] AtomicLong 테스트")
class ProductConcurrencyTest {

    @Test
    @DisplayName("[AtomicLong ✅] 1000개 스레드가 동시에 증가 → 정확한 값")
    void atomic_동시에_재고_증가_시_정확한_값() throws InterruptedException {
        Product product = new Product(1L, "MacBook", 0L, 2000000L);
        int threadCount = 1000;

        long finalQuantity = runConcurrentIncrement(product, threadCount);

        System.out.printf("[AtomicLong ✅] 예상: %d, 실제: %d%n", threadCount, finalQuantity);
        assertEquals(threadCount, finalQuantity, "AtomicLong으로 동기화되어 정확히 " + threadCount + "이어야 함");
    }

    @Test
    @DisplayName("[AtomicLong ✅] 재고 감소 테스트 - CAS 패턴")
    void atomic_재고_감소_테스트() throws InterruptedException {
        Product product = new Product(1L, "MacBook", 100L, 2000000L);
        int threadCount = 100;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    product.decrement(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(0L, product.getQuantity(), "100개 재고에서 100번 감소하면 0이어야 함");
    }

    @Test
    @DisplayName("[AtomicLong ✅] 재고 부족 시 감소 실패")
    void atomic_재고_부족_시_감소_실패() {
        Product product = new Product(1L, "MacBook", 5L, 2000000L);

        boolean result = product.decrement(10);

        assertFalse(result, "재고가 부족하면 감소 실패해야 함");
        assertEquals(5L, product.getQuantity(), "재고는 변경되지 않아야 함");
    }

    private long runConcurrentIncrement(Product product, int threadCount) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    product.increment(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        return product.getQuantity();
    }
}
