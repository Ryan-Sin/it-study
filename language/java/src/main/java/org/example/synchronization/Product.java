package org.example.synchronization;

public class Product {
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
     * synchronized 키워드로 메서드 전체를 임계 영역으로 지정
     */
    public synchronized void increment(long quantity) {
        this.quantity += quantity;
    }

    /**
     * synchronized 키워드로 메서드 전체를 임계 영역으로 지정
     */
    public synchronized boolean decrement(long quantity) {
        if (this.quantity < quantity) {
            return false;
        }
        this.quantity -= quantity;
        return true;
    }

    public synchronized long getQuantity() {
        return quantity;
    }
}
