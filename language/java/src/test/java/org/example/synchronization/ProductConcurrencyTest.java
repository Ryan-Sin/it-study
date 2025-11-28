package org.example.synchronization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * synchronized 키워드를 사용한 동시성 제어 테스트
 *
 * -------------------------------------------------------------------------
 * synchronized 란?
 * -------------------------------------------------------------------------
 * Java의 암묵적 락(Intrinsic Lock) / 모니터 락(Monitor Lock)
 * - 메서드나 블록에 붙여서 한 번에 하나의 스레드만 실행하도록 보장
 * - JVM이 자동으로 락 획득/해제 관리 (개발자가 unlock 호출 불필요)
 *
 * -------------------------------------------------------------------------
 * 동작 방식
 * -------------------------------------------------------------------------
 *   Thread A              Thread B              락 상태
 *   --------              --------              --------
 *   락 획득                                      A 보유
 *   quantity++ 실행        락 획득 시도 → 대기    A 보유
 *   락 해제                                      해제됨
 *                         락 획득                B 보유
 *                         quantity++ 실행        B 보유
 *                         락 해제                해제됨
 *
 * -------------------------------------------------------------------------
 * 특징
 * -------------------------------------------------------------------------
 * - 재진입 가능 (Reentrant): 같은 스레드가 이미 획득한 락을 다시 획득 가능
 * - 블로킹 방식: 락을 획득할 때까지 스레드가 대기 (busy-wait 아님)
 * - 비관적 락: "충돌이 발생할 것"이라 가정하고 미리 잠금
 *
 * -------------------------------------------------------------------------
 * synchronized vs ReentrantLock
 * -------------------------------------------------------------------------
 * synchronized:
 *   - 간단한 문법 (메서드에 키워드 추가)
 *   - 자동 unlock (예외 발생해도 안전)
 *   - tryLock, timeout 불가
 *
 * ReentrantLock:
 *   - 명시적 lock()/unlock() 호출 필요
 *   - tryLock(), lockInterruptibly() 등 세밀한 제어 가능
 *   - 공정성(fairness) 설정 가능
 */
@DisplayName("[synchronization] synchronized 키워드 테스트")
class ProductConcurrencyTest {

    /**
     * [테스트 1] synchronized 기본 동작 검증
     *
     * 목적: 1000개 스레드가 동시에 +1 할 때 정확히 1000이 되는지 확인
     * 검증: synchronized가 상호 배제(Mutual Exclusion)를 보장하는지
     */
    @Test
    @DisplayName("[synchronized ✅] 1000개 스레드가 동시에 증가 → 정확한 값")
    void synchronized_동시에_재고_증가_시_정확한_값() throws InterruptedException {
        Product product = new Product(1L, "MacBook", 0L, 2000000L);
        int threadCount = 1000;

        long finalQuantity = runConcurrentIncrement(product, threadCount);

        System.out.printf("[synchronized ✅] 예상: %d, 실제: %d%n", threadCount, finalQuantity);
        assertEquals(threadCount, finalQuantity, "synchronized로 동기화되어 정확히 " + threadCount + "이어야 함");
    }

    /**
     * [테스트 2] synchronized 복합 연산 검증
     *
     * 목적: 재고 100개에서 100번 -1 할 때 정확히 0이 되는지 확인
     * 검증: "읽기 → 비교 → 쓰기" 복합 연산이 원자적으로 수행되는지
     */
    @Test
    @DisplayName("[synchronized ✅] 재고 감소 테스트")
    void synchronized_재고_감소_테스트() throws InterruptedException {
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
