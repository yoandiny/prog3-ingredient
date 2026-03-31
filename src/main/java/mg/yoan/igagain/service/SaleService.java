package mg.yoan.igagain.service;

import mg.yoan.igagain.entity.Order;
import mg.yoan.igagain.entity.Sale;
import mg.yoan.igagain.repository.SaleRepository;
import org.springframework.stereotype.Service;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final OrderService orderService;

    public SaleService(SaleRepository saleRepository, OrderService orderService) {
        this.saleRepository = saleRepository;
        this.orderService = orderService;
    }

    public Sale createSaleFromOrderReference(String reference) {
        Order order = orderService.findOrderByReference(reference);
        return saleRepository.createSaleFrom(order);
    }
}
