package com.mindpulse.backend.mapper;

import com.mindpulse.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    User findById(@Param("id") Long id);

    User findByUsername(@Param("username") String username);

    User findByEmail(@Param("email") String email);

    boolean existsByUsername(@Param("username") String username);

    boolean existsByEmail(@Param("email") String email);

    void insertUser(User user);

    void updateUser(User user);

    void deleteById(@Param("id") Long id);

    List<User> findAll();

    int countAll();

    void updateRole(@Param("id") Long id, @Param("role") String role);
}