package org.example.readwritelock;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ReadWriteLock을 사용한 동시성 제어
 *
 * 특징:
 * - 읽기 락(Read Lock): 여러 스레드가 동시에 읽기 가능 (공유 락)
 * - 쓰기 락(Write Lock): 한 스레드만 쓰기 가능 (배타 락)
 * - 읽기가 많고 쓰기가 적은 시나리오에서 ReentrantLock보다 성능 우수
 *
 * 사용 시나리오:
 * - 재고 조회(읽기)가 재고 변경(쓰기)보다 훨씬 많은 커머스 환경
 * - 캐시 데이터 읽기/갱신
 * - 설정 값 조회/변경
 */
public class Product {
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    private Long id;
    private String name;
    private long quantity;
    private final long price;

    public Product(Long id, String name, long quantity, long price) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    /**
     * 재고 증가 - Write Lock 사용
     * 쓰기 작업이므로 배타적 락 필요
     */
    public void increment(long quantity) {
        rwLock.writeLock().lock();
        try {
            this.quantity += quantity;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 재고 감소 - Write Lock 사용
     * 읽기(재고 확인) + 쓰기(감소)가 원자적으로 이루어져야 함
     */
    public boolean decrement(long quantity) {
        rwLock.writeLock().lock();
        try {
            if (this.quantity < quantity) {
                return false;
            }
            this.quantity -= quantity;
            return true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 재고 조회 - Read Lock 사용
     * 여러 스레드가 동시에 읽기 가능
     */
    public long getQuantity() {
        rwLock.readLock().lock();
        try {
            return quantity;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 상품 정보 전체 조회 - Read Lock 사용
     * 여러 필드를 일관된 상태로 읽기
     */
    public ProductSnapshot getSnapshot() {
        rwLock.readLock().lock();
        try {
            return new ProductSnapshot(id, name, quantity, price);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 상품 정보 스냅샷 (불변 객체)
     */
    public record ProductSnapshot(Long id, String name, long quantity, long price) {}
}
