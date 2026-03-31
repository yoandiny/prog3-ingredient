package mg.yoan.igagain.entity;

import jdk.jshell.spi.ExecutionControl;

import java.sql.*;
import java.time.Instant;

public class Sale {
    private int id;
    private Instant creationDateTime;
    private  Order order;

    public Sale(int id, Instant creationDateTime, Order order) {
        this.id = id;
        this.creationDateTime = creationDateTime;
        this.order = order;
    }

    public int getId() {
        return id;
    }

    public Instant getCreationDateTime() {
        return creationDateTime;
    }

    public Order getOrder() {
        return order;
    }

    private void updateSequenceNextValue(Connection conn, String tableName, String columnName, String sequenceName) throws SQLException {
        String setValSql = String.format(
                "SELECT setval('%s', (SELECT COALESCE(MAX(%s), 0) FROM %s))",
                sequenceName, columnName, tableName
        );

        try (PreparedStatement ps = conn.prepareStatement(setValSql)) {
            ps.executeQuery();
        }
    }


    private String getSerialSequenceName(Connection conn, String tableName, String columnName)
            throws SQLException {

        String sql = "SELECT pg_get_serial_sequence(?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    private int getNextSerialValue(Connection conn, String tableName, String columnName)
            throws SQLException {

        String sequenceName = getSerialSequenceName(conn, tableName, columnName);
        if (sequenceName == null) {
            throw new IllegalArgumentException(
                    "Any sequence found for " + tableName + "." + columnName
            );
        }
        updateSequenceNextValue(conn, tableName, columnName, sequenceName);

        String nextValSql = "SELECT nextval(?)";

        try (PreparedStatement ps = conn.prepareStatement(nextValSql)) {
            ps.setString(1, sequenceName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }



    public Sale createSaleFrom(Order order) {
        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException("Order (avec id) est obligatoire.");
        }

        if (order.getPaymentStatus() != PaymentStatusEnum.PAID) {
            throw new IllegalStateException(
                    "Une vente ne peut être créée que pour une commande payée (PAID)."
            );
        }

        String checkAlreadySoldSql = """
            SELECT 1
            FROM sale
            WHERE id_order = ?
            LIMIT 1
        """;

        String insertSaleSql = """
            INSERT INTO sale (id, creation_datetime, id_order)
            VALUES (?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false);

            try {
                try (PreparedStatement ps = conn.prepareStatement(checkAlreadySoldSql)) {
                    ps.setInt(1, order.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalStateException(
                                    "Cette commande est déjà associée à une vente existante."
                            );
                        }
                    }
                }

                int saleId;
                Instant now = Instant.now();

                try (PreparedStatement ps = conn.prepareStatement(insertSaleSql)) {
                    ps.setInt(1, getNextSerialValue(conn, "sale", "id"));
                    ps.setTimestamp(2, Timestamp.from(now));
                    ps.setInt(3, order.getId());

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        saleId = rs.getInt(1);
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

