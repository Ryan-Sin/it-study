package org.example.synchronization;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Product {
    private final Lock lock = new ReentrantLock();

    private Long id;
    private String name;
    private volatile long quantity;
    private final long price;

    public Product(Long id, String name, long quantity, long price) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public void increment(long quantity) {
        lock.lock();
        try {
            this.quantity += quantity;
        } finally {
            lock.unlock();
        }
    }

    public boolean decrement(long quantity) {
        lock.lock();
        try {
            if (this.quantity >= quantity) {
                this.quantity -= quantity;
                return true;
            }
            return false;
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
