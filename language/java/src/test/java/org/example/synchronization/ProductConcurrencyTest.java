package org.example.synchronization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("동시성 테스트")
class ProductConcurrencyTest {

    @Test
    @DisplayName("[락 ✅] 2개 스레드가 동시에 값을 변경 → 최종값 2")
    void 락_있음_동시에_재고_증가_시_정확한_값() throws InterruptedException {
        Product product = new Product(1L, "MacBook", 0L, 2000000L);
        int threadCount = 2;

        long finalQuantity = runConcurrentIncrement(product, threadCount);

        System.out.printf("[락 ✅] 예상: %d, 실제: %d%n", threadCount, finalQuantity);
        assertEquals(threadCount, finalQuantity, "락이 있으므로 정확히 " + threadCount + "이어야 함");
    }

    @Test
    @DisplayName("[락 ❌] 1000개 스레드가 동시에 증가 → Lost Update 발생")
    void 락_없음_동시에_재고_증가_시_손실_발생() throws InterruptedException {
        ProductWithoutLock product = new ProductWithoutLock(1L, "MacBook", 0L, 2000000L);
        /**
         * 1000개 스레드를 사용하는 이유 (OS 레벨 관점):
         *
         * 1. 타임슬라이스(Time Slice) vs 연산 속도
         *    - OS 스케줄러의 타임슬라이스: 약 5~10ms
         *    - quantity++ 연산 시간: 약 10ns (나노초)
         *    - 한 번의 타임슬라이스 동안 약 500,000번의 연산 가능
         *    → 소수의 스레드는 순차 실행되어 동시성 이슈가 거의 발생하지 않음
         *
         * 2. 컨텍스트 스위칭(Context Switching) 빈도
         *    - 스레드 수가 CPU 코어 수(보통 8~16)보다 훨씬 많으면
         *      OS 스케줄러가 강제로 컨텍스트 스위칭을 자주 수행
         *    - 1000개 스레드 → CPU 코어당 100개 이상의 대기 스레드
         *    → Read-Modify-Write 중간에 끼어들 확률 급증
         *
         * 3. Thread.sleep(100)의 효과
         *    - 스레드가 자발적으로 CPU를 양보 (WAITING 상태로 전환)
         *    - 모든 스레드가 동시에 quantity 값을 읽은 후 sleep
         *    - 100ms 후 거의 동시에 깨어나 동일한 값으로 덮어씀
         *    → Lost Update 확실히 재현
         */
        int threadCount = 1000;

        long finalQuantity = runConcurrentIncrementWithoutLock(product, threadCount);

        long lost = threadCount - finalQuantity;
        System.out.printf("[락 ❌] 예상: %d, 실제: %d, 손실: %d%n", threadCount, finalQuantity, lost);

        assertTrue(finalQuantity < threadCount, "락이 없으므로 Lost Update로 손실이 발생해야 함");
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

    private long runConcurrentIncrementWithoutLock(ProductWithoutLock product, int threadCount) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(100);
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
