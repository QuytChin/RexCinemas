package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.LoyaltySettings;
import com.rexchain.cinema.entity.MembershipTier;
import com.rexchain.cinema.entity.MembershipTierPolicy;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.LoyaltyPolicyAuditRepository;
import com.rexchain.cinema.repository.LoyaltySettingsRepository;
import com.rexchain.cinema.repository.MembershipTierPolicyRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MembershipPolicyMySqlIntegrationTest {

    @Test
    void temporarySilverThresholdChangeFlowsThroughMembershipAndCheckoutThenRollsBack() throws Exception {
        String url = System.getenv("DB_URL");
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        assumeTrue("true".equalsIgnoreCase(System.getenv("REX_MYSQL_MEMBERSHIP_IT"))
                        && url != null && username != null && password != null,
                "Set REX_MYSQL_MEMBERSHIP_IT only after a verified DEV backup is complete");
        assumeTrue(url.matches("jdbc:mysql://(?:localhost|127\\.0\\.0\\.1)(?::\\d+)?/rex_chain_cinema(?:\\?.*)?"),
                "Integration test only permits the local rex_chain_cinema development schema");

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            try {
                assertThat(scalarString(connection, "SELECT DATABASE()")).isEqualTo("rex_chain_cinema");
                List<MembershipTierPolicy> beforePolicies = readPolicies(connection);
                assertThat(beforePolicies).hasSize(MembershipTier.values().length);
                MembershipTierPolicy silver = beforePolicies.stream()
                        .filter(policy -> policy.getTier() == MembershipTier.SILVER).findFirst().orElseThrow();
                assertThat(silver.getQualifyingThreshold()).isEqualTo(250);
                int originalThreshold = silver.getQualifyingThreshold();
                long userCountBefore = scalarLong(connection, "SELECT COUNT(*) FROM app_users");
                long bookingCountBefore = scalarLong(connection, "SELECT COUNT(*) FROM bookings");
                long loyaltyCountBefore = scalarLong(connection, "SELECT COUNT(*) FROM loyalty_ledger");

                try (Statement statement = connection.createStatement()) {
                    assertThat(statement.executeUpdate("UPDATE membership_tier_policies SET qualifying_threshold=400 WHERE tier='SILVER'"))
                            .isEqualTo(1);
                }

                MembershipTierPolicyRepository policyRepository = mock(MembershipTierPolicyRepository.class);
                when(policyRepository.findAllByOrderByQualifyingThresholdAsc()).thenAnswer(invocation -> readPolicies(connection));
                LoyaltySettingsRepository settingsRepository = mock(LoyaltySettingsRepository.class);
                when(settingsRepository.findById(1L)).thenAnswer(invocation -> Optional.of(readSettings(connection)));
                MembershipService service = new MembershipService(settingsRepository, policyRepository,
                        mock(LoyaltyPolicyAuditRepository.class));

                assertThat(service.statusFor(399).tier()).isEqualTo(MembershipTier.MEMBER);
                assertThat(service.statusFor(399).nextThreshold()).isEqualTo(400);
                assertThat(service.statusFor(400).tier()).isEqualTo(MembershipTier.SILVER);
                assertThat(service.statusFor(400).nextThreshold()).isEqualTo(600);

                User checkoutUser = new User();
                checkoutUser.setTierQualifyingPoints(400);
                var quote = service.quote(checkoutUser, new BigDecimal("100000"), BigDecimal.ZERO);
                assertThat(quote.tier()).isEqualTo(MembershipTier.SILVER);
                assertThat(quote.ticketDiscountPercent()).isEqualByComparingTo(silver.getTicketDiscountPercent());
                assertThat(quote.pointMultiplier()).isEqualByComparingTo(silver.getPointMultiplier());

                assertThat(scalarLong(connection, "SELECT COUNT(*) FROM app_users")).isEqualTo(userCountBefore);
                assertThat(scalarLong(connection, "SELECT COUNT(*) FROM bookings")).isEqualTo(bookingCountBefore);
                assertThat(scalarLong(connection, "SELECT COUNT(*) FROM loyalty_ledger")).isEqualTo(loyaltyCountBefore);
                assertThat(readPolicies(connection).stream().filter(policy -> policy.getTier() == MembershipTier.SILVER)
                        .findFirst().orElseThrow().getQualifyingThreshold()).isEqualTo(400);
            } finally {
                connection.rollback();
            }
            assertThat(readPolicies(connection).stream().filter(policy -> policy.getTier() == MembershipTier.SILVER)
                    .findFirst().orElseThrow().getQualifyingThreshold()).isEqualTo(250);
        }
    }

    private static List<MembershipTierPolicy> readPolicies(Connection connection) throws Exception {
        List<MembershipTierPolicy> values = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT tier, qualifying_threshold, ticket_discount_percent, point_multiplier, active FROM membership_tier_policies ORDER BY qualifying_threshold")) {
            while (rs.next()) {
                MembershipTierPolicy policy = new MembershipTierPolicy();
                policy.setTier(MembershipTier.valueOf(rs.getString("tier")));
                policy.setQualifyingThreshold(rs.getInt("qualifying_threshold"));
                policy.setTicketDiscountPercent(rs.getBigDecimal("ticket_discount_percent"));
                policy.setPointMultiplier(rs.getBigDecimal("point_multiplier"));
                policy.setActive(rs.getBoolean("active"));
                values.add(policy);
            }
        }
        return values;
    }

    private static LoyaltySettings readSettings(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT amount_per_point, minimum_positive_order_points, voucher_stacking_allowed, redemption_enabled FROM loyalty_settings WHERE id=1")) {
            if (!rs.next()) throw new IllegalStateException("Missing Rex Rewards settings row");
            LoyaltySettings settings = new LoyaltySettings();
            settings.setId(1L);
            settings.setAmountPerPoint(rs.getBigDecimal("amount_per_point"));
            settings.setMinimumPositiveOrderPoints(rs.getInt("minimum_positive_order_points"));
            settings.setVoucherStackingAllowed(rs.getBoolean("voucher_stacking_allowed"));
            settings.setRedemptionEnabled(rs.getBoolean("redemption_enabled"));
            return settings;
        }
    }

    private static long scalarLong(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static String scalarString(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}
