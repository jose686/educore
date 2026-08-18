package com.educore.platform.store.controller;

import com.educore.platform.store.event.CursoCompradoEvent;
import com.educore.platform.store.service.PromocionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración para StripeWebhookController.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StripeWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromocionService promocionService;

    @Autowired
    private TestEventListener testEventListener;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestEventListener testEventListener() {
            return new TestEventListener();
        }
    }

    static class TestEventListener {
        private CursoCompradoEvent lastEvent;

        @EventListener
        public void onCursoComprado(CursoCompradoEvent event) {
            this.lastEvent = event;
        }

        public CursoCompradoEvent getLastEvent() {
            return lastEvent;
        }

        public void clear() {
            this.lastEvent = null;
        }
    }

    @BeforeEach
    void setUp() {
        testEventListener.clear();
    }

    @Test
    void webhookShouldReceiveSessionCompletedAndPublishEvent() throws Exception {
        String payload = "{\n" +
                "  \"id\": \"evt_123\",\n" +
                "  \"object\": \"event\",\n" +
                "  \"type\": \"checkout.session.completed\",\n" +
                "  \"data\": {\n" +
                "    \"object\": {\n" +
                "      \"id\": \"cs_test_123\",\n" +
                "      \"object\": \"checkout.session\",\n" +
                "      \"customer_email\": \"alumno@educore.com\",\n" +
                "      \"metadata\": {\n" +
                "        \"email\": \"alumno@educore.com\",\n" +
                "        \"tipo_producto\": \"curso\",\n" +
                "        \"producto_id\": \"12\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        mockMvc.perform(post("/api/v1/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        CursoCompradoEvent event = testEventListener.getLastEvent();
        assertNotNull(event, "El evento CursoCompradoEvent debería haberse publicado");
        assertEquals("alumno@educore.com", event.emailUsuario());
        assertEquals(12L, event.lmsCursoId());
    }

    @Test
    void webhookShouldReceiveSessionCompletedForPackageAndTriggerPurchase() throws Exception {
        String payload = "{\n" +
                "  \"id\": \"evt_124\",\n" +
                "  \"object\": \"event\",\n" +
                "  \"type\": \"checkout.session.completed\",\n" +
                "  \"data\": {\n" +
                "    \"object\": {\n" +
                "      \"id\": \"cs_test_124\",\n" +
                "      \"object\": \"checkout.session\",\n" +
                "      \"customer_email\": \"alumno@educore.com\",\n" +
                "      \"metadata\": {\n" +
                "        \"email\": \"alumno@educore.com\",\n" +
                "        \"tipo_producto\": \"paquete\",\n" +
                "        \"producto_id\": \"5\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        mockMvc.perform(post("/api/v1/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(promocionService).comprarPaquete(eq(5L), eq("alumno@educore.com"));
    }

    @Test
    void webhookShouldReceiveSessionCompletedForCartAndActivateAllItems() throws Exception {
        String payload = "{\n" +
                "  \"id\": \"evt_125\",\n" +
                "  \"object\": \"event\",\n" +
                "  \"type\": \"checkout.session.completed\",\n" +
                "  \"data\": {\n" +
                "    \"object\": {\n" +
                "      \"id\": \"cs_test_125\",\n" +
                "      \"object\": \"checkout.session\",\n" +
                "      \"customer_email\": \"alumno@educore.com\",\n" +
                "      \"metadata\": {\n" +
                "        \"email\": \"alumno@educore.com\",\n" +
                "        \"tipo_producto\": \"carrito\",\n" +
                "        \"producto_id\": \"curso_12,paquete_5,servicio_retos_pro\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        mockMvc.perform(post("/api/v1/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        // Verificar curso activado
        CursoCompradoEvent event = testEventListener.getLastEvent();
        assertNotNull(event);
        assertEquals("alumno@educore.com", event.emailUsuario());
        assertEquals(12L, event.lmsCursoId());

        // Verificar paquete activado
        verify(promocionService).comprarPaquete(eq(5L), eq("alumno@educore.com"));
    }
}
