package com.example.termproject.repository;

import com.example.termproject.model.Equipment;
import java.util.List;
import java.util.Optional;

public interface EquipmentRepository {
    Equipment save(Equipment equipment);
    Optional<Equipment> findById(Long id);
    List<Equipment> findByProviderId(Long providerId);
    List<Equipment> findAll();
    List<Equipment> findReported();
    void report(Long id);
    void deactivate(Long id, boolean deactivated);
    void delete(Long id);
}
