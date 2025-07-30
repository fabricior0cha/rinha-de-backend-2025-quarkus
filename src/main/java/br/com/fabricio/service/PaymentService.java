package br.com.fabricio.service;

import br.com.fabricio.model.Payment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@ApplicationScoped
public class PaymentService {

    @Inject
    DataSource dataSource;

    public void addPayment(Payment payment) throws SQLException {
        String sql = "INSERT INTO tb_payments VALUES (?, ?, ?, ?)";
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ) {
            ps.setObject(1, UUID.fromString(payment.getCorrelationId()));
            ps.setDouble(2, payment.getAmount());
            ps.setObject(3, Timestamp.from(payment.getRequestedAt()));
            ps.setInt(4, payment.getProcessor());

            ps.executeLargeUpdate();
        }
    }

    public String getSummary(Instant to, Instant from) {
        StringBuilder sql = new StringBuilder("SELECT processor, amount FROM tb_payments WHERE 1=1");

        if (from != null) {
            sql.append(" AND requested_at >= ?");
        }

        if (to != null) {
            sql.append(" AND requested_at <= ?");
        }

        int totalDefault = 0;
        BigDecimal amountDefault = BigDecimal.ZERO;

        int totalFallback = 0;
        BigDecimal amountFallback = BigDecimal.ZERO;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;

            if (from != null) {
                ps.setTimestamp(paramIndex++, Timestamp.from(from));
            }

            if (to != null) {
                ps.setTimestamp(paramIndex++, Timestamp.from(to));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int processor = rs.getInt("processor");
                    BigDecimal amount = rs.getBigDecimal("amount");

                    if (processor == 1) {
                        totalDefault++;
                        amountDefault = amountDefault.add(amount);
                    } else if (processor == 2) {
                        totalFallback++;
                        amountFallback = amountFallback.add(amount);
                    }
                }
            }

        } catch (SQLException e) {
            return "{}";
        }

        return String.format("""
        {
          "default": {
            "totalRequests": %d,
            "totalAmount": %s
          },
          "fallback": {
            "totalRequests": %d,
            "totalAmount": %s
          }
        }
        """,
                totalDefault, amountDefault.toPlainString(),
                totalFallback, amountFallback.toPlainString()
        );
    }


}
