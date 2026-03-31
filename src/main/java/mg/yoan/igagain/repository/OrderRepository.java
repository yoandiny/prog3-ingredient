package mg.yoan.igagain.repository;

import mg.yoan.igagain.entity.Dish;
import mg.yoan.igagain.entity.DishOrder;
import mg.yoan.igagain.entity.Order;
import mg.yoan.igagain.entity.PaymentStatusEnum;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderRepository {

    private final DataSource dataSource;
    private final SequenceHelper sequenceHelper;
    private final DishRepository dishRepository;

    public OrderRepository(DataSource dataSource, SequenceHelper sequenceHelper, DishRepository dishRepository) {
        this.dataSource = dataSource;
        this.sequenceHelper = sequenceHelper;
        this.dishRepository = dishRepository;
    }

    public Order findOrderByReference(String reference) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
            SELECT id, reference, creation_datetime, payment_status
            FROM "order"
            WHERE reference = ?
        """);
            preparedStatement.setString(1, reference);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Order order = new Order();
                Integer idOrder = resultSet.getInt("id");
                order.setId(idOrder);
                order.setReference(resultSet.getString("reference"));
                order.setCreationDatetime(resultSet.getTimestamp("creation_datetime").toInstant());
                String paymentStatusStr = resultSet.getString("payment_status");
                if (paymentStatusStr != null) {
                    order.setPaymentStatus(PaymentStatusEnum.valueOf(paymentStatusStr));
                }
                order.setDishOrderList(findDishOrderByIdOrder(connection, idOrder));
                return order;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<DishOrder> findDishOrderByIdOrder(Connection connection, Integer idOrder) throws SQLException {
        List<DishOrder> dishOrders = new ArrayList<>();
        PreparedStatement preparedStatement = connection.prepareStatement(
                """
                        select id, id_dish, quantity from dish_order where dish_order.id_order = ?
                        """);
        preparedStatement.setInt(1, idOrder);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            Dish dish = dishRepository.findDishById(resultSet.getInt("id_dish"));
            DishOrder dishOrder = new DishOrder();
            dishOrder.setId(resultSet.getInt("id"));
            dishOrder.setQuantity(resultSet.getInt("quantity"));
            dishOrder.setDish(dish);
            dishOrders.add(dishOrder);
        }
        return dishOrders;
    }

    private void deleteDishOrders(Connection conn, int orderId) throws SQLException {
        String sql = "DELETE FROM dish_order WHERE id_order = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }

    private void insertDishOrders(Connection conn, Order order, int orderId) throws SQLException {
        String sql = """
        INSERT INTO dish_order (id, id_order, id_dish, quantity)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE
        SET id_dish = EXCLUDED.id_dish,
            quantity = EXCLUDED.quantity
    """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (order.getDishOrderList() == null) return;
            for (DishOrder d : order.getDishOrderList()) {
                int lineId = (d.getId() != null)
                        ? d.getId()
                        : sequenceHelper.getNextSerialValue(conn, "dish_order", "id");
                ps.setInt(1, lineId);
                ps.setInt(2, orderId);
                ps.setInt(3, d.getDish().getId());
                ps.setInt(4, d.getQuantity());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public Order saveOrder(Order toSave) {
        String upsertOrderSql = """
        INSERT INTO "order" (id, reference, creation_datetime, payment_status)
        VALUES (?, ?, ?, ?::payment_status)
        ON CONFLICT (id) DO UPDATE
        SET reference = EXCLUDED.reference,
            creation_datetime = EXCLUDED.creation_datetime,
            payment_status = EXCLUDED.payment_status
        RETURNING id
    """;

        String lockOrderSql = """
        SELECT payment_status, id_sale
        FROM "order"
        WHERE id = ?
        FOR UPDATE
    """;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                if (toSave.getCreationDatetime() == null) {
                    toSave.setCreationDatetime(java.time.Instant.now());
                }
                if (toSave.getPaymentStatus() == null) {
                    throw new IllegalArgumentException("paymentStatus ne peut pas être null");
                }
                if (toSave.getPaymentStatus() != PaymentStatusEnum.PAID) {
                    throw new IllegalStateException("Impossible d'enregistrer : la commande n'est pas encore payée (PAID).");
                }

                if (toSave.getId() != null) {
                    try (PreparedStatement ps = conn.prepareStatement(lockOrderSql)) {
                        ps.setInt(1, toSave.getId());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                Integer idSale = (Integer) rs.getObject("id_sale");
                                if (idSale != null) {
                                    throw new IllegalStateException("Impossible : la commande est déjà associée à une vente.");
                                }
                            }
                        }
                    }
                }

                int orderId;
                try (PreparedStatement ps = conn.prepareStatement(upsertOrderSql)) {
                    int id = (toSave.getId() != null)
                            ? toSave.getId()
                            : sequenceHelper.getNextSerialValue(conn, "\"order\"", "id");

                    ps.setInt(1, id);
                    ps.setString(2, toSave.getReference());
                    ps.setTimestamp(3, Timestamp.from(toSave.getCreationDatetime()));
                    ps.setString(4, toSave.getPaymentStatus().name());

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        orderId = rs.getInt(1);
                    }
                }

                deleteDishOrders(conn, orderId);
                insertDishOrders(conn, toSave, orderId);

                conn.commit();
                return findOrderByReference(toSave.getReference());
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Order updateOrder(Order toUpdate) {
        if (toUpdate.getId() == null) {
            throw new IllegalArgumentException("Order.id est obligatoire pour modifier une commande.");
        }

        String lockSql = """
        SELECT payment_status
        FROM "order"
        WHERE id = ?
        FOR UPDATE
    """;

        String updateOrderSql = """
        UPDATE "order"
        SET reference = ?, creation_datetime = ?, payment_status = ?::payment_status
        WHERE id = ?
    """;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                PaymentStatusEnum currentStatus;
                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setInt(1, toUpdate.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new RuntimeException("Commande introuvable: " + toUpdate.getId());
                        }
                        String s = rs.getString("payment_status");
                        currentStatus = (s == null) ? null : PaymentStatusEnum.valueOf(s);
                    }
                }

                if (currentStatus == PaymentStatusEnum.PAID) {
                    throw new IllegalStateException(
                            "La commande a déjà été payée et ne peut plus être modifiée."
                    );
                }

                try (PreparedStatement ps = conn.prepareStatement(updateOrderSql)) {
                    ps.setString(1, toUpdate.getReference());
                    ps.setTimestamp(2, Timestamp.from(toUpdate.getCreationDatetime()));
                    ps.setString(3, toUpdate.getPaymentStatus().name());
                    ps.setInt(4, toUpdate.getId());
                    ps.executeUpdate();
                }

                deleteDishOrders(conn, toUpdate.getId());
                insertDishOrders(conn, toUpdate, toUpdate.getId());

                conn.commit();
                return findOrderByReference(toUpdate.getReference());
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
