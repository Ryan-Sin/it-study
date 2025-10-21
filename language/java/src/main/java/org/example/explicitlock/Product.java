package org.example.explicitlock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Product {
    private final Lock lock = new ReentrantLock();

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
     * ReentrantLock으로 명시적 락 제어
     * try-finally를 사용하여 반드시 unlock 보장
     */
    public void increment(long quantity) {
        lock.lock();
        try {
            this.quantity += quantity;
        } finally {
            lock.unlock();
        }
    }

    /**
     * ReentrantLock으로 명시적 락 제어
     * try-finally를 사용하여 반드시 unlock 보장
     */
    public boolean decrement(long quantity) {
        lock.lock();
        try {
            if (this.quantity < quantity) {
                return false;
            }
            this.quantity -= quantity;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public long getQuantity() {
        lock.lock();
        try {
            return quantity;
        } finally {
            lock.unlock();
        }
    }
}
