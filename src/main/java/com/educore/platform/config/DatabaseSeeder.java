package com.educore.platform.config;
            
import com.educore.platform.blog.model.Articulo;
import com.educore.platform.blog.repository.ArticuloRepository;
import com.educore.platform.lms.model.Curso;
import com.educore.platform.lms.repository.CursoRepository;
import com.educore.platform.media.model.MediaFile;
import com.educore.platform.media.model.MediaType;
import com.educore.platform.media.repository.MediaFileRepository;
import com.educore.platform.media.service.MediaService;
import com.educore.platform.store.model.Cupon;
import com.educore.platform.store.model.TipoCupon;
import com.educore.platform.store.model.ProductoCurso;
import com.educore.platform.store.model.DatosFiscales;
import com.educore.platform.store.model.Paquete;
import com.educore.platform.store.model.GuestToken;
import com.educore.platform.store.repository.CuponRepository;
import com.educore.platform.store.repository.ProductoCursoRepository;
import com.educore.platform.store.repository.DatosFiscalesRepository;
import com.educore.platform.store.repository.PaqueteRepository;
import com.educore.platform.store.repository.GuestTokenRepository;
import com.educore.platform.users.model.Role;
import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.repository.UsuarioRepository;
import com.educore.platform.store.model.RecursoInteractivo;
import com.educore.platform.store.repository.RecursoInteractivoRepository;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;



/**
 * Seeder automático para inyectar datos de prueba en la base de datos H2 en el arranque.
 */


