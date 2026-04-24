package com.travelo.admin.bootstrap;

import com.travelo.admin.domain.AdminRole;
import com.travelo.admin.domain.AdminUser;
import com.travelo.admin.domain.FeatureFlag;
import com.travelo.admin.domain.FeaturePlatform;
import com.travelo.admin.repository.AdminUserRepository;
import com.travelo.admin.repository.FeatureFlagRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class AdminDataInitializer implements ApplicationRunner {
    private final AdminUserRepository users;
    private final FeatureFlagRepository flags;
    private final PasswordEncoder passwordEncoder;
    @Value("${admin.bootstrap.username:admin}")
    private String bootstrapUser;
    @Value("${admin.bootstrap.password:ChangeMe!Admin1}")
    private String bootstrapPassword;

    public AdminDataInitializer(AdminUserRepository users, FeatureFlagRepository flags, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.flags = flags;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() == 0) {
            var u = new AdminUser();
            u.setUsername(bootstrapUser);
            u.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
            u.setRole(AdminRole.ADMIN);
            users.save(u);
        }
        if (flags.findByFeatureNameIgnoreCaseAndPlatform("test_rollout", FeaturePlatform.WEB).isEmpty()) {
            var f = new FeatureFlag();
            f.setFeatureName("test_rollout");
            f.setEnabled(false);
            f.setRolloutPercentage(0);
            f.setPlatform(FeaturePlatform.WEB);
            flags.save(f);
        }
    }
}
