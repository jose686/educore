# EduCore Platform

EduCore es una plataforma educativa modular y robusta basada en **Spring Boot 3** y **Angular**. La solución está diseñada bajo una arquitectura orientada a dominios (**Package-by-Feature**) e integra un módulo de aula virtual (LMS), una tienda en línea con pasarela de pagos Stripe, un blog de contenido y una biblioteca de gestión multimedia avanzada.

---

## 🚀 Módulos Clave del Proyecto

1. **Aula Virtual (LMS):** Gestión de cursos, lecciones interactivas, temarios ordenados correlativamente, inscripciones temporales o permanentes y control estricto de acceso estudiantil.
2. **Tienda y Pasarela de Pagos:** Integración con Stripe Checkout (pagos individuales y carrito de compras) y procesamiento de eventos idempotentes mediante Stripe Webhooks (compras y reembolsos automáticos).
3. **Biblioteca Multimedia:** Almacenamiento físico de recursos, transcodificación asíncrona de videos MP4 a formato de streaming adaptativo **HLS (m3u8/TS)** mediante FFmpeg, y resolución dinámica de URLs por alias.
4. **Blog y SEO:** Motor de publicación de artículos de blog con validación automatizada de longitud límite de resumen corto SEO (máximo 300 caracteres).
5. **Panel de Administración:** Control total sobre las entidades del sistema restringido por roles (`ADMIN`, `WORKER`, `CLIENT`).

---

## 🛠️ Stack Tecnológico

* **Lenguaje:** Java 21 (Zulú OpenJDK)
* **Framework Principal:** Spring Boot 3+ (Spring Web, Spring Security 6)
* **Persistencia:** Spring Data JPA / Hibernate (claves UUID)
* **Base de Datos:** MySQL (Producción/Desarrollo) / H2 (Pruebas unitarias)
* **Seguridad:** API Stateless protegida por Tokens JWT (clientes) y Autenticación con sesión web clásica (administración)
* **Motor de Plantillas:** Thymeleaf (para el frontend administrativo)
* **Infraestructura de Pruebas:** JUnit 5, Mockito
* **Auditoría de Calidad:** JaCoCo (Jacoco Maven Plugin 0.8.12)

---

## ⚙️ Requisitos y Arranque Local

### Requisitos Previos
* **Java 21** instalado y configurado en el `PATH` (o gestionado por SDKMAN).
* **Maven** (usar el wrapper `./mvnw` incluido).
* **FFmpeg** instalado en el sistema (necesario para la conversión de video a HLS).

### Configuración del Entorno
Crea un archivo `.env` en la raíz del proyecto o exporta las siguientes variables:
```bash
STRIPE_API_SECRET_KEY=sk_test_tu_clave_secreta
FILE_UPLOAD_DIR=./uploads
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/educore
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=tu_password
```

### Ejecutar la Aplicación
```bash
./mvnw spring-boot:run
```

---

## 🧪 Calidad de Código y Pruebas

La plataforma cuenta con una suite completa de pruebas unitarias y de integración que auditan de forma automatizada los controladores, servicios, flujos de seguridad y reglas de negocio del sistema.

### Ejecución de Pruebas
Para ejecutar la suite de pruebas completa en verde:
```bash
# Con JDK configurado
./mvnw clean test
```

### Reporte de Cobertura JaCoCo
Una vez completadas las pruebas, se genera automáticamente un informe de cobertura en formato HTML. Puedes abrirlo en tu navegador favorito en la siguiente ruta:
```bash
# Ruta al reporte visual
target/site/jacoco/index.html
```

### Métricas de Cobertura Actuales
A continuación se detallan los porcentajes de cobertura por líneas obtenidos mediante auditoría:

| Paquete / Componente | Cobertura de Líneas | Estado |
| :--- | :---: | :---: |
| **Global del Sistema** | **77.69%** | 🟢 Superado (>75%) |
| `com.educore.platform.lms.service` | **96.48%** | 🟢 Superado (>80%) |
| `com.educore.platform.store.service` | **89.68%** | 🟢 Superado (>80%) |
| `com.educore.platform.media.service` | **82.91%** | 🟢 Superado (>80%) |
| `com.educore.platform.admin.controller` | **79.68%** | 🟢 Superado (>75%) |
| `com.educore.platform.users.service` | **82.98%** | 🟢 Superado |

Para un desglose técnico detallado del modelo de pruebas y exclusiones del reporte, consulta el archivo [TESTING.md](file:///home/jose/Documentos/Programacion/java/app-curso/TESTING.md).
