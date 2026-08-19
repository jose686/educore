package com.educore.platform.store.service;

import com.educore.platform.store.dto.CartItem;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para StripeService.
 */
class StripeServiceTest {

    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        stripeService = new StripeService();
        ReflectionTestUtils.setField(stripeService, "secretKey", "sk_test_mock");
        stripeService.init();
    }

    @Test
    void crearSesionPago_ShouldReturnCreatedSession() throws Exception {
        Session mockSession = mock(Session.class);

        try (MockedStatic<Session> mockedSessionClass = mockStatic(Session.class)) {
            mockedSessionClass.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(mockSession);

            Session result = stripeService.crearSesionPago(
                    "Curso Ajedrez",
                    new BigDecimal("19.99"),
                    "student@educore.com",
                    "curso",
                    "100",
                    2L,
                    100L,
                    "http://success.com",
                    "http://cancel.com"
            );

            assertNotNull(result);
            assertEquals(mockSession, result);
            mockedSessionClass.verify(() -> Session.create(any(SessionCreateParams.class)), times(1));
        }
    }

    @Test
    void crearSesionPagoCarrito_ShouldReturnCreatedSession() throws Exception {
        Session mockSession = mock(Session.class);
        CartItem item1 = CartItem.builder().id("100").titulo("Curso").tipo("curso").precio(new BigDecimal("10.00")).build();
        CartItem item2 = CartItem.builder().id("200").titulo("Pack").tipo("paquete").precio(new BigDecimal("20.00")).build();

        try (MockedStatic<Session> mockedSessionClass = mockStatic(Session.class)) {
            mockedSessionClass.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(mockSession);

            Session result = stripeService.crearSesionPagoCarrito(
                    List.of(item1, item2),
                    10, // 10% discount
                    "student@educore.com",
                    2L,
                    100L,
                    "100,200",
                    "http://success.com",
                    "http://cancel.com"
            );

            assertNotNull(result);
            assertEquals(mockSession, result);
            mockedSessionClass.verify(() -> Session.create(any(SessionCreateParams.class)), times(1));
        }
    }

    @Test
    void crearReembolso_ShouldReturnCreatedRefund() throws Exception {
        Refund mockRefund = mock(Refund.class);

        try (MockedStatic<Refund> mockedRefundClass = mockStatic(Refund.class)) {
            mockedRefundClass.when(() -> Refund.create(any(RefundCreateParams.class))).thenReturn(mockRefund);

            Refund result = stripeService.crearReembolso("pi_123");

            assertNotNull(result);
            assertEquals(mockRefund, result);
            mockedRefundClass.verify(() -> Refund.create(any(RefundCreateParams.class)), times(1));
        }
    }
}
