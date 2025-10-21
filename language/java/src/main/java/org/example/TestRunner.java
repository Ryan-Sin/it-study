package org.example;

import org.example.basic.Product;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestRunner {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 동시성 테스트 시작 ===\n");

        // 2개 스레드 테스트
        System.out.println("1. 2개 스레드 테스트 (awaitTermination 있음)");
        test2Threads();

        System.out.println("\n2. 1000개 스레드 테스트 (sleep 포함)");
        test1000ThreadsWithSleep();
    }

    private static void test2Threads() throws InterruptedException {
        Product product = new Product(1L, "MacBook", 0L, 2000000L);
        int threadCount = 2;

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

        long finalQuantity = product.getQuantity();
        long lost = threadCount - finalQuantity;
        System.out.printf("   예상: %d, 실제: %d, 손실: %d%n", threadCount, finalQuantity, lost);
        System.out.println("   → 2개 스레드는 CPU 코어가 충분하면 순차 실행되어 손실이 적습니다");
    }

    private static void test1000ThreadsWithSleep() throws InterruptedException {
        Product product = new Product(1L, "MacBook", 0L, 2000000L);
        int threadCount = 1000;

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
        executor.awaitTermination(15, TimeUnit.SECONDS);

        long finalQuantity = product.getQuantity();
        long lost = threadCount - finalQuantity;
        System.out.printf("   예상: %d, 실제: %d, 손실: %d%n", threadCount, finalQuantity, lost);
        System.out.println("   → Thread.sleep()으로 동시에 깨어나 Lost Update 발생!");
    }
}
