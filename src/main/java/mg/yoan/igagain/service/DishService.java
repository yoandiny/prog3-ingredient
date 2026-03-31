package mg.yoan.igagain.service;

import mg.yoan.igagain.entity.Dish;
import mg.yoan.igagain.entity.DishIngredient;
import mg.yoan.igagain.entity.Ingredient;
import mg.yoan.igagain.exception.ResourceNotFoundException;
import mg.yoan.igagain.repository.DishRepository;
import mg.yoan.igagain.repository.IngredientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishService {

    private final DishRepository dishRepository;
    private final IngredientRepository ingredientRepository;

    public DishService(DishRepository dishRepository, IngredientRepository ingredientRepository) {
        this.dishRepository = dishRepository;
        this.ingredientRepository = ingredientRepository;
    }

    public List<Dish> findAllDishes() {
        return dishRepository.findAllDishes();
    }

    public Dish findDishById(Integer id) {
        try {
            return dishRepository.findDishById(id);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Dish not found")) {
                throw new ResourceNotFoundException("Dish.id=" + id + " is not found");
            }
            throw e;
        }
    }

    public Dish updateDishIngredients(Integer dishId, List<DishIngredient> newIngredients) {
        Dish dish = findDishById(dishId);
        
        // On récupère tous les ID d'ingrédients de la BDD pour ignorer ceux qui n'existent pas
        List<Integer> existingIngredientIds = ingredientRepository.findAllIngredients().stream()
                .map(Ingredient::getId)
                .toList();

        // On filtre la liste envoyée pour ne garder que ceux qui existent
        List<DishIngredient> validIngredients = newIngredients.stream()
                .filter(di -> di.getIngredient() != null 
                           && di.getIngredient().getId() != null 
                           && existingIngredientIds.contains(di.getIngredient().getId()))
                .peek(di -> di.setDish(dish)) // Associer l'ingrédient au plat pour le repo
                .collect(Collectors.toList());

        dish.setDishIngredients(validIngredients);
        return dishRepository.saveDish(dish);
    }
}