@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final ProductoCursoRepository productoCursoRepository;
    private final ArticuloRepository articuloRepository;
    private final PasswordEncoder passwordEncoder;
    private final MediaService mediaService;
    private final CuponRepository cuponRepository;
    private final DatosFiscalesRepository datosFiscalesRepository;
    private final RecursoInteractivoRepository recursoInteractivoRepository;
    private final MediaFileRepository mediaFileRepository;
    private final PaqueteRepository paqueteRepository;
    private final GuestTokenRepository guestTokenRepository;

    public DatabaseSeeder(UsuarioRepository usuarioRepository,
                          CursoRepository cursoRepository,
                          ProductoCursoRepository productoCursoRepository,
                          ArticuloRepository articuloRepository,
                          PasswordEncoder passwordEncoder,
                          MediaService mediaService,
                          CuponRepository cuponRepository,
                          DatosFiscalesRepository datosFiscalesRepository,
                          RecursoInteractivoRepository recursoInteractivoRepository,
                          MediaFileRepository mediaFileRepository,
                          PaqueteRepository paqueteRepository,
                          GuestTokenRepository guestTokenRepository) {
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.productoCursoRepository = productoCursoRepository;
        this.articuloRepository = articuloRepository;
        this.passwordEncoder = passwordEncoder;
        this.mediaService = mediaService;
        this.cuponRepository = cuponRepository;
        this.datosFiscalesRepository = datosFiscalesRepository;
        this.recursoInteractivoRepository = recursoInteractivoRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.paqueteRepository = paqueteRepository;
        this.guestTokenRepository = guestTokenRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // mediaService.syncDatabaseWithStorage();
        seedUsuarios();
        // seedLmsAndCatalog();
        // seedBlog();
        // seedDatosFiscales();
        // seedRecursosInteractivos();
    }

    private void seedDatosFiscales() {
        if (datosFiscalesRepository.count() == 0) {
            DatosFiscales df = DatosFiscales.builder()
            .razonSocial("EduCore Platform S.L.")
            .cifNif("B-88765432")
            .direccionFiscal("Calle de la Táctica, 42, 28001 Madrid, España")
            .emailContacto("soporte@educore.com")
            .telefono("+34 912 345 678")
            .build();
            datosFiscalesRepository.save(df);
            System.out.println("[DatabaseSeeder] Datos Fiscales globales de la empresa registrados.");
        }
    }

    private void seedUsuarios() {
        // Registrar Administrador
        if (usuarioRepository.findByEmail("admin@educore.com").isEmpty()) {
            Usuario admin = Usuario.builder()
                    .nombre("Administrador del Sitio")
                    .email("admin@educore.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.ADMIN)
                    .activo(true)
                    .build();
            usuarioRepository.save(admin);
            System.out.println("[DatabaseSeeder] Usuario ADMINISTRADOR creado: admin@educore.com");
        }

        // Registrar Alumno
        if (usuarioRepository.findByEmail("alumno@educore.com").isEmpty()) {
            Usuario alumno = Usuario.builder()
                    .nombre("Alumno de Prueba")
                    .email("alumno@educore.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.VISITOR)
                    .activo(true)
                    .saldoCreditos(100)
                    .build();
            usuarioRepository.save(alumno);
            System.out.println("[DatabaseSeeder] Usuario ESTUDIANTE (inicialmente VISITOR) creado: alumno@educore.com");
        }
    }

    private void seedLmsAndCatalog() {
        // Solo inyectar curso si no hay ninguno registrado
        if (cursoRepository.count() == 0) {
            // 1. Crear el curso en el LMS
            Curso curso = Curso.builder()
            .titulo("Curso de Ajedrez Avanzado")
            .descripcion("Aprende tácticas avanzadas, aperturas complejas y jaques mate en 1.")
            .precio(29.99)
            .teacherId(1L)
            .build();
            Curso cursoGuardado = cursoRepository.save(curso);
            System.out.println("[DatabaseSeeder] Curso LMS creado ID: " + cursoGuardado.getId());
            
            // Crear un segundo curso de prueba para bundles/tokens
            Curso curso2 = Curso.builder()
            .titulo("Curso de Ajedrez Básico")
            .descripcion("Principios elementales y reglas fundamentales de juego.")
            .precio(19.99)
            .teacherId(1L)
            .build();
            Curso curso2Guardado = cursoRepository.save(curso2);
            System.out.println("[DatabaseSeeder] Curso LMS 2 creado ID: " + curso2Guardado.getId());
            
            // 2. Crear la oferta correspondiente en la Tienda (E-Commerce)
            if (productoCursoRepository.count() == 0) {
                ProductoCurso producto = ProductoCurso.builder()
                .titulo("Curso de Ajedrez Avanzado")
                .descripcionCorta("Estudia táctica y aperturas de la mano de Maestros FIDE.")
                .precio(new BigDecimal("29.99"))
                .imagenPortadaUrl("/images/chess-course-cover.jpg")
                .lmsCursoId(cursoGuardado.getId())
                .estado("PUBLISHED")
                .build();
                productoCursoRepository.save(producto);
                
                ProductoCurso producto2 = ProductoCurso.builder()
                .titulo("Curso de Ajedrez Básico")
                .descripcionCorta("Descubre la magia del ajedrez desde cero.")
                .precio(new BigDecimal("19.99"))
                .imagenPortadaUrl("/images/chess-basic-cover.jpg")
                .lmsCursoId(curso2Guardado.getId())
                .estado("PUBLISHED")
                .build();
                productoCursoRepository.save(producto2);
            }
            
            // 3. Crear cupones de prueba
            if (cuponRepository.count() == 0) {
                Cupon promo15 = Cupon.builder()
                .codigo("PROMO15")
                .tipo(TipoCupon.DESCUENTO)
                .descuentoPorcentaje(15)
                .activo(true)
                .build();
                cuponRepository.save(promo15);
                
                Cupon accesoGratis = Cupon.builder()
                .codigo("ACCESOGRATIS")
                .tipo(TipoCupon.ACCESO_TEMPORAL)
                .diasAcceso(7)
                .cursoId(cursoGuardado.getId())
                .activo(true)
                .build();
                cuponRepository.save(accesoGratis);
                System.out.println("[DatabaseSeeder] Cupones de prueba registrados: PROMO15 y ACCESOGRATIS.");
            }
            
            // 4. Crear Paquete / Bundle de prueba
            if (paqueteRepository.count() == 0) {
                Paquete pack = Paquete.builder()
                .titulo("Megapack Ajedrez Completo")
                .descripcion("Acceso total de por vida a todos los cursos de ajedrez disponibles actualmente.")
                .precio(new BigDecimal("69.99"))
                .cursoIds(Set.of(cursoGuardado.getId(), curso2Guardado.getId()))
                .activo(true)
                .build();
                paqueteRepository.save(pack);
                System.out.println("[DatabaseSeeder] Paquete de prueba registrado.");
            }
            
            // 5. Crear GuestTokens de prueba
            if (guestTokenRepository.count() == 0) {
                GuestToken token = GuestToken.builder()
                .token("INV-EDUCORE2026")
                .cursoIds(Set.of(cursoGuardado.getId()))
                .diasAcceso(15)
                .activo(true)
                .build();
                guestTokenRepository.save(token);
                System.out.println("[DatabaseSeeder] GuestToken de prueba registrado.");
            }
        }
    }

    private void seedBlog() {
        // Solo inyectar artículo si el blog está vacío
        if (articuloRepository.count() == 0) {
            Articulo articulo = Articulo.builder()
            .titulo("Bienvenidos a la Plataforma EduCore")
            .slug("bienvenidos-a-educore")
            .resumenCorto("Conoce los fundamentos de nuestra academia monolítica modular y el sistema de juego táctico.")
            .contenido("<h2>El Futuro del Aprendizaje Interactivo</h2>" +
            "<p>En <strong>EduCore</strong>, hemos desarrollado un sistema integral de LMS y e-commerce " +
            "que combina explicaciones teóricas con simulaciones y minijuegos lúdicos en tiempo real.</p>" +
            "<h3>¿Cómo funciona?</h3>" +
            "<p>El alumno adquiere el curso en la tienda pública, se le inscribe de forma asíncrona mediante eventos, " +
            "y obtiene acceso al Aula Virtual para interactuar con tableros tácticos en Vanilla JS.</p>")
            .fechaPublicacion(LocalDateTime.now())
            .usuarioId(1L)
            .build();
            articuloRepository.save(articulo);
            System.out.println("[DatabaseSeeder] Artículo de blog de prueba registrado.");
        }
    }
                    
    private void seedRecursosInteractivos() {
        // Seed MediaFiles for Chess & TicTacToe first
        if (mediaFileRepository.findByAlias("game-ajedrez").isEmpty()) {
            MediaFile chessFile = MediaFile.builder()
            .filename("chess-puzzle.html")
            .url("/minijuegos/chess-puzzle.html")
            .uploadedAt(LocalDateTime.now())
            .tipo(MediaType.HTML_INTERACTIVO)
            .nombreOriginal("chess-puzzle.html")
            .alias("game-ajedrez")
            .categoriaMedia(com.educore.platform.media.model.CategoriaMedia.MINIJUEGO)
            .build();
            mediaFileRepository.save(chessFile);
        }
        if (mediaFileRepository.findByAlias("game-tictactoe").isEmpty()) {
            MediaFile tictactoeFile = MediaFile.builder()
            .filename("tictactoe.html")
            .url("/minijuegos/tictactoe.html")
            .uploadedAt(LocalDateTime.now())
            .tipo(MediaType.HTML_INTERACTIVO)
            .nombreOriginal("tictactoe.html")
            .alias("game-tictactoe")
            .categoriaMedia(com.educore.platform.media.model.CategoriaMedia.MINIJUEGO)
            .build();
            mediaFileRepository.save(tictactoeFile);
        }

        if (recursoInteractivoRepository.count() == 0) {
            RecursoInteractivo ajedrez = RecursoInteractivo.builder()
            .identificador("CHESS-PUZZLE-MATE1")
            .titulo("Ajedrez Reto Táctico")
            .descripcion("Mate en 1: juegan las blancas. Encuentra el único movimiento ganador en este puzzle diseñado por grandes maestros.")
            .imagenPortadaUrl("/images/chess-cover-mock.jpg")
            .htmlUrl("game-ajedrez")
            .etiquetas(Set.of("Lógica", "Ajedrez", "Arcade"))
            .esGratis(true)
            .costeCreditos(0)
            .activo(true)
            .build();
            recursoInteractivoRepository.save(ajedrez);
            
            RecursoInteractivo tictactoe = RecursoInteractivo.builder()
            .identificador("HTML5-TICTACTOE")
            .titulo("Tres en Raya Leyenda")
            .descripcion("El clásico e imbatible Tres en Raya interactivo. Reta a un amigo o entrena tu mente.")
            .imagenPortadaUrl(null)
            .htmlUrl("game-tictactoe")
            .etiquetas(Set.of("Arcade", "Estrategia"))
            .esGratis(false)
            .costeCreditos(20)
            .activo(true)
            .build();
            recursoInteractivoRepository.save(tictactoe);
            
            System.out.println("[DatabaseSeeder] Recursos interactivos por defecto registrados.");
        }
    }
}
