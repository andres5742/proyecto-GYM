package com.gym.management.config;

import com.gym.management.model.AppModule;
import com.gym.management.model.CarouselSlide;
import com.gym.management.model.ModuleCategory;
import com.gym.management.model.Employee;
import com.gym.management.model.GymMediaItem;
import com.gym.management.model.Holiday;
import com.gym.management.model.MediaType;
import com.gym.management.model.MembershipPlan;
import com.gym.management.model.PostCategory;
import com.gym.management.model.SiteFooter;
import com.gym.management.model.UserRole;
import com.gym.management.model.WallPost;
import com.gym.management.repository.AppModuleRepository;
import com.gym.management.repository.CarouselSlideRepository;
import com.gym.management.repository.EmployeeRepository;
import com.gym.management.repository.GymMediaItemRepository;
import com.gym.management.repository.HolidayRepository;
import com.gym.management.repository.MembershipPlanRepository;
import com.gym.management.repository.SiteFooterRepository;
import com.gym.management.repository.WallPostRepository;
import com.gym.management.service.AppModuleService;
import com.gym.management.service.BusinessHoursService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final AppModuleRepository appModuleRepository;
    private final AppModuleService appModuleService;
    private final MembershipPlanRepository planRepository;
    private final EmployeeRepository employeeRepository;
    private final HolidayRepository holidayRepository;
    private final WallPostRepository wallPostRepository;
    private final CarouselSlideRepository carouselSlideRepository;
    private final GymMediaItemRepository gymMediaItemRepository;
    private final SiteFooterRepository siteFooterRepository;
    private final BusinessHoursService businessHoursService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedAppModules() {
        return args -> {
            if (appModuleRepository.count() > 0) {
                return;
            }
            int order = 0;
            saveModule("SOCIOS", "Socios", "Gestión de socios del gimnasio", ModuleCategory.PANEL, order++);
            saveModule("PLANES", "Planes", "Planes de membresía", ModuleCategory.PANEL, order++);
            saveModule("INVENTARIO", "Inventario", "Productos y stock", ModuleCategory.PANEL, order++);
            saveModule("VENTAS", "Ventas", "Registro de ventas", ModuleCategory.PANEL, order++);
            saveModule(
                    "ENTREGA_TURNO",
                    "Entrega de turno",
                    "Cierre de caja y entrega de dinero del entrenador",
                    ModuleCategory.PANEL,
                    order++);
            saveModule("JORNADA", "Jornada", "Control de jornada del equipo", ModuleCategory.PANEL, order++);
            saveModule("ENTRENADORES", "Entrenadores", "Personal y accesos del equipo", ModuleCategory.PANEL, order++);
            saveModule("NOMINA", "Pago por hora", "Configuración de nómina", ModuleCategory.PANEL, order++);
            saveModule("CONTENIDO_INICIO", "Contenido inicio", "Slider, galería y pie de página", ModuleCategory.PANEL, order++);
            saveModule("BUZON", "Buzón (panel)", "Revisión de sugerencias y quejas", ModuleCategory.PANEL, order++);
            saveModule(
                    "CALIFICACIONES",
                    "Calificaciones",
                    "Ranking mensual de entrenadores",
                    ModuleCategory.PANEL,
                    order++);
            saveModule("ACCESO", "Acceso / torniquete", "Huella y torniquete", ModuleCategory.PANEL, order++);
            saveModule(
                    "MODULOS_SISTEMA",
                    "Módulos del sistema",
                    "Activar o desactivar funciones (solo super admin)",
                    ModuleCategory.PANEL,
                    order++);
            saveModule(
                    "PUBLIC_BUZON",
                    "Buzón público",
                    "Botón de sugerencias en la página de inicio",
                    ModuleCategory.PUBLIC,
                    100);
            saveModule(
                    "PUBLIC_CALIFICACIONES",
                    "Calificar entrenador",
                    "Calificación de entrenadores en el inicio",
                    ModuleCategory.PUBLIC,
                    101);
            saveModule(
                    "PUBLIC_ACCESO",
                    "Pantalla de acceso",
                    "Kiosco del torniquete (/acceso)",
                    ModuleCategory.PUBLIC,
                    102);
        };
    }

    @Bean
    CommandLineRunner ensureEntregaTurnoModule() {
        return args -> {
            if (!appModuleRepository.existsById("ENTREGA_TURNO")) {
                saveModule(
                        "ENTREGA_TURNO",
                        "Entrega de turno",
                        "Cierre de caja y entrega de dinero del entrenador",
                        ModuleCategory.PANEL,
                        6);
            }
            appModuleService.ensureDefaultRolePermissions();
        };
    }

    @Bean
    CommandLineRunner ensureRoleModulePermissions() {
        return args -> appModuleService.ensureDefaultRolePermissions();
    }

    @Bean
    CommandLineRunner relaxBillingGuestMemberColumn(JdbcTemplate jdbc) {
        return args -> {
            try {
                jdbc.execute("ALTER TABLE billing_payments ALTER COLUMN member_id DROP NOT NULL");
            } catch (Exception ignored) {
                // Columna ya nullable o tabla aún no creada
            }
        };
    }

    @Bean
    CommandLineRunner ensureBillingModuleAndDayPlan() {
        return args -> {
            if (appModuleRepository.findById("FACTURACION").isEmpty()) {
                saveModule(
                        "FACTURACION",
                        "Facturación",
                        "Entrenos del día, membresías y medios de pago",
                        ModuleCategory.PANEL,
                        61);
            }
            appModuleService.ensureFacturacionStaffAccess();
            appModuleService.ensureDefaultRolePermissions();
            if (planRepository.findByNameIgnoreCase("Entreno día").isEmpty()) {
                planRepository.save(MembershipPlan.builder()
                        .name("Entreno día")
                        .description("Pase de entreno por un solo día (Facturación / F2)")
                        .durationDays(1)
                        .price(new BigDecimal("15000"))
                        .active(true)
                        .build());
            }
        };
    }

    private void saveModule(
            String code, String name, String description, ModuleCategory category, int sortOrder) {
        appModuleRepository.save(AppModule.builder()
                .code(code)
                .name(name)
                .description(description)
                .category(category)
                .enabled(true)
                .sortOrder(sortOrder)
                .build());
    }

    @Bean
    CommandLineRunner seedPlans() {
        return args -> {
            if (planRepository.count() > 0) {
                return;
            }
            planRepository.save(MembershipPlan.builder()
                    .name("Mensual")
                    .description("Acceso ilimitado por 30 días")
                    .durationDays(30)
                    .price(new BigDecimal("120000"))
                    .build());
            planRepository.save(MembershipPlan.builder()
                    .name("Trimestral")
                    .description("Acceso ilimitado por 90 días")
                    .durationDays(90)
                    .price(new BigDecimal("320000"))
                    .build());
            planRepository.save(MembershipPlan.builder()
                    .name("Anual")
                    .description("Acceso ilimitado por 365 días")
                    .durationDays(365)
                    .price(new BigDecimal("1100000"))
                    .build());
        };
    }

    @Bean
    CommandLineRunner seedEmployees() {
        return args -> {
            if (employeeRepository.count() > 0) {
                return;
            }
            employeeRepository.save(Employee.builder()
                    .firstName("Super")
                    .lastName("Admin")
                    .phone("3000000000")
                    .username("superadmin")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(UserRole.SUPER_ADMIN)
                    .nequiNumber("3000000000")
                    .active(true)
                    .build());
            employeeRepository.save(Employee.builder()
                    .firstName("Andrés")
                    .lastName("Pérez")
                    .phone("3000000001")
                    .username("andres.perez")
                    .passwordHash(passwordEncoder.encode("andres.perez574"))
                    .role(UserRole.SUPER_ADMIN)
                    .nequiNumber("3000000001")
                    .active(true)
                    .build());
            if (employeeRepository.count() <= 1) {
                employeeRepository.save(Employee.builder()
                        .firstName("Carlos")
                        .lastName("Gómez")
                        .phone("3001234567")
                        .username("entrenador1")
                        .passwordHash(passwordEncoder.encode("entrenador123"))
                        .role(UserRole.TRAINER)
                        .nequiNumber("3001234567")
                        .active(true)
                        .ratingEligible(true)
                        .build());
            }
        };
    }

    @Bean
    CommandLineRunner seedHomeContent() {
        return args -> {
            businessHoursService.get();

            seedColombiaHolidays();
            seedCarouselAndMedia();
            seedFooter();

            if (wallPostRepository.count() == 0) {
                employeeRepository.findByUsernameIgnoreCase("andres.perez").stream()
                        .findFirst()
                        .or(() -> employeeRepository.findAll().stream().findFirst())
                        .ifPresent(admin -> {
                            Instant now = Instant.now();
                            wallPostRepository.save(WallPost.builder()
                                    .author(admin)
                                    .title("Bienvenidos al muro del gimnasio")
                                    .body(
                                            "Aquí publicamos avisos, horarios, promociones y novedades. "
                                                    + "Las publicaciones temporales se eliminan solas al vencer.")
                                    .emoji("📌")
                                    .category(PostCategory.AVISO)
                                    .publishedAt(now)
                                    .permanent(true)
                                    .build());
                        });
            }
        };
    }

    private void seedCarouselAndMedia() {
        if (carouselSlideRepository.count() == 0) {
            carouselSlideRepository.save(CarouselSlide.builder()
                    .imageUrl("https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=1400&q=80")
                    .title("Tu mejor versión empieza hoy")
                    .caption("Zona de musculación y cardio")
                    .displayOrder(0)
                    .active(true)
                    .build());
            carouselSlideRepository.save(CarouselSlide.builder()
                    .imageUrl("https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=1400&q=80")
                    .title("Clases y entrenamiento personal")
                    .caption("Entrenadores certificados")
                    .displayOrder(1)
                    .active(true)
                    .build());
            carouselSlideRepository.save(CarouselSlide.builder()
                    .imageUrl("https://images.unsplash.com/photo-1540497077202-7c8a3999166f?w=1400&q=80")
                    .title("Ambiente motivador")
                    .caption("Sport Gym R.IO")
                    .displayOrder(2)
                    .active(true)
                    .build());
        }

        if (gymMediaItemRepository.count() == 0) {
            gymMediaItemRepository.save(GymMediaItem.builder()
                    .mediaType(MediaType.PHOTO)
                    .mediaUrl("https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?w=800&q=80")
                    .title("Zona de pesas")
                    .displayOrder(0)
                    .active(true)
                    .build());
            gymMediaItemRepository.save(GymMediaItem.builder()
                    .mediaType(MediaType.PHOTO)
                    .mediaUrl("https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800&q=80")
                    .title("Cardio")
                    .displayOrder(1)
                    .active(true)
                    .build());
            gymMediaItemRepository.save(GymMediaItem.builder()
                    .mediaType(MediaType.VIDEO)
                    .mediaUrl("https://www.youtube.com/embed/MLpWrANjFbI")
                    .thumbnailUrl("https://images.unsplash.com/photo-1518611012118-696072aa579a?w=800&q=80")
                    .title("Conoce el gimnasio")
                    .displayOrder(2)
                    .active(true)
                    .build());
        }
    }

    private void seedFooter() {
        if (siteFooterRepository.existsById(SiteFooter.SINGLETON_ID)) {
            return;
        }
        siteFooterRepository.save(SiteFooter.builder()
                .id(SiteFooter.SINGLETON_ID)
                .tagline("Sport Gym R.IO — Entrena fuerte, vive mejor")
                .address("Riohacha, La Guajira")
                .phone("300 000 0000")
                .instagramUrl("https://instagram.com")
                .facebookUrl("https://facebook.com")
                .whatsappUrl("https://wa.me/573000000000")
                .build());
    }

    private void seedColombiaHolidays() {
        int currentYear = LocalDate.now().getYear();
        for (int year = currentYear - 1; year <= currentYear + 1; year++) {
            for (ColombiaHolidayCatalog.HolidayEntry entry : ColombiaHolidayCatalog.forYear(year)) {
                if (holidayRepository.existsByDate(entry.date())) {
                    continue;
                }
                holidayRepository.save(Holiday.builder()
                        .date(entry.date())
                        .name(entry.name())
                        .description("Festivo oficial Colombia")
                        .build());
            }
        }
    }
}
