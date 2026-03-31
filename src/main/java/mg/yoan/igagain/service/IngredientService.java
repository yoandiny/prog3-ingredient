package mg.yoan.igagain.service;

import mg.yoan.igagain.entity.Ingredient;
import mg.yoan.igagain.entity.StockValue;
import mg.yoan.igagain.entity.Unit;
import mg.yoan.igagain.exception.ResourceNotFoundException;
import mg.yoan.igagain.repository.IngredientRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    public List<Ingredient> findAllIngredients() {
        return ingredientRepository.findAllIngredients();
    }

    public Ingredient findIngredientById(Integer id) {
        try {
            return ingredientRepository.findIngredientById(id);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Ingredient not found")) {
                throw new ResourceNotFoundException("Ingredient.id=" + id + " is not found");
            }
            throw e;
        }
    }

    public StockValue getIngredientStock(Integer id, Instant temporal, Unit unit) {
        Ingredient ingredient = findIngredientById(id);
        StockValue stockValue = ingredient.getStockValueAt(temporal);
        return stockValue;
    }

    public Ingredient saveIngredient(Ingredient toSave) {
        return ingredientRepository.saveIngredient(toSave);
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        return ingredientRepository.createIngredients(newIngredients);
    }
}
