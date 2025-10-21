package org.example.basic;

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
     * 재고 증가 (락 없음 - Race Condition 발생 가능)
     */
    public void increment(long quantity) {
        this.quantity += quantity;
    }

    /**
     * 재고 감소 (락 없음 - Race Condition 발생 가능)
     */
    public boolean decrement(long quantity) {
        if (this.quantity < quantity) {
            return false;
        }
        this.quantity -= quantity;
        return true;
    }

    public long getQuantity() {
        return quantity;
    }
}
