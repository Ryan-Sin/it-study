package org.example.explicitlock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReentrantLock을 사용한 동시성 제어 테스트
 *
 * -------------------------------------------------------------------------
 * ReentrantLock 이란?
 * -------------------------------------------------------------------------
 * Java의 명시적 락 (Explicit Lock)
 * - java.util.concurrent.locks 패키지
 * - lock()/unlock()을 명시적으로 호출하여 락 제어
 * - synchronized보다 세밀한 제어 가능
 *
 * -------------------------------------------------------------------------
 * 기본 사용법
 * -------------------------------------------------------------------------
 * Lock lock = new ReentrantLock();
 *
 * lock.lock();           // 락 획득 (블로킹)
 * try {
 *     // 임계 영역
 * } finally {
 *     lock.unlock();     // 반드시 finally에서 해제!
 * }
 *
 * -------------------------------------------------------------------------
 * Reentrant (재진입) 의미
 * -------------------------------------------------------------------------
 * 같은 스레드가 이미 획득한 락을 다시 획득 가능:
 *
 *   void outer() {
 *       lock.lock();      // 락 획득 (count = 1)
 *       inner();          // 내부에서 또 lock() 호출해도 OK
 *       lock.unlock();    // count = 0 → 해제
 *   }
 *   void inner() {
 *       lock.lock();      // 같은 스레드 → 허용 (count = 2)
 *       lock.unlock();    // count = 1
 *   }
 *
 * -------------------------------------------------------------------------
 * synchronized와의 차이점
 * -------------------------------------------------------------------------
 *                      | synchronized    | ReentrantLock
 *   -------------------+-----------------+------------------
 *   락 획득/해제        | 자동            | 수동 (lock/unlock)
 *   tryLock()          | 불가            | 가능
 *   timeout            | 불가            | 가능
 *   lockInterruptibly  | 불가            | 가능
 *   공정성(fairness)   | 불가            | 설정 가능
 *   Condition          | wait/notify     | newCondition()
 */
@DisplayName("[explicitlock] ReentrantLock 테스트")
class ProductConcurrencyTest {

    /**
     * [테스트 1] ReentrantLock 기본 동작 검증
     *
     * 목적: 1000개 스레드가 동시에 +1 할 때 정확히 1000이 되는지 확인
     * 검증: ReentrantLock이 상호 배제(Mutual Exclusion)를 보장하는지
     */
    @Test
    @DisplayName("[ReentrantLock ✅] 1000개 스레드가 동시에 증가 → 정확한 값")
    void reentrantLock_동시에_재고_증가_시_정확한_값() throws InterruptedException {
        Product product = new Product(1L, "MacBook", 0L, 2000000L);
        int threadCount = 1000;

        long finalQuantity = runConcurrentIncrement(product, threadCount);

        System.out.printf("[ReentrantLock ✅] 예상: %d, 실제: %d%n", threadCount, finalQuantity);
        assertEquals(threadCount, finalQuantity, "ReentrantLock으로 동기화되어 정확히 " + threadCount + "이어야 함");
    }

    /**
     * [테스트 2] ReentrantLock 복합 연산 검증
     *
     * 목적: 재고 100개에서 100번 -1 할 때 정확히 0이 되는지 확인
     * 검증: "읽기 → 비교 → 쓰기" 복합 연산이 원자적으로 수행되는지
     */
    @Test
    @DisplayName("[ReentrantLock ✅] 재고 감소 테스트")
    void reentrantLock_재고_감소_테스트() throws InterruptedException {
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
