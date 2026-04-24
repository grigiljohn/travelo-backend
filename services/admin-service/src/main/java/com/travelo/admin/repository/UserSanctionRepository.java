package com.travelo.admin.repository;

import com.travelo.admin.domain.UserSanction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSanctionRepository extends JpaRepository<UserSanction, String> {
}
