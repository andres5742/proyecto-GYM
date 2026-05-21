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
            saveModule(
                    "DESCUADRES_CAJA",
                    "Descuadres de caja",
                    "Faltantes de efectivo en entrega de turno para cobro al cierre del mes",
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
            if (!appModuleRepository.existsById("DESCUADRES_CAJA")) {
                saveModule(
                        "DESCUADRES_CAJA",
                        "Descuadres de caja",
                        "Registro de faltantes de efectivo en entrega de turno para cobro mensual",
                        ModuleCategory.PANEL,
                        7);
            }
            if (!appModuleRepository.existsById("FIADO")) {
                saveModule(
                        "FIADO",
                        "Fiado",
                        "Crédito de productos a afiliados y registro de abonos",
                        ModuleCategory.PANEL,
                        8);
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
    CommandLineRunner ensureBillingPaymentTypeConstraint(JdbcTemplate jdbc) {
        return args -> {
            try {
                jdbc.execute(
                        """
                        ALTER TABLE billing_payments
                        DROP CONSTRAINT IF EXISTS billing_payments_payment_type_check
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE billing_payments
                        ADD CONSTRAINT billing_payments_payment_type_check
                        CHECK (payment_type IN ('DAY_WORKOUT', 'SPORTS_DANCE', 'MEMBERSHIP'))
                        """);
            } catch (Exception ignored) {
                // Tabla aún no creada o motor distinto a PostgreSQL
            }
        };
    }

    @Bean
    CommandLineRunner ensureMemberMembershipFreezeColumns(JdbcTemplate jdbc) {
        return args -> {
            try {
                jdbc.execute(
                        """
                        ALTER TABLE members
                        ADD COLUMN IF NOT EXISTS membership_frozen BOOLEAN DEFAULT FALSE
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE members
                        ADD COLUMN IF NOT EXISTS frozen_remaining_days INTEGER
                        """);
                jdbc.execute(
                        """
                        UPDATE members SET membership_frozen = FALSE
                        WHERE membership_frozen IS NULL
                        """);
            } catch (Exception ignored) {
                // Tabla aún no creada
            }
        };
    }

    @Bean
    CommandLineRunner ensureMembershipPlanTiqueteraColumns(JdbcTemplate jdbc) {
        return args -> {
            try {
                jdbc.execute(
                        """
                        ALTER TABLE membership_plans
                        ADD COLUMN IF NOT EXISTS plan_kind VARCHAR(20) DEFAULT 'REGULAR'
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE membership_plans
                        ADD COLUMN IF NOT EXISTS monthly_entry_limit INTEGER
                        """);
                jdbc.execute(
                        """
                        UPDATE membership_plans
                        SET plan_kind = 'REGULAR'
                        WHERE plan_kind IS NULL
                        """);
                jdbc.execute(
                        """
                        UPDATE membership_plans
                        SET plan_kind = 'TIQUETERA',
                            monthly_entry_limit = COALESCE(monthly_entry_limit, 16),
                            duration_days = CASE
                                WHEN duration_days IS NULL OR duration_days < 7 THEN 30
                                ELSE duration_days
                            END
                        WHERE LOWER(name) LIKE '%tiquetera%'
                          AND (plan_kind IS NULL OR plan_kind = 'REGULAR')
                        """);
                jdbc.execute(
                        """
                        UPDATE membership_plans
                        SET monthly_entry_limit = COALESCE(monthly_entry_limit, 16),
                            duration_days = CASE
                                WHEN duration_days IS NULL OR duration_days < 7 THEN 30
                                ELSE duration_days
                            END
                        WHERE plan_kind = 'TIQUETERA'
                          AND (monthly_entry_limit IS NULL OR monthly_entry_limit < 1)
                        """);
            } catch (Exception ignored) {
                // Tabla aún no creada
            }
        };
    }

    @Bean
    CommandLineRunner ensureBillingExpensePaymentMethod(JdbcTemplate jdbc) {
        return args -> {
            try {
                jdbc.execute(
                        """
                        ALTER TABLE billing_cash_register_expenses
                        ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20)
                        """);
                jdbc.execute(
                        """
                        UPDATE billing_cash_register_expenses
                        SET payment_method = 'CASH'
                        WHERE payment_method IS NULL
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE billing_cash_register_expenses
                        ALTER COLUMN payment_method SET DEFAULT 'CASH'
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE billing_cash_register_expenses
                        ALTER COLUMN payment_method SET NOT NULL
                        """);
            } catch (Exception ignored) {
                // Tabla aún no creada
            }
        };
    }

    @Bean
    CommandLineRunner ensureCashShortfallInventoryDetail(JdbcTemplate jdbc) {
        return args -> {
            try {
                jdbc.execute(
                        """
                        ALTER TABLE employee_cash_shortfalls
                        ADD COLUMN IF NOT EXISTS kind VARCHAR(20)
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE employee_cash_shortfalls
                        ADD COLUMN IF NOT EXISTS inventory_missing_json TEXT
                        """);
                jdbc.execute(
                        """
                        UPDATE employee_cash_shortfalls
                        SET kind = 'CASH_HANDOVER'
                        WHERE kind IS NULL
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE employee_cash_shortfalls
                        ADD COLUMN IF NOT EXISTS billing_cash_register_id BIGINT
                        """);
                jdbc.execute(
                        """
                        UPDATE employee_cash_shortfalls
                        SET kind = 'INVENTORY'
                        WHERE shift_handover_id IS NULL
                          AND billing_cash_register_id IS NULL
                          AND (notes ILIKE '%inventario%' OR inventory_missing_json IS NOT NULL)
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE employee_cash_shortfalls
                        ALTER COLUMN notes TYPE TEXT
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE employee_cash_shortfalls
                        DROP CONSTRAINT IF EXISTS employee_cash_shortfalls_kind_check
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE employee_cash_shortfalls
                        ADD CONSTRAINT employee_cash_shortfalls_kind_check
                        CHECK (kind IN ('CASH_HANDOVER', 'INVENTORY', 'CASH_REGISTER'))
                        """);
            } catch (Exception ignored) {
                // Tabla aún no creada
            }
        };
    }

    @Bean
    CommandLineRunner ensureBiometricCredentialTypeCard(JdbcTemplate jdbc) {
        return args -> {
            try {
                jdbc.execute(
                        """
                        ALTER TABLE access_logs
                        DROP CONSTRAINT IF EXISTS access_logs_credential_type_check
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE access_logs
                        ADD CONSTRAINT access_logs_credential_type_check
                        CHECK (credential_type IN ('FINGERPRINT', 'FACE', 'CARD'))
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE member_biometric_credentials
                        DROP CONSTRAINT IF EXISTS member_biometric_credentials_credential_type_check
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE member_biometric_credentials
                        ADD CONSTRAINT member_biometric_credentials_credential_type_check
                        CHECK (credential_type IN ('FINGERPRINT', 'FACE', 'CARD'))
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE employee_biometric_credentials
                        DROP CONSTRAINT IF EXISTS employee_biometric_credentials_credential_type_check
                        """);
                jdbc.execute(
                        """
                        ALTER TABLE employee_biometric_credentials
                        ADD CONSTRAINT employee_biometric_credentials_credential_type_check
                        CHECK (credential_type IN ('FINGERPRINT', 'FACE', 'CARD'))
                        """);
            } catch (Exception ignored) {
                // Tabla aún no creada
            }
        };
    }

    @Bean
    CommandLineRunner dedupeBillingCashRegisters(JdbcTemplate jdbc) {
        return args -> {
            try {
                jdbc.execute(
                        """
                        DELETE FROM billing_cash_register_expenses e
                        WHERE e.cash_register_id IN (
                          SELECT a.id FROM billing_cash_registers a
                          INNER JOIN billing_cash_registers b
                            ON a.register_date = b.register_date AND a.id < b.id
                        )
                        """);
                jdbc.execute(
                        """
                        UPDATE billing_payments p
                        SET billing_cash_register_id = (
                          SELECT MAX(b.id) FROM billing_cash_registers b
                          WHERE b.register_date = (
                            SELECT register_date FROM billing_cash_registers WHERE id = p.billing_cash_register_id
                          )
                        )
                        WHERE billing_cash_register_id IN (
                          SELECT a.id FROM billing_cash_registers a
                          INNER JOIN billing_cash_registers b
                            ON a.register_date = b.register_date AND a.id < b.id
                        )
                        """);
                jdbc.execute(
                        """
                        DELETE FROM billing_cash_registers a
                        USING billing_cash_registers b
                        WHERE a.register_date = b.register_date AND a.id < b.id
                        """);
                jdbc.execute(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS uk_billing_cash_register_date
                        ON billing_cash_registers (register_date)
                        """);
            } catch (Exception ignored) {
                // Tablas aún no creadas
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
            if (planRepository.findByNameIgnoreCase("Bailes deportivos").isEmpty()) {
                planRepository.save(MembershipPlan.builder()
                        .name("Bailes deportivos")
                        .description("Pase de bailes deportivos por un solo día (Facturación / F3)")
                        .durationDays(1)
                        .price(new BigDecimal("12000"))
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
                    .firstName("Andrés")
                    .lastName("Pérez")
                    .phone("3000000001")
                    .username("andres.perez")
                    .passwordHash(passwordEncoder.encode("andres.perez574"))
                    .role(UserRole.SUPER_ADMIN)
                    .nequiNumber("3000000001")
                    .active(true)
                    .build());
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
        };
    }

    @Bean
    CommandLineRunner ensureSingleSuperAdminAccount() {
        return args -> {
            employeeRepository.findByUsernameIgnoreCase("andres.perez").ifPresentOrElse(
                    andres -> {
                        andres.setRole(UserRole.SUPER_ADMIN);
                        andres.setActive(true);
                        andres.setPasswordHash(passwordEncoder.encode("andres.perez574"));
                        employeeRepository.save(andres);
                    },
                    () -> employeeRepository.save(Employee.builder()
                            .firstName("Andrés")
                            .lastName("Pérez")
                            .phone("3000000001")
                            .username("andres.perez")
                            .passwordHash(passwordEncoder.encode("andres.perez574"))
                            .role(UserRole.SUPER_ADMIN)
                            .nequiNumber("3000000001")
                            .active(true)
                            .build()));

            employeeRepository.findByUsernameIgnoreCase("superadmin").ifPresent(legacy -> {
                employeeRepository
                        .findByUsernameIgnoreCase("andres.perez")
                        .ifPresent(owner -> wallPostRepository.findAll().forEach(post -> {
                            if (post.getAuthor() != null
                                    && post.getAuthor().getId().equals(legacy.getId())) {
                                post.setAuthor(owner);
                                wallPostRepository.save(post);
                            }
                        }));
                legacy.setActive(false);
                legacy.setUsername("retired.superadmin." + legacy.getId());
                legacy.setPasswordHash(passwordEncoder.encode("disabled-" + legacy.getId()));
                employeeRepository.save(legacy);
            });
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
