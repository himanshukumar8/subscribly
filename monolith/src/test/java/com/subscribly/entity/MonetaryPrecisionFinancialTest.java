package com.subscribly.entity;

import com.subscribly.gateway.RazorpayGatewayProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class MonetaryPrecisionFinancialTest {

    @Test
    @DisplayName("Monetary Precision: Preserves exact decimal fractions 0.10 and 49.99 without binary floating point drift")
    void testExactDecimalPreservation() {
        BigDecimal amountTenCents = new BigDecimal("0.10");
        BigDecimal amountFortyNine = new BigDecimal("49.99");

        Subscription sub = Subscription.builder()
                .price(amountFortyNine)
                .build();
        Payment pay = Payment.builder()
                .amount(amountTenCents)
                .build();

        assertEquals(0, sub.getPrice().compareTo(new BigDecimal("49.99")));
        assertEquals(0, pay.getAmount().compareTo(new BigDecimal("0.10")));

        // Demonstrate binary floating point would drift if Double were used
        double floatTenCents = 0.10;
        double floatFortyNine = 49.99;
        assertNotEquals("0.1", Double.toString(floatTenCents * 3 - 0.2)); // 0.10000000000000003
        assertEquals(new BigDecimal("0.10"), amountTenCents.multiply(new BigDecimal("3")).subtract(new BigDecimal("0.20")));
    }

    @Test
    @DisplayName("Rounding Boundaries: Half-up rounding at 2 decimal places")
    void testRoundingBoundaries() {
        BigDecimal roundDown = new BigDecimal("49.994").setScale(2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("49.99"), roundDown);

        BigDecimal roundUp = new BigDecimal("49.995").setScale(2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("50.00"), roundUp);

        BigDecimal boundaryNine = new BigDecimal("0.005").setScale(2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("0.01"), boundaryNine);

        BigDecimal boundaryFour = new BigDecimal("0.004").setScale(2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("0.00"), boundaryFour);
    }

    @Test
    @DisplayName("Minor Unit Conversion: Converts exact major units to integer paise without truncation")
    void testMinorUnitConversion() {
        BigDecimal priceStandard = new BigDecimal("49.99");
        long minorUnitsStandard = priceStandard.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        assertEquals(4999L, minorUnitsStandard);

        BigDecimal priceSmall = new BigDecimal("0.10");
        long minorUnitsSmall = priceSmall.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        assertEquals(10L, minorUnitsSmall);

        BigDecimal priceSingleCent = new BigDecimal("0.01");
        long minorUnitsCent = priceSingleCent.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        assertEquals(1L, minorUnitsCent);

        // Maximum supported by NUMERIC(12,2): 9,999,999,999.99
        BigDecimal maxSupported = new BigDecimal("9999999999.99");
        long maxMinorUnits = maxSupported.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        assertEquals(999999999999L, maxMinorUnits);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "-1.00", "-49.99"})
    @DisplayName("Validation: Negative monetary values are rejected by gateway order creation")
    void testNegativeMonetaryRejection(String negativeStr) {
        BigDecimal negativeAmount = new BigDecimal(negativeStr);
        RazorpayGatewayProvider provider = new RazorpayGatewayProvider("key", "secret", "webhookSec");

        assertThrows(IllegalArgumentException.class, () ->
                provider.createOrder("REF-NEG", negativeAmount, "INR")
        );
    }

    @Test
    @DisplayName("Validation: Zero amount rejected for payment order")
    void testZeroAmountRejection() {
        RazorpayGatewayProvider provider = new RazorpayGatewayProvider("key", "secret", "webhookSec");

        assertThrows(IllegalArgumentException.class, () ->
                provider.createOrder("REF-ZERO", BigDecimal.ZERO, "INR")
        );
    }
}
