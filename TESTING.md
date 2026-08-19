# Informe Técnico de Pruebas y Cobertura (TESTING.md)

Este documento detalla la arquitectura de pruebas, los resultados de la auditoría de cobertura con JaCoCo y las directrices técnicas para mantener la calidad del software en la plataforma **EduCore**.

---

## 📈 Resumen Ejecutivo

* **Estado de la Suite:** 🟢 **229 tests ejecutados y aprobados con éxito** (0 fallos, 0 errores).
* **Cobertura de Líneas Global:** **77.69%** (superando la meta mínima del 75%).
* **Cobertura de Líneas por Módulos Críticos:**
  * Dominios de servicio LMS, Store y Media: **>80%** de cobertura en cada paquete.
  * Controladores Administrativos: **>79.6%** de cobertura.

---

## ⚙️ Arquitectura de Pruebas y Desglose por Módulos

### 1. Servicios de LMS (`com.educore.platform.lms.service`) — **96.48%**
* **Objetivo:** Auditar la lógica del aula virtual y los accesos de alumnos a cursos.
* **Componentes clave evaluados:**
  * `LmsServiceImplTest`: Valida la correlatividad en el orden de inserción de módulos y lecciones (fórmula `maxOrden + 1`), el retorno exclusivo de cursos activos y la expiración de inscripciones temporales.
  * `AccesoServiceImplTest`: Verifica la lógica de compra de paquetes de cursos e inscripciones múltiples controlando que no se dupliquen accesos preexistentes.
  * `AulaVirtualServiceImplTest`: Comprueba el control de matriculación manual y la posterior promoción automática del usuario de rol `VISITOR` a `STUDENT`.

### 2. Servicios de Tienda (`com.educore.platform.store.service`) — **89.68%**
* **Objetivo:** Probar el motor de facturación, cupones y la comunicación asíncrona de Stripe.
* **Componentes clave evaluados:**
  * `PedidoServiceImplTest`: Evalúa la creación del pedido mediante webhook (idempotencia ante Stripe Session IDs repetidos), la descomposición de la metadata del carrito de compras (ítems de curso, paquete o servicios) y el flujo de reembolso completo (llamada a Stripe y revocación en cascada de los accesos LMS).
  * `StripeServiceTest`: Emplea **Mockito estático** (`MockedStatic<Session>` y `MockedStatic<Refund>`) para simular las llamadas HTTP nativas de Stripe, validando la construcción de metadatos del cliente sin realizar tráfico de red real.
  * `PromocionServiceImplTest`: Controla las validaciones de validez de cupones de descuento, de acceso temporal y el canje seguro de tokens para invitados de cursos.

### 3. Servicios de Media (`com.educore.platform.media.service`) — **82.91%**
* **Objetivo:** Comprobar la resiliencia en la manipulación y almacenamiento físico de archivos multimedia.
* **Componentes clave evaluados:**
  * `MediaServiceImplTest`: Pruebas aisladas en directorios temporales que validan la subida de imágenes, PDFs y ficheros HTML interactivos (clasificación por categorías y tipos de medio). Audita la protección contra ataques de Path Traversal (`../`) y la sincronización recursiva de la base de datos con el sistema de archivos físico.
  * `VideoConversionServiceTest`: Valida la invocación asíncrona del comando de transcodificación FFmpeg (MP4 a HLS) asegurando que el archivo original se elimine en el bloque `finally` incluso si la conversión falla.

### 4. Controladores Administrativos (`com.educore.platform.admin.controller`) — **79.68%**
* **Objetivo:** Comprobar la seguridad web de Spring Security y el binding de datos.
* **Componentes clave evaluados:**
  * `AdminControllerTest`: Valida la interceptación de peticiones anónimas (redirección a login) y de usuarios con rol insuficiente (HTTP 403 Forbidden para estudiantes). Verifica el truncamiento seguro en servidor de resúmenes cortos de blog superiores a 300 caracteres (reglas de SEO).
  * `AdminBibliotecaControllerTest`: Cobertura del 100% de la gestión de la biblioteca de minijuegos y retos interactivos.

---

## 🚫 Exclusiones de JaCoCo

Para asegurar que las métricas de cobertura reflejen de manera precisa la lógica de negocio, se han excluido componentes puramente declarativos o auto-generados. Esta configuración está definida en el archivo [pom.xml](file:///home/jose/Documentos/Programacion/java/app-curso/pom.xml):

* **DTOs y Request Payload:** Paquetes `com.educore.platform.*.dto.*` (Lombok genera sus getters/setters).
* **Entidades JPA y Mapeos HBM:** Paquetes `com.educore.platform.*.model.*` (estructuras de datos planas).
* **Configuraciones de Sistema:** Paquetes `com.educore.platform.*.config.*` (definiciones estáticas de Beans y seguridad).
* **Clase Main:** `com.educore.platform.PlatformApplication` (punto de entrada).

---

## ✍️ Guía para Añadir Nuevas Pruebas

Para mantener la cobertura global por encima de la meta del **75%**, sigue este estándar al desarrollar nuevas características:

### Pruebas Unitarias de Servicios (`*ServiceImplTest.java`)
* Usa JUnit 5 con la extensión de Mockito:
```java
@ExtendWith(MockitoExtension.class)
class MiServicioImplTest {
    @Mock private MiRepository repository;
    @InjectMocks private MiServiceImpl service;
    
    // ...
}
```
* **Requisito:** Cubrir tanto caminos exitosos (happy path) como lanzamientos de excepciones de negocio.

### Pruebas de Integración de Controladores (`*ControllerTest.java`)
* Usa la configuración ligera de MockMvc y seguridad mockeada:
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MiControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private MiService miService;
    
    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void admin_ShouldPerformAction() throws Exception {
        mockMvc.perform(post("/admin/ruta")
                .param("parametro", "valor")
                .with(csrf())) // Obligatorio para POST protegidos
                .andExpect(status().is3xxRedirection());
    }
}
```
