package org.example.atomic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AtomicLong을 사용한 동시성 제어 테스트
 *
 * -------------------------------------------------------------------------
 * AtomicLong 이란?
 * -------------------------------------------------------------------------
 * Lock-Free 방식의 원자적 연산을 제공하는 클래스
 * - CAS (Compare-And-Swap) 알고리즘 기반
 * - 락을 사용하지 않아 데드락 위험 없음
 * - 경합이 낮을 때 synchronized보다 빠름
 *
 * -------------------------------------------------------------------------
 * CAS (Compare-And-Swap) 동작 원리
 * -------------------------------------------------------------------------
 * "현재 값이 예상 값과 같으면 새 값으로 교체, 아니면 실패"
 *
 *   1. 현재 값 읽기: current = 10
 *   2. 새 값 계산: next = 10 - 1 = 9
 *   3. CAS 시도: compareAndSet(10, 9)
 *      - 메모리 값이 10이면 → 9로 교체, 성공 반환
 *      - 메모리 값이 10이 아니면 → 실패, 1번부터 재시도
 *
 * -------------------------------------------------------------------------
 * 낙관적 락 vs 비관적 락
 * -------------------------------------------------------------------------
 * 비관적 락 (synchronized, ReentrantLock):
 *   - "충돌이 발생할 것"이라 가정
 *   - 미리 락을 잡고 작업
 *   - 락 획득 실패 시 블로킹
 *
 * 낙관적 락 (AtomicLong CAS):
 *   - "충돌이 없을 것"이라 가정
 *   - 일단 작업 후 충돌 검증
 *   - 충돌 시 재시도 (블로킹 없음)
 *
 * -------------------------------------------------------------------------
 * 주의: 경합이 높으면 CAS 재시도가 많아져 성능 저하
 * -------------------------------------------------------------------------
 * - 낮은 경합: AtomicLong > synchronized (락 오버헤드 없음)
 * - 높은 경합: synchronized > AtomicLong (재시도 비용)
 */
@DisplayName("[atomic] AtomicLong 테스트")
class ProductConcurrencyTest {

    /**
     * [테스트 1] AtomicLong 기본 동작 검증
     *
     * 목적: 1000개 스레드가 동시에 +1 할 때 정확히 1000이 되는지 확인
     * 검증: addAndGet()이 원자적으로 동작하는지
     */
    @Test
    @DisplayName("[AtomicLong ✅] 1000개 스레드가 동시에 증가 → 정확한 값")
    void atomic_동시에_재고_증가_시_정확한_값() throws InterruptedException {
        Product product = new Product(1L, "MacBook", 0L, 2000000L);
        int threadCount = 1000;

        long finalQuantity = runConcurrentIncrement(product, threadCount);

        System.out.printf("[AtomicLong ✅] 예상: %d, 실제: %d%n", threadCount, finalQuantity);
        assertEquals(threadCount, finalQuantity, "AtomicLong으로 동기화되어 정확히 " + threadCount + "이어야 함");
    }

    /**
     * [테스트 2] CAS 기반 재고 감소 검증
     *
     * 목적: 재고 100개에서 100번 -1 할 때 정확히 0이 되는지 확인
     * 검증: compareAndSet() 기반의 낙관적 락 패턴이 정상 동작하는지
     *
     * decrement() 내부 동작:
     *   while (true) {
     *       current = quantity.get();      // 1. 현재 값 읽기
     *       if (current < amount) return false;
     *       next = current - amount;       // 2. 새 값 계산
     *       if (quantity.compareAndSet(current, next)) // 3. CAS
     *           return true;
     *       // CAS 실패 시 다시 1번으로
     *   }
     */
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

    /**
     * [테스트 3] 재고 부족 시 CAS 실패 검증
     *
     * 목적: 재고가 부족하면 감소 연산이 실패하고 재고가 유지되는지 확인
     * 검증: CAS 패턴에서 조건 검사(current < amount)가 정상 동작하는지
     */
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
