package com.educore.platform.users.controller;

import com.educore.platform.store.model.Pedido;
import com.educore.platform.store.model.DatosFiscales;
import com.educore.platform.store.repository.PedidoRepository;
import com.educore.platform.store.repository.DatosFiscalesRepository;
import com.educore.platform.users.model.TicketSoporte;
import com.educore.platform.users.repository.TicketSoporteRepository;
import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private PedidoRepository pedidoRepository;

    @MockBean
    private DatosFiscalesRepository datosFiscalesRepository;

    @MockBean
    private TicketSoporteRepository ticketSoporteRepository;

    @MockBean
    private com.educore.platform.store.service.PedidoService pedidoService;

    @Test
    void anonymousUser_ShouldRedirectToLogin_WhenAccessingProfile() throws Exception {
        mockMvc.perform(get("/perfil"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void authenticatedUser_ShouldSeeProfile() throws Exception {
        Usuario mockUsuario = Usuario.builder()
                .id(1L)
                .email("alumno@educore.com")
                .nombre("Alumno Prueba")
                .build();
        when(usuarioService.obtenerPorEmail("alumno@educore.com")).thenReturn(mockUsuario);

        mockMvc.perform(get("/perfil"))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil"))
                .andExpect(model().attributeExists("usuario"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void authenticatedUser_ShouldSeePurchasesList() throws Exception {
        when(pedidoRepository.findByUsuarioEmail("alumno@educore.com")).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/perfil/compras"))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil-compras"))
                .andExpect(model().attributeExists("pedidos"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void descargarFactura_ShouldReturnFacturaView_WhenUserIsBuyer() throws Exception {
        Pedido mockPedido = Pedido.builder()
                .id(123L)
                .emailUsuario("alumno@educore.com")
                .totalEuros(BigDecimal.valueOf(29.99))
                .detalles(new ArrayList<>())
                .build();

        when(pedidoRepository.findById(123L)).thenReturn(Optional.of(mockPedido));

        mockMvc.perform(get("/perfil/compras/123/factura"))
                .andExpect(status().isOk())
                .andExpect(view().name("factura"))
                .andExpect(model().attributeExists("pedido"));
    }

    @Test
    @WithMockUser(username = "otro@educore.com")
    void descargarFactura_ShouldReturnForbidden_WhenUserIsNotBuyer() throws Exception {
        Pedido mockPedido = Pedido.builder()
                .id(123L)
                .emailUsuario("alumno@educore.com")
                .totalEuros(BigDecimal.valueOf(29.99))
                .detalles(new ArrayList<>())
                .build();

        when(pedidoRepository.findById(123L)).thenReturn(Optional.of(mockPedido));

        mockMvc.perform(get("/perfil/compras/123/factura"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void mostrarAjustes_ShouldReturnAjustesView_WhenUserIsAdmin() throws Exception {
        when(datosFiscalesRepository.findAll()).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/admin/ajustes"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-ajustes"))
                .andExpect(model().attributeExists("config"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com", roles = "STUDENT")
    void mostrarAjustes_ShouldReturnForbidden_WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/admin/ajustes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void guardarAjustes_ShouldSaveAndRedirect_WhenUserIsAdmin() throws Exception {
        DatosFiscales mockConfig = DatosFiscales.builder()
                .id(1L)
                .razonSocial("Old Name")
                .build();
        when(datosFiscalesRepository.findAll()).thenReturn(java.util.List.of(mockConfig));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/ajustes")
                        .param("razonSocial", "New Name")
                        .param("cifNif", "B1234567")
                        .param("direccionFiscal", "Calle 1")
                        .param("emailContacto", "admin@educore.com")
                        .param("telefono", "123")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ajustes?exito=true"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void verSoporte_ShouldReturnSoporteView_WhenUserIsAdmin() throws Exception {
        when(ticketSoporteRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/admin/soporte"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-soporte"))
                .andExpect(model().attributeExists("tickets"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com", roles = "STUDENT")
    void verSoporte_ShouldReturnForbidden_WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/admin/soporte"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void resolverTicket_ShouldUpdateAndRedirect_WhenUserIsAdmin() throws Exception {
        TicketSoporte ticket = TicketSoporte.builder()
                .id(1L)
                .nombre("Usuario")
                .email("test@test.com")
                .motivo("Duda")
                .mensaje("Hola")
                .estado("PENDIENTE")
                .build();
        when(ticketSoporteRepository.findById(1L)).thenReturn(Optional.of(ticket));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/soporte/1/resolver")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/soporte"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void verDetallePedido_ShouldReturnDetailView_WhenUserIsAdmin() throws Exception {
        Pedido mockPedido = Pedido.builder()
                .id(123L)
                .emailUsuario("alumno@educore.com")
                .stripeSessionId("cs_123")
                .totalEuros(BigDecimal.valueOf(29.99))
                .detalles(new ArrayList<>())
                .build();
        when(pedidoService.obtenerPedidoPorId(123L)).thenReturn(mockPedido);

        mockMvc.perform(get("/admin/pedidos/123"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-pedido-detalle"))
                .andExpect(model().attributeExists("pedido"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void matricularManualPedido_ShouldPerformEnrollmentAndRedirect() throws Exception {
        when(ticketSoporteRepository.findByPedidoId(123L)).thenReturn(new ArrayList<>());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/pedidos/123/matricular-manual")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/pedidos/123"));
    }
}
