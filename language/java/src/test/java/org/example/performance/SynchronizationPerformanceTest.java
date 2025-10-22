package org.example.performance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 동기화 방식별 성능 비교 테스트 - 재고 감소 시나리오 (이벤트/플래시 세일)
 *
 * 비교 대상:
 * 1. AtomicLong (CAS, Lock-Free)
 * 2. synchronized (암묵적 락)
 * 3. ReentrantLock (명시적 락)
 *
 * 테스트 시나리오:
 * - 낮은 경합 (Low Contention): 4 threads, 1,000 재고, 각 1씩 감소
 * - 중간 경합 (Medium Contention): 50 threads, 10,000 재고, 각 1씩 감소
 * - 높은 경합 (High Contention): 200 threads, 100,000 재고, 각 1씩 감소
 * - 극한 경합 (Extreme Contention): 500 threads, 500,000 재고, 각 1씩 감소
 *
 * 측정 항목:
 * - 처리 시간 (성능)
 * - 성공/실패 횟수 (정확성)
 * - CAS 재시도 횟수 (경합 정도)
 * - 최종 재고 (데이터 일관성)
 */
@DisplayName("[Performance] 동기화 방식 성능 비교")
class SynchronizationPerformanceTest {


    /**
     * 테스트 결과를 담는 DTO
     */
    static class TestResult {
        final long elapsedTimeNanos;
        final long successCount;
        final long failCount;
        final long finalStock;

        TestResult(long elapsedTimeNanos, long successCount, long failCount, long finalStock) {
            this.elapsedTimeNanos = elapsedTimeNanos;
            this.successCount = successCount;
            this.failCount = failCount;
            this.finalStock = finalStock;
        }
    }

    @Test
    @DisplayName("낮은 경합 상황 (4 threads, 1,000 재고)")
    void 낮은_경합_성능_비교() throws InterruptedException {
        int threads = 4;
        long initialStock = 1000L;
        int operationsPerThread = 250; // 총 1,000번 감소 시도

        System.out.println("\n========== 낮은 경합 상황 (4 threads, 1,000 재고) ==========");
        System.out.println("초기 재고: " + initialStock + ", 스레드당 시도: " + operationsPerThread);
        System.out.println("예상: 모든 시도가 성공하고 재고가 0이 됨");

        TestResult atomicResult = measureAtomicPerformance(threads, operationsPerThread, initialStock);
        TestResult syncResult = measureSynchronizedPerformance(threads, operationsPerThread, initialStock);
        TestResult lockResult = measureReentrantLockPerformance(threads, operationsPerThread, initialStock);

        printResults("낮은 경합", atomicResult, syncResult, lockResult, initialStock);
    }

    @Test
    @DisplayName("중간 경합 상황 (50 threads, 10,000 재고)")
    void 중간_경합_성능_비교() throws InterruptedException {
        int threads = 50;
        long initialStock = 10000L;
        int operationsPerThread = 200; // 총 10,000번 감소 시도

        System.out.println("\n========== 중간 경합 상황 (50 threads, 10,000 재고) ==========");
        System.out.println("초기 재고: " + initialStock + ", 스레드당 시도: " + operationsPerThread);
        System.out.println("예상: 모든 시도가 성공하고 재고가 0이 됨");

        TestResult atomicResult = measureAtomicPerformance(threads, operationsPerThread, initialStock);
        TestResult syncResult = measureSynchronizedPerformance(threads, operationsPerThread, initialStock);
        TestResult lockResult = measureReentrantLockPerformance(threads, operationsPerThread, initialStock);

        printResults("중간 경합", atomicResult, syncResult, lockResult, initialStock);
    }

    @Test
    @DisplayName("높은 경합 상황 (200 threads, 100,000 재고)")
    void 높은_경합_성능_비교() throws InterruptedException {
        int threads = 200;
        long initialStock = 100000L;
        int operationsPerThread = 500; // 총 100,000번 감소 시도

        System.out.println("\n========== 높은 경합 상황 (200 threads, 100,000 재고) ==========");
        System.out.println("초기 재고: " + initialStock + ", 스레드당 시도: " + operationsPerThread);
        System.out.println("예상: 모든 시도가 성공하고 재고가 0이 됨");

        TestResult atomicResult = measureAtomicPerformance(threads, operationsPerThread, initialStock);
        TestResult syncResult = measureSynchronizedPerformance(threads, operationsPerThread, initialStock);
        TestResult lockResult = measureReentrantLockPerformance(threads, operationsPerThread, initialStock);

        printResults("높은 경합", atomicResult, syncResult, lockResult, initialStock);
    }

