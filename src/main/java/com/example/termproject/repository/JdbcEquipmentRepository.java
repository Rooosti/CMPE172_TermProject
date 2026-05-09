package com.example.termproject.repository;

import com.example.termproject.model.Equipment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcEquipmentRepository implements EquipmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEquipmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Equipment> equipmentRowMapper = (rs, rowNum) -> Equipment.builder()
            .equipmentId(rs.getLong("equipment_id"))
            .providerId(rs.getLong("provider_id"))
            .name(rs.getString("equipment_name"))
            .description(rs.getString("description"))
            .hourlyRate(rs.getBigDecimal("hourly_rate"))
            .isDeactivated(rs.getBoolean("is_deactivated"))
            .isReported(rs.getBoolean("is_reported"))
            .build();

    @Override
    public Equipment save(Equipment equipment) {
        String sql = "INSERT INTO equipment (provider_id, equipment_name, description, hourly_rate, is_deactivated, is_reported) VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, equipment.getProviderId());
            ps.setString(2, equipment.getName());
            ps.setString(3, equipment.getDescription());
            ps.setBigDecimal(4, equipment.getHourlyRate());
            ps.setBoolean(5, equipment.isDeactivated());
            ps.setBoolean(6, equipment.isReported());
            return ps;
        }, keyHolder);

        equipment.setEquipmentId(keyHolder.getKey().longValue());
        return equipment;
    }

    @Override
    public Optional<Equipment> findById(Long id) {
        String sql = "SELECT * FROM equipment WHERE equipment_id = ?";
        return jdbcTemplate.query(sql, equipmentRowMapper, id).stream().findFirst();
    }

    @Override
    public List<Equipment> findByProviderId(Long providerId) {
        String sql = "SELECT * FROM equipment WHERE provider_id = ?";
        return jdbcTemplate.query(sql, equipmentRowMapper, providerId);
    }

    @Override
    public List<Equipment> findAll() {
        return jdbcTemplate.query("SELECT * FROM equipment", equipmentRowMapper);
    }

    @Override
    public List<Equipment> findReported() {
        return jdbcTemplate.query("SELECT * FROM equipment WHERE is_reported = true", equipmentRowMapper);
    }

    @Override
    public void report(Long id) {
        jdbcTemplate.update("UPDATE equipment SET is_reported = true WHERE equipment_id = ?", id);
    }

    @Override
    public void deactivate(Long id, boolean deactivated) {
        jdbcTemplate.update("UPDATE equipment SET is_deactivated = ? WHERE equipment_id = ?", deactivated, id);
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM equipment WHERE equipment_id = ?", id);
    }
}
