package mexa.club.authservice.config;

import mexa.club.authservice.entity.Permission;
import mexa.club.authservice.entity.Role;
import mexa.club.authservice.entity.User;
import mexa.club.authservice.repository.RoleRepository;
import mexa.club.authservice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // ── Default system roles ───────────────────────────────────────────────
        // ROLE_SUPER_ADMIN va ROLE_USER — asosiy tizim rollari (o'chirib bo'lmaydi).
        // ROLE_ADMIN — qo'shimcha boshqaruv roli.
        Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_SUPER_ADMIN");
            return role;
        });
        superAdminRole.setSystem(true);
        superAdminRole.setDisplayName("Super Admin");
        superAdminRole.setDescription("Barcha huquqlarga ega tizim boshqaruvchisi. O'chirib bo'lmaydi.");
        roleRepository.save(superAdminRole);

        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_ADMIN");
            return role;
        });
        adminRole.setSystem(true);
        adminRole.setDisplayName("Admin");
        adminRole.setDescription("Kontent va buyurtmalarni boshqaruvchi administrator.");
        roleRepository.save(adminRole);

        Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_USER");
            return role;
        });
        // ROLE_USER — har bir yangi ro'yxatdan o'tgan foydalanuvchiga avtomatik beriladigan standart rol.
        userRole.setSystem(true);
        userRole.setDisplayName("Foydalanuvchi (Xaridor)");
        userRole.setDescription("Ro'yxatdan o'tganda avtomatik beriladigan standart rol. Sotib oluvchi.");
        roleRepository.save(userRole);

        Role warehouseRole = roleRepository.findByName("ROLE_WAREHOUSE").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_WAREHOUSE");
            return role;
        });
        warehouseRole.setSystem(true);
        warehouseRole.setDisplayName("Ombor Boshqaruvchi");
        warehouseRole.setDescription("Ombor va zaxiralarni boshqaruvchi xodim.");
        roleRepository.save(warehouseRole);

        Role courierRole = roleRepository.findByName("ROLE_COURIER").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_COURIER");
            return role;
        });
        courierRole.setSystem(true);
        courierRole.setDisplayName("Kuryer");
        courierRole.setDescription("Yetkazib berish xizmati xodimi.");
        roleRepository.save(courierRole);

        // ── Seed role permissions (idempotent) ────────────────────────────────

        // SUPER_ADMIN har doim barcha permissionlarga ega bo'lishi kerak (yangi permission qo'shilsa ham)
        Set<Permission> allPermissions = EnumSet.allOf(Permission.class);
        if (!allPermissions.equals(superAdminRole.getPermissions())) {
            superAdminRole.setPermissions(allPermissions);
            roleRepository.save(superAdminRole);
        }

        if (adminRole.getPermissions().isEmpty()) {
            adminRole.setPermissions(EnumSet.of(
                    Permission.USER_VIEW, Permission.USER_CREATE, Permission.USER_EDIT, Permission.USER_DELETE,
                    Permission.PRODUCT_VIEW, Permission.PRODUCT_CREATE, Permission.PRODUCT_EDIT,
                    Permission.PRODUCT_DELETE, Permission.PRODUCT_MANAGE,
                    Permission.ORDER_VIEW, Permission.ORDER_CREATE, Permission.ORDER_EDIT,
                    Permission.ORDER_DELETE, Permission.ORDER_MANAGE,
                    Permission.WAREHOUSE_VIEW, Permission.WAREHOUSE_MANAGE,
                    Permission.STOCK_VIEW, Permission.STOCK_MANAGE,
                    Permission.AUDIT_LOG_VIEW, Permission.AUDIT_LOG_EXPORT,
                    Permission.PAYMENT_VIEW, Permission.PAYMENT_PROCESS, Permission.PAYMENT_REFUND,
                    Permission.PAYMENT_REPORT,
                    Permission.DELIVERY_VIEW, Permission.DELIVERY_MANAGE,
                    Permission.DELIVERY_ASSIGN, Permission.DELIVERY_STATUS_UPDATE,
                    Permission.COURIER_VIEW, Permission.COURIER_MANAGE,
                    Permission.REPORT_VIEW, Permission.REPORT_EXPORT,
                    Permission.REPORT_FINANCIAL, Permission.REPORT_WAREHOUSE,
                    Permission.ANALYTICS_VIEW, Permission.ANALYTICS_EXPORT,
                    Permission.DISCOUNT_VIEW, Permission.DISCOUNT_MANAGE,
                    Permission.NOTIFICATION_VIEW, Permission.NOTIFICATION_MANAGE,
                    Permission.ROLE_MANAGE,
                    Permission.GROUP_VIEW, Permission.GROUP_CREATE, Permission.GROUP_EDIT,
                    Permission.GROUP_DELETE, Permission.GROUP_MANAGE
            ));
            roleRepository.save(adminRole);
        }

        if (userRole.getPermissions().isEmpty()) {
            userRole.setPermissions(EnumSet.of(
                    Permission.PRODUCT_VIEW,
                    Permission.ORDER_VIEW, Permission.ORDER_CREATE,
                    Permission.PAYMENT_VIEW,
                    Permission.DELIVERY_VIEW,
                    Permission.DISCOUNT_VIEW,
                    Permission.NOTIFICATION_VIEW
            ));
            roleRepository.save(userRole);
        }

        if (warehouseRole.getPermissions().isEmpty()) {
            warehouseRole.setPermissions(EnumSet.of(
                    Permission.WAREHOUSE_VIEW, Permission.WAREHOUSE_MANAGE,
                    Permission.STOCK_VIEW, Permission.STOCK_MANAGE,
                    Permission.PRODUCT_VIEW,
                    Permission.REPORT_WAREHOUSE,
                    Permission.AUDIT_LOG_VIEW
            ));
            roleRepository.save(warehouseRole);
        }

        if (courierRole.getPermissions().isEmpty()) {
            courierRole.setPermissions(EnumSet.of(
                    Permission.DELIVERY_VIEW,
                    Permission.DELIVERY_STATUS_UPDATE
            ));
            roleRepository.save(courierRole);
        }

        // ── Seed default superadmin user ──────────────────────────────────────

        if (!userRepository.existsByUsername("superadmin")) {
            User superAdmin = new User();
            superAdmin.setUsername("superadmin");
            superAdmin.setEmail("superadmin@example.com");
            superAdmin.setFullName("System Admin");
            superAdmin.setPassword(passwordEncoder.encode("super123"));
            superAdmin.setEnabled(true);
            superAdmin.setVerified(true);
            Set<Role> roles = new HashSet<>();
            roles.add(superAdminRole);
            superAdmin.setRoles(roles);
            userRepository.save(superAdmin);
        }
    }
}
