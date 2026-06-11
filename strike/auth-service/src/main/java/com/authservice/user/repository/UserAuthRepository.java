package com.authservice.user.repository;

import com.authservice.user.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAuthRepository
        extends JpaRepository<UserAuth, Long> {

    Optional<UserAuth> findByMobileNumber(String mobileNumber);

    boolean existsByMobileNumber(String mobileNumber);
}