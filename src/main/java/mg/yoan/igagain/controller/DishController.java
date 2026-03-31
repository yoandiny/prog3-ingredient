package mg.yoan.igagain.controller;

import mg.yoan.igagain.entity.Dish;
import mg.yoan.igagain.entity.DishIngredient;
import mg.yoan.igagain.exception.BadRequestException;
import mg.yoan.igagain.service.DishService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dishes")
public class DishController {

    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping
    public ResponseEntity<List<Dish>> getDishes() {
        return ResponseEntity.ok(dishService.findAllDishes());
    }

    @PutMapping("/{id}/ingredients")
    public ResponseEntity<Dish> updateDishIngredients(
            @PathVariable Integer id,
            @RequestBody(required = false) List<DishIngredient> ingredients) {
        if (ingredients == null) {
            throw new BadRequestException("Request body containing list of ingredients is mandatory");
        }
        return ResponseEntity.ok(dishService.updateDishIngredients(id, ingredients));
    }

    @GetMapping("/{id}/cost")
    public ResponseEntity<Double> getDishCost(@PathVariable Integer id) {
        return ResponseEntity.ok(dishService.getDishCost(id));
    }
}
