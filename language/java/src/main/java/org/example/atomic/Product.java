package org.example.atomic;

import java.util.concurrent.atomic.AtomicLong;

public class Product {
    private Long id;
    private String name;
    private final AtomicLong quantity;
    private final long price;

    public Product(Long id, String name, long quantity, long price) {
        this.id = id;
        this.name = name;
        this.quantity = new AtomicLong(quantity);
        this.price = price;
    }

    /**
     * AtomicLong의 addAndGet으로 원자적 증가 연산
     * CAS(Compare-And-Swap) 알고리즘 사용
     */
    public void increment(long quantity) {
        this.quantity.addAndGet(quantity);
    }

    /**
     * AtomicLong의 compareAndSet으로 원자적 감소 연산
     * 재고가 충분할 때만 감소 (낙관적 락 패턴)
     */
    public boolean decrement(long quantity) {
        while (true) {
            long current = this.quantity.get();
            if (current < quantity) {
                return false;
            }
            long next = current - quantity;
            if (this.quantity.compareAndSet(current, next)) {
                return true;
            }
            // CAS 실패 시 재시도 (다른 스레드가 먼저 변경한 경우)
        }
    }

    public long getQuantity() {
        return quantity.get();
    }
}
