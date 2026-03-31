package mg.yoan.igagain.controller;

import mg.yoan.igagain.entity.Sale;
import mg.yoan.igagain.service.SaleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping("/{orderReference}")
    public ResponseEntity<Sale> createSaleFromOrder(@PathVariable String orderReference) {
        return ResponseEntity.ok(saleService.createSaleFromOrderReference(orderReference));
    }
}
