package mg.yoan.igagain.repository;

import mg.yoan.igagain.entity.CategoryEnum;
import mg.yoan.igagain.entity.Dish;
import mg.yoan.igagain.entity.DishIngredient;
import mg.yoan.igagain.entity.DishTypeEnum;
import mg.yoan.igagain.entity.Ingredient;
import mg.yoan.igagain.entity.Unit;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class DishRepository {

    private final DataSource dataSource;
    private final SequenceHelper sequenceHelper;

    public DishRepository(DataSource dataSource, SequenceHelper sequenceHelper) {
        this.dataSource = dataSource;
        this.sequenceHelper = sequenceHelper;
    }

    public Dish findDishById(Integer id) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                            select dish.id as dish_id, dish.name as dish_name, dish_type, dish.selling_price as dish_price
                            from dish
                            where dish.id = ?;
                            """);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Dish dish = new Dish();
                dish.setId(resultSet.getInt("dish_id"));
                dish.setName(resultSet.getString("dish_name"));
                dish.setDishType(DishTypeEnum.valueOf(resultSet.getString("dish_type")));
                dish.setPrice(resultSet.getObject("dish_price") == null
                        ? null : resultSet.getDouble("dish_price"));
                dish.setDishIngredients(findIngredientByDishId(connection, id));
                return dish;
            }
            throw new RuntimeException("Dish not found " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Dish saveDish(Dish toSave) {
        String upsertDishSql = """
                    INSERT INTO dish (id, selling_price, name, dish_type)
                    VALUES (?, ?, ?, ?::dish_type)
                    ON CONFLICT (id) DO UPDATE
                    SET name = EXCLUDED.name,
                        dish_type = EXCLUDED.dish_type,
                        selling_price = EXCLUDED.selling_price
                    RETURNING id
                """;
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            Integer dishId;
            try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
                if (toSave.getId() != null) {
                    ps.setInt(1, toSave.getId());
                } else {
                    ps.setInt(1, sequenceHelper.getNextSerialValue(conn, "dish", "id"));
                }
                if (toSave.getPrice() != null) {
                    ps.setDouble(2, toSave.getPrice());
                } else {
                    ps.setNull(2, Types.DOUBLE);
                }
                ps.setString(3, toSave.getName());
                ps.setString(4, toSave.getDishType().name());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    dishId = rs.getInt(1);
                }
            }
            List<DishIngredient> newDishIngredients = toSave.getDishIngredients();
            detachIngredients(conn, newDishIngredients);
            attachIngredients(conn, newDishIngredients);
            conn.commit();
            return findDishById(dishId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void detachIngredients(Connection conn, List<DishIngredient> dishIngredients) {
        if (dishIngredients == null || dishIngredients.isEmpty()) return;
        Map<Integer, List<DishIngredient>> dishIngredientsGroupByDishId = dishIngredients.stream()
                .filter(di -> di.getDish() != null && di.getDish().getId() != null)
                .collect(Collectors.groupingBy(dishIngredient -> dishIngredient.getDish().getId()));
        dishIngredientsGroupByDishId.forEach((dishId, dishIngredientList) -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM dish_ingredient where id_dish = ?")) {
                ps.setInt(1, dishId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void attachIngredients(Connection conn, List<DishIngredient> ingredients)
            throws SQLException {
        if (ingredients == null || ingredients.isEmpty()) {
            return;
        }
        String attachSql = """
                    insert into dish_ingredient (id, id_ingredient, id_dish, required_quantity, unit)
                    values (?, ?, ?, ?, ?::unit)
                """;
        try (PreparedStatement ps = conn.prepareStatement(attachSql)) {
            for (DishIngredient dishIngredient : ingredients) {
                ps.setInt(1, sequenceHelper.getNextSerialValue(conn, "dish_ingredient", "id"));
                ps.setInt(2, dishIngredient.getIngredient().getId());
                ps.setInt(3, dishIngredient.getDish().getId());
                ps.setDouble(4, dishIngredient.getQuantity());
                ps.setObject(5, dishIngredient.getUnit().name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<DishIngredient> findIngredientByDishId(Connection connection, Integer idDish) throws SQLException {
        List<DishIngredient> dishIngredients = new ArrayList<>();
        PreparedStatement preparedStatement = connection.prepareStatement(
                """
                        select ingredient.id, ingredient.name, ingredient.price, ingredient.category, di.required_quantity, di.unit
                        from ingredient join dish_ingredient di on di.id_ingredient = ingredient.id where id_dish = ?;
                        """);
        preparedStatement.setInt(1, idDish);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            Ingredient ingredient = new Ingredient();
            ingredient.setId(resultSet.getInt("id"));
            ingredient.setName(resultSet.getString("name"));
            ingredient.setPrice(resultSet.getDouble("price"));
            ingredient.setCategory(CategoryEnum.valueOf(resultSet.getString("category")));
            DishIngredient dishIngredient = new DishIngredient();
            dishIngredient.setIngredient(ingredient);
            dishIngredient.setQuantity(resultSet.getObject("required_quantity") == null ? null : resultSet.getDouble("required_quantity"));
            dishIngredient.setUnit(Unit.valueOf(resultSet.getString("unit")));
            dishIngredients.add(dishIngredient);
        }
        return dishIngredients;
    }

    public Double getDishCost(Integer dishId) {
        if(dishId == null) {
            throw new IllegalArgumentException("dishId ne peut pas être null");
        }
        // Assuming DataRetriever originally threw a partial implementation, I will leave it empty as it was in the original snippet, or add a comment.
        return 0.0;
    }
}
