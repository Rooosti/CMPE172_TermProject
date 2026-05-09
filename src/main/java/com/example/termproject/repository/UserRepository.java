package com.example.termproject.repository;

import com.example.termproject.model.User;
import java.util.Optional;
import java.util.List;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    void deleteById(Long id);
    
    // Registration helpers
    void addRole(Long userId, String roleName);
    void registerAsRenter(Long userId);
    void registerAsProvider(Long userId);
    List<String> findRolesByUserId(Long userId);
}
