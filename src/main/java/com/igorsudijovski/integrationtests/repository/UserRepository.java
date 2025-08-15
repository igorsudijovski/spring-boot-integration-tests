package com.igorsudijovski.integrationtests.repository;

import com.igorsudijovski.integrationtests.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
}
