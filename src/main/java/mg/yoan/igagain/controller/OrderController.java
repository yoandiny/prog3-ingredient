package mg.yoan.igagain.controller;

import mg.yoan.igagain.entity.Order;
import mg.yoan.igagain.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{reference}")
    public ResponseEntity<Order> getOrderByReference(@PathVariable String reference) {
        return ResponseEntity.ok(orderService.findOrderByReference(reference));
    }

    @PostMapping
    public ResponseEntity<Order> saveOrder(@RequestBody Order toSave) {
        return ResponseEntity.ok(orderService.saveOrder(toSave));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Integer id, @RequestBody Order toUpdate) {
        toUpdate.setId(id);
        return ResponseEntity.ok(orderService.updateOrder(toUpdate));
    }
}
