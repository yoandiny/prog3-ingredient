package mg.yoan.igagain.entity;

import java.time.Instant;

public class Sale {
    private int id;
    private Instant creationDateTime;
    private Order order;

    public Sale() {
    }

    public Sale(int id, Instant creationDateTime, Order order) {
        this.id = id;
        this.creationDateTime = creationDateTime;
        this.order = order;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Instant getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(Instant creationDateTime) {
        this.creationDateTime = creationDateTime;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
