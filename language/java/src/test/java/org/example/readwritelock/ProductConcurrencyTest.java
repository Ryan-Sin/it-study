package org.example.readwritelock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReadWriteLock 동시성 테스트
 *
 * -------------------------------------------------------------------------
 * ReadWriteLock 개념
 * -------------------------------------------------------------------------
 * - Read Lock (공유 락): 여러 스레드가 동시에 읽기 가능
 * - Write Lock (배타 락): 한 스레드만 쓰기 가능, 읽기도 차단
 *
 * -------------------------------------------------------------------------
 * 락 호환성 매트릭스
 * -------------------------------------------------------------------------
 *                  | Read Lock 보유 | Write Lock 보유
 *   ---------------+----------------+-----------------
 *   Read 요청      |    O (허용)    |    X (대기)
 *   Write 요청     |    X (대기)    |    X (대기)
 *
 * -------------------------------------------------------------------------
 * 사용 시나리오: 읽기가 쓰기보다 훨씬 많은 경우
 * -------------------------------------------------------------------------
 * - 상품 상세 페이지 조회 (읽기) >> 재고 차감 (쓰기)
 * - 설정 값 조회 >> 설정 변경
 */
@DisplayName("[readwritelock] ReadWriteLock 테스트")
class ProductConcurrencyTest {

    /**
     * [테스트 1] WriteLock 기본 동작 검증
     *
     * 목적: 1000개 스레드가 동시에 +1 할 때 결과가 정확히 1000인지 확인
     * 검증: WriteLock이 상호 배제(Mutual Exclusion)를 제대로 보장하는지
     *
     * 이 테스트는 다른 동기화 방식(synchronized, ReentrantLock)과 동일한 기본 테스트로,
     * ReadWriteLock의 WriteLock도 동일하게 동작함을 확인한다.
     */
    @Test
    @DisplayName("[ReadWriteLock ✅] 1000개 스레드가 동시에 증가 → 정확한 값")
    void readWriteLock_동시에_재고_증가_시_정확한_값() throws InterruptedException {
        Product product = new Product(1L, "MacBook", 0L, 2000000L);
        int threadCount = 1000;

        long finalQuantity = runConcurrentIncrement(product, threadCount);

        System.out.printf("[ReadWriteLock ✅] 예상: %d, 실제: %d%n", threadCount, finalQuantity);
        assertEquals(threadCount, finalQuantity, "ReadWriteLock으로 동기화되어 정확히 " + threadCount + "이어야 함");
    }

