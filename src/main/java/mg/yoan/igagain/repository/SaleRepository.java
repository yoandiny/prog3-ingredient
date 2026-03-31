package mg.yoan.igagain.repository;

import mg.yoan.igagain.entity.Order;
import mg.yoan.igagain.entity.PaymentStatusEnum;
import mg.yoan.igagain.entity.Sale;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;

@Repository
public class SaleRepository {

    private final DataSource dataSource;
    private final SequenceHelper sequenceHelper;

    public SaleRepository(DataSource dataSource, SequenceHelper sequenceHelper) {
        this.dataSource = dataSource;
        this.sequenceHelper = sequenceHelper;
    }

    public Sale createSaleFrom(Order order) {
        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException("Order (avec id) est obligatoire.");
        }

        if (order.getPaymentStatus() != PaymentStatusEnum.PAID) {
            throw new IllegalStateException("Une vente ne peut être créée que pour une commande payée (PAID).");
        }

        String checkAlreadySoldSql = "SELECT 1 FROM sale WHERE id_order = ? LIMIT 1";
        String insertSaleSql = "INSERT INTO sale (id, creation_datetime, id_order) VALUES (?, ?, ?) RETURNING id";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(checkAlreadySoldSql)) {
                    ps.setInt(1, order.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalStateException("Cette commande est déjà associée à une vente existante.");
                        }
                    }
                }

                int saleId;
                Instant now = Instant.now();
                try (PreparedStatement ps = conn.prepareStatement(insertSaleSql)) {
                    ps.setInt(1, sequenceHelper.getNextSerialValue(conn, "sale", "id"));
                    ps.setTimestamp(2, Timestamp.from(now));
                    ps.setInt(3, order.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            saleId = rs.getInt(1);
                        } else {
                            throw new SQLException("Failed to get generated sale ID.");
                        }
                    }
                }
                conn.commit();
                return new Sale(saleId, now, order);
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