    @Test
    @DisplayName("극한 경합 상황 (500 threads, 500,000 재고) - CAS 재시도 부담 확인")
    void 극한_경합_성능_비교() throws InterruptedException {
        int threads = 500;
        long initialStock = 500000L;
        int operationsPerThread = 1000; // 총 500,000번 감소 시도

        System.out.println("\n========== 극한 경합 상황 (500 threads, 500,000 재고) ==========");
        System.out.println("초기 재고: " + initialStock + ", 스레드당 시도: " + operationsPerThread);
        System.out.println("예상: 모든 시도가 성공하고 재고가 0이 됨");
        System.out.println("⚠️  AtomicLong의 CAS 재시도가 성능에 미치는 영향 확인");

        TestResult atomicResult = measureAtomicPerformance(threads, operationsPerThread, initialStock);
        TestResult syncResult = measureSynchronizedPerformance(threads, operationsPerThread, initialStock);
        TestResult lockResult = measureReentrantLockPerformance(threads, operationsPerThread, initialStock);

        printResults("극한 경합", atomicResult, syncResult, lockResult, initialStock);
    }

    /**
     * AtomicLong 성능 측정 (재고 감소 시나리오)
     */
    private TestResult measureAtomicPerformance(int threads, int operationsPerThread, long initialStock)
            throws InterruptedException {
        org.example.atomic.Product product = new org.example.atomic.Product(1L, "Item", initialStock, 1000L);

        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failCount = new AtomicLong(0);

        long startTime = System.nanoTime();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            boolean success = product.decrement(1);
                            if (success) {
                                successCount.incrementAndGet();
                            } else {
                                failCount.incrementAndGet();
                            }
                        } catch (IllegalStateException e) {
                            // CAS 재시도 횟수 초과 시 실패로 카운트
                            failCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 동시 시작
        endLatch.await();
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        long elapsedTime = System.nanoTime() - startTime;
        long finalStock = product.getQuantity();

        System.out.printf("[AtomicLong] %d ms | 성공: %d, 실패: %d, 최종 재고: %d%n",
            elapsedTime / 1_000_000, successCount.get(), failCount.get(), finalStock);

        return new TestResult(elapsedTime, successCount.get(), failCount.get(), finalStock);
    }

    /**
     * synchronized 성능 측정 (재고 감소 시나리오)
     */
    private TestResult measureSynchronizedPerformance(int threads, int operationsPerThread, long initialStock)
            throws InterruptedException {
        org.example.synchronization.Product product = new org.example.synchronization.Product(1L, "Item", initialStock, 1000L);

        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failCount = new AtomicLong(0);

        long startTime = System.nanoTime();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        boolean success = product.decrement(1);
                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        long elapsedTime = System.nanoTime() - startTime;
        long finalStock = product.getQuantity();

        System.out.printf("[synchronized] %d ms | 성공: %d, 실패: %d, 최종 재고: %d%n",
            elapsedTime / 1_000_000, successCount.get(), failCount.get(), finalStock);

        return new TestResult(elapsedTime, successCount.get(), failCount.get(), finalStock);
    }

    /**
     * ReentrantLock 성능 측정 (재고 감소 시나리오)
     */
    private TestResult measureReentrantLockPerformance(int threads, int operationsPerThread, long initialStock)
            throws InterruptedException {
        org.example.explicitlock.Product product = new org.example.explicitlock.Product(1L, "Item", initialStock, 1000L);

        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failCount = new AtomicLong(0);

        long startTime = System.nanoTime();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        boolean success = product.decrement(1);
                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        long elapsedTime = System.nanoTime() - startTime;
        long finalStock = product.getQuantity();

        System.out.printf("[ReentrantLock] %d ms | 성공: %d, 실패: %d, 최종 재고: %d%n",
            elapsedTime / 1_000_000, successCount.get(), failCount.get(), finalStock);

        return new TestResult(elapsedTime, successCount.get(), failCount.get(), finalStock);
    }

