package mg.yoan.igagain.service;

import mg.yoan.igagain.entity.Order;
import mg.yoan.igagain.exception.ResourceNotFoundException;
import mg.yoan.igagain.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order findOrderByReference(String reference) {
        Order order = orderRepository.findOrderByReference(reference);
        if (order == null) {
            throw new ResourceNotFoundException("Order with reference " + reference + " is not found");
        }
        return order;
    }

    public Order saveOrder(Order toSave) {
        return orderRepository.saveOrder(toSave);
    }

    public Order updateOrder(Order toUpdate) {
        return orderRepository.updateOrder(toUpdate);
    }
}