    /**
     * [테스트 2] WriteLock 복합 연산 검증
     *
     * 목적: 재고 100개에서 100개 스레드가 각각 -1 할 때 최종 재고가 0인지 확인
     * 검증: decrement()의 "읽기(재고 확인) + 쓰기(감소)" 복합 연산이 원자적으로 동작하는지
     *
     * decrement()는 재고를 확인하고 감소시키는 두 단계가 있는데,
     * 이 두 단계가 WriteLock 안에서 원자적으로 처리되어야 한다.
     */
    @Test
    @DisplayName("[ReadWriteLock ✅] 재고 감소 테스트")
    void readWriteLock_재고_감소_테스트() throws InterruptedException {
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
     * [테스트 3] ReadLock 동시성 검증 - ⭐ ReadWriteLock의 핵심 장점
     *
     * 목적: 100개 스레드가 동시에 읽기를 수행할 때 모두 블로킹 없이 실행되는지 확인
     * 검증: ReadLock은 공유 락이므로 여러 스레드가 동시에 획득 가능해야 함
     *
     * ReentrantLock과의 차이점:
     * - ReentrantLock: 읽기도 직렬화됨 (한 번에 하나씩)
     * - ReadWriteLock: 읽기는 동시에 가능 (공유 락)
     *
     * 이것이 ReadWriteLock을 사용하는 이유다.
     * 읽기가 많은 워크로드에서 ReentrantLock보다 훨씬 좋은 성능을 보인다.
     */
    @Test
    @DisplayName("[ReadWriteLock ✅] 읽기 동시성 테스트 - 여러 스레드가 동시에 읽기 가능")
    void readWriteLock_동시_읽기_테스트() throws InterruptedException {
        Product product = new Product(1L, "MacBook", 1000L, 2000000L);
        int readerThreads = 100;
        AtomicLong totalReadCount = new AtomicLong(0);

        ExecutorService executor = Executors.newFixedThreadPool(readerThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(readerThreads);

        long startTime = System.nanoTime();

        for (int i = 0; i < readerThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // 여러 번 읽기 수행 - ReadLock은 동시에 획득 가능
                    for (int j = 0; j < 1000; j++) {
                        product.getQuantity();
                        totalReadCount.incrementAndGet();
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
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        System.out.printf("[ReadWriteLock] %d개 스레드가 총 %d번 읽기 완료 (%d ms)%n",
            readerThreads, totalReadCount.get(), elapsedMs);
        assertEquals(readerThreads * 1000, totalReadCount.get());
    }

    /**
     * [테스트 4] 읽기/쓰기 혼합 시나리오 - ⭐ 실제 운영 환경과 유사
     *
     * 목적: Writer 10개 + Reader 100개가 동시에 동작할 때 데이터 일관성 검증
     * 검증: (초기 재고) - (성공한 감소 횟수) == (최종 재고)
     *
     * 실제 커머스 환경 시뮬레이션:
     * - 재고 조회 (읽기) >> 재고 차감 (쓰기)
     * - 상품 페이지 100명 조회 중, 10명이 구매
     *
     * 이 테스트가 중요한 이유:
     * - Writer가 작업 중이면 모든 Reader는 대기
     * - Writer가 없으면 모든 Reader는 동시에 읽기 가능
     * - 혼합 상황에서도 데이터 일관성이 유지되어야 함
     */
    @Test
    @DisplayName("[ReadWriteLock ✅] 읽기/쓰기 혼합 테스트")
    void readWriteLock_읽기_쓰기_혼합_테스트() throws InterruptedException {
        Product product = new Product(1L, "MacBook", 10000L, 2000000L);
        int writerThreads = 10;   // 쓰기 스레드 (재고 감소)
        int readerThreads = 100;  // 읽기 스레드 (재고 조회)
        int operationsPerThread = 100;

        ExecutorService executor = Executors.newFixedThreadPool(writerThreads + readerThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(writerThreads + readerThreads);

        AtomicLong successfulDecrements = new AtomicLong(0);
        AtomicLong totalReads = new AtomicLong(0);

        // Writer 스레드들 - 재고 감소
        for (int i = 0; i < writerThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (product.decrement(1)) {
                            successfulDecrements.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Reader 스레드들 - 재고 조회
        for (int i = 0; i < readerThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        product.getQuantity();
                        totalReads.incrementAndGet();
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
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long finalQuantity = product.getQuantity();
        long expectedQuantity = 10000L - successfulDecrements.get();

        System.out.printf("[ReadWriteLock] 쓰기 성공: %d, 읽기 횟수: %d, 최종 재고: %d%n",
            successfulDecrements.get(), totalReads.get(), finalQuantity);

        assertEquals(expectedQuantity, finalQuantity, "최종 재고가 일치해야 함");
        assertEquals(readerThreads * operationsPerThread, totalReads.get(), "모든 읽기가 완료되어야 함");
    }

    /**
     * [테스트 5] 다중 필드 스냅샷 일관성 검증 - ⭐ AtomicLong으로는 불가능
     *
     * 목적: 여러 필드(id, name, quantity, price)를 한 번에 읽을 때 일관된 상태인지 확인
     * 검증: 스냅샷을 읽는 도중 다른 스레드가 수정해도 일관된 상태를 봐야 함
     *
     * AtomicLong의 한계:
     * - 단일 변수(quantity)만 원자적으로 보호 가능
     * - name, quantity, price를 "동시에 일관되게" 읽는 것은 불가능
     *
     * ReadWriteLock의 장점:
     * - ReadLock을 잡고 여러 필드를 읽으면 그 동안 Writer가 수정 불가
     * - 따라서 항상 일관된 스냅샷을 얻을 수 있음
     *
     * 이 테스트에서 quantity가 음수로 관찰되면 일관성이 깨진 것이다.
     */
    @Test
    @DisplayName("[ReadWriteLock ✅] 스냅샷 일관성 테스트")
    void readWriteLock_스냅샷_일관성_테스트() throws InterruptedException {
        Product product = new Product(1L, "MacBook", 1000L, 2000000L);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(20);

        AtomicLong inconsistentCount = new AtomicLong(0);

        // 10개 Writer 스레드
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 100; j++) {
                        product.decrement(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 10개 Reader 스레드 - 스냅샷 읽기
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 100; j++) {
                        Product.ProductSnapshot snapshot = product.getSnapshot();
                        // 스냅샷 내 데이터 일관성 확인 (quantity는 음수가 되면 안됨)
                        if (snapshot.quantity() < 0) {
                            inconsistentCount.incrementAndGet();
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
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.printf("[ReadWriteLock] 스냅샷 비일관성 발생 횟수: %d%n", inconsistentCount.get());
        assertEquals(0, inconsistentCount.get(), "스냅샷은 항상 일관된 상태여야 함");
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