    /**
     * 결과 출력 및 분석 + Assertion
     */
    private void printResults(String scenario, TestResult atomic, TestResult sync, TestResult lock, long initialStock) {
        System.out.println("\n========== 결과 분석 ==========");

        // 1. 성능 비교 (처리 시간) - 밀리초 단위로 명확하게 표시
        long atomicMs = atomic.elapsedTimeNanos / 1_000_000;
        long syncMs = sync.elapsedTimeNanos / 1_000_000;
        long lockMs = lock.elapsedTimeNanos / 1_000_000;
        long baseTime = Math.min(Math.min(atomic.elapsedTimeNanos, sync.elapsedTimeNanos), lock.elapsedTimeNanos);

        System.out.println("\n[처리 시간 비교]");
        System.out.println("┌─────────────────┬──────────────┬──────────────┐");
        System.out.println("│ 동기화 방식        │ 처리 시간(ms)  │ 기준 대비      │");
        System.out.println("├─────────────────┼──────────────┼──────────────┤");
        System.out.printf("│ AtomicLong      │ %,10d ms │ %.2fx        │%n", atomicMs, (double) atomic.elapsedTimeNanos / baseTime);
        System.out.printf("│ synchronized    │ %,10d ms │ %.2fx        │%n", syncMs, (double) sync.elapsedTimeNanos / baseTime);
        System.out.printf("│ ReentrantLock   │ %,10d ms │ %.2fx        │%n", lockMs, (double) lock.elapsedTimeNanos / baseTime);
        System.out.println("└─────────────────┴──────────────┴──────────────┘");

        // 2. 동시성 정확성 검증
        System.out.println("\n[동시성 정확성 검증]");
        System.out.println("┌─────────────────┬──────────────┬──────────────┬──────────────┐");
        System.out.println("│ 동기화 방식        │ 성공          │ 실패          │ 최종 재고      │");
        System.out.println("├─────────────────┼──────────────┼──────────────┼──────────────┤");
        System.out.printf("│ AtomicLong      │ %,10d   │ %,10d   │ %,10d   │%n",
            atomic.successCount, atomic.failCount, atomic.finalStock);
        System.out.printf("│ synchronized    │ %,10d   │ %,10d   │ %,10d   │%n",
            sync.successCount, sync.failCount, sync.finalStock);
        System.out.printf("│ ReentrantLock   │ %,10d   │ %,10d   │ %,10d   │%n",
            lock.successCount, lock.failCount, lock.finalStock);
        System.out.println("└─────────────────┴──────────────┴──────────────┴──────────────┘");

        // 3. 데이터 일관성 검증 (초기재고 - 성공횟수 = 최종재고)
        System.out.println("\n[데이터 일관성 검증]");
        long expectedFinalStock = initialStock - atomic.successCount;
        boolean atomicCorrect = (initialStock - atomic.successCount) == atomic.finalStock;
        boolean syncCorrect = (initialStock - sync.successCount) == sync.finalStock;
        boolean lockCorrect = (initialStock - lock.successCount) == lock.finalStock;

        System.out.printf("AtomicLong:      %s (초기: %,d, 성공: %,d, 예상: %,d, 실제: %,d)%n",
            atomicCorrect ? "✅ 일관성 유지" : "❌ 불일치", initialStock, atomic.successCount, expectedFinalStock, atomic.finalStock);
        System.out.printf("synchronized:    %s (초기: %,d, 성공: %,d, 예상: %,d, 실제: %,d)%n",
            syncCorrect ? "✅ 일관성 유지" : "❌ 불일치", initialStock, sync.successCount, initialStock - sync.successCount, sync.finalStock);
        System.out.printf("ReentrantLock:   %s (초기: %,d, 성공: %,d, 예상: %,d, 실제: %,d)%n",
            lockCorrect ? "✅ 일관성 유지" : "❌ 불일치", initialStock, lock.successCount, initialStock - lock.successCount, lock.finalStock);

        // 4. 승자 판정
        String winner;
        if (atomic.elapsedTimeNanos <= sync.elapsedTimeNanos && atomic.elapsedTimeNanos <= lock.elapsedTimeNanos) {
            winner = "AtomicLong (Lock-Free)";
        } else if (sync.elapsedTimeNanos <= lock.elapsedTimeNanos) {
            winner = "synchronized (암묵적 락)";
        } else {
            winner = "ReentrantLock (명시적 락)";
        }

        System.out.printf("\n✅ %s 시나리오 최고 성능: %s%n", scenario, winner);
        System.out.println("=====================================\n");

        // 5. ✅ Assertion: 데이터 일관성 검증 (필수)
        assertEquals(initialStock - atomic.successCount, atomic.finalStock,
            String.format("AtomicLong: 최종 재고 불일치 (초기: %,d, 성공: %,d, 예상: %,d, 실제: %,d)",
                initialStock, atomic.successCount, initialStock - atomic.successCount, atomic.finalStock));
        assertEquals(initialStock - sync.successCount, sync.finalStock,
            String.format("synchronized: 최종 재고 불일치 (초기: %,d, 성공: %,d, 예상: %,d, 실제: %,d)",
                initialStock, sync.successCount, initialStock - sync.successCount, sync.finalStock));
        assertEquals(initialStock - lock.successCount, lock.finalStock,
            String.format("ReentrantLock: 최종 재고 불일치 (초기: %,d, 성공: %,d, 예상: %,d, 실제: %,d)",
                initialStock, lock.successCount, initialStock - lock.successCount, lock.finalStock));

        // 6. ✅ Assertion: 모든 시도가 성공 또는 실패로 처리되었는지 검증
        long totalOperations = initialStock; // 모든 시나리오에서 총 시도 횟수 = 초기 재고
        assertEquals(totalOperations, atomic.successCount + atomic.failCount,
            String.format("AtomicLong: 성공+실패 횟수가 총 시도 횟수와 불일치 (성공: %,d, 실패: %,d, 합계: %,d, 예상: %,d)",
                atomic.successCount, atomic.failCount, atomic.successCount + atomic.failCount, totalOperations));
        assertEquals(totalOperations, sync.successCount + sync.failCount,
            String.format("synchronized: 성공+실패 횟수가 총 시도 횟수와 불일치 (성공: %,d, 실패: %,d, 합계: %,d, 예상: %,d)",
                sync.successCount, sync.failCount, sync.successCount + sync.failCount, totalOperations));
        assertEquals(totalOperations, lock.successCount + lock.failCount,
            String.format("ReentrantLock: 성공+실패 횟수가 총 시도 횟수와 불일치 (성공: %,d, 실패: %,d, 합계: %,d, 예상: %,d)",
                lock.successCount, lock.failCount, lock.successCount + lock.failCount, totalOperations));

        // 7. ✅ Assertion: 최종 재고가 음수가 되지 않는지 검증
        assertTrue(atomic.finalStock >= 0,
            String.format("AtomicLong: 최종 재고가 음수가 될 수 없음 (실제: %,d)", atomic.finalStock));
        assertTrue(sync.finalStock >= 0,
            String.format("synchronized: 최종 재고가 음수가 될 수 없음 (실제: %,d)", sync.finalStock));
        assertTrue(lock.finalStock >= 0,
            String.format("ReentrantLock: 최종 재고가 음수가 될 수 없음 (실제: %,d)", lock.finalStock));

        // 8. ✅ Assertion: 성능 회귀 검증 (모든 방식이 합리적인 시간 내에 완료되어야 함)
        // 시나리오별 예상 최대 처리 시간 (밀리초 단위, 여유를 두고 설정)
        long maxAcceptableMs = switch(scenario) {
            case "낮은 경합" -> 1000;      // 1초
            case "중간 경합" -> 2000;      // 2초
            case "높은 경합" -> 5000;      // 5초
            case "극한 경합" -> 10000;     // 10초
            default -> 10000;
        };

        assertTrue(atomicMs <= maxAcceptableMs,
            String.format("AtomicLong: 처리 시간이 너무 오래 걸림 (실제: %,d ms, 최대 허용: %,d ms)", atomicMs, maxAcceptableMs));
        assertTrue(syncMs <= maxAcceptableMs,
            String.format("synchronized: 처리 시간이 너무 오래 걸림 (실제: %,d ms, 최대 허용: %,d ms)", syncMs, maxAcceptableMs));
        assertTrue(lockMs <= maxAcceptableMs,
            String.format("ReentrantLock: 처리 시간이 너무 오래 걸림 (실제: %,d ms, 최대 허용: %,d ms)", lockMs, maxAcceptableMs));
    }
}
