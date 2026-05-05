package com.stock_tracker.grey_toad.config;

import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class HeadAdminInitializer implements ApplicationRunner {

    @Value("${head-admin.email}")
    private String headAdminEmail;

    @Value("${head-admin.username}")
    private String headAdminUsername;

    @Value("${head-admin.password}")
    private String headAdminPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public HeadAdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Optional<User> existing = userRepository.findByEmailIncludeDeleted(headAdminEmail);

        if (existing.isPresent()) {
            User user = existing.get();
            user.setDeleted(false);
            user.setRole("ADMIN");
            user.setHeadAdmin(true);
            user.setJobTitle("Head Admin");
            userRepository.save(user);
        } else {
            User user = new User();
            user.setEmail(headAdminEmail);
            user.setUsername(headAdminUsername);
            user.setPassword(passwordEncoder.encode(headAdminPassword));
            user.setRole("ADMIN");
            user.setHeadAdmin(true);
            user.setJobTitle("Head Admin");
            userRepository.save(user);
        }
    }
}
