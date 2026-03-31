package mg.yoan.igagain.controller;

import mg.yoan.igagain.entity.Ingredient;
import mg.yoan.igagain.entity.StockValue;
import mg.yoan.igagain.entity.Unit;
import mg.yoan.igagain.service.IngredientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public ResponseEntity<List<Ingredient>> getIngredients() {
        return ResponseEntity.ok(ingredientService.findAllIngredients());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ingredient> getIngredientById(@PathVariable Integer id) {
        return ResponseEntity.ok(ingredientService.findIngredientById(id));
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<StockValue> getIngredientStock(
            @PathVariable Integer id,
            @RequestParam("at") Instant temporal,
            @RequestParam("unit") Unit unit) {
        return ResponseEntity.ok(ingredientService.getIngredientStock(id, temporal, unit));
    }

    @PostMapping
    public ResponseEntity<Ingredient> saveIngredient(@RequestBody Ingredient toSave) {
        return ResponseEntity.ok(ingredientService.saveIngredient(toSave));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<Ingredient>> createIngredients(@RequestBody List<Ingredient> newIngredients) {
        return ResponseEntity.ok(ingredientService.createIngredients(newIngredients));
    }
}
