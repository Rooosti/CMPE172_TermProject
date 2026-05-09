package com.example.termproject.repository;

import com.example.termproject.model.Booking;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcBookingRepository implements BookingRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcBookingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Booking> bookingRowMapper = (rs, rowNum) -> Booking.builder()
            .bookingId(rs.getLong("booking_id"))
            .renterId(rs.getLong("renter_id"))
            .equipmentId(rs.getLong("equipment_id"))
            .equipmentName(rs.getString("equipment_name") != null ? rs.getString("equipment_name") : "Deleted Item")
            .startTime(rs.getTimestamp("start_time").toLocalDateTime())
            .endTime(rs.getTimestamp("end_time").toLocalDateTime())
            .totalPrice(rs.getBigDecimal("total_price"))
            .status(rs.getString("status"))
            .build();

    @Override
    public Booking save(Booking booking) {
        String sql = "INSERT INTO booking (renter_id, equipment_id, start_time, end_time, total_price, status) VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, booking.getRenterId());
            ps.setLong(2, booking.getEquipmentId());
            ps.setTimestamp(3, Timestamp.valueOf(booking.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(booking.getEndTime()));
            ps.setBigDecimal(5, booking.getTotalPrice());
            ps.setString(6, booking.getStatus());
            return ps;
        }, keyHolder);

        booking.setBookingId(keyHolder.getKey().longValue());
        return booking;
    }

    @Override
    public boolean existsOverlapping(Long equipmentId, LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT COUNT(*) FROM booking " +
                     "WHERE equipment_id = ? AND status != 'CANCELLED' " +
                     "AND start_time < ? AND end_time > ?";
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 
                equipmentId, Timestamp.valueOf(end), Timestamp.valueOf(start));
        
        return count != null && count > 0;
    }

    @Override
    public Optional<Booking> findById(Long id) {
        String sql = "SELECT b.*, e.equipment_name FROM booking b LEFT JOIN equipment e ON b.equipment_id = e.equipment_id WHERE b.booking_id = ?";
        return jdbcTemplate.query(sql, bookingRowMapper, id).stream().findFirst();
    }

    @Override
    public List<Booking> findByRenterId(Long renterId) {
        String sql = "SELECT b.*, e.equipment_name FROM booking b LEFT JOIN equipment e ON b.equipment_id = e.equipment_id WHERE b.renter_id = ?";
        return jdbcTemplate.query(sql, bookingRowMapper, renterId);
    }

    @Override
    public List<Booking> findByEquipmentId(Long equipmentId) {
        String sql = "SELECT b.*, e.equipment_name FROM booking b LEFT JOIN equipment e ON b.equipment_id = e.equipment_id WHERE b.equipment_id = ?";
        return jdbcTemplate.query(sql, bookingRowMapper, equipmentId);
    }

    @Override
    public List<Booking> findByProviderId(Long providerId) {
        String sql = "SELECT b.*, e.equipment_name FROM booking b " +
                     "LEFT JOIN equipment e ON b.equipment_id = e.equipment_id " +
                     "WHERE e.provider_id = ?";
        return jdbcTemplate.query(sql, bookingRowMapper, providerId);
    }

    @Override
    public List<Long> findPartnersByUserId(Long userId) {
        String sql = "SELECT DISTINCT renter_id FROM booking b JOIN equipment e ON b.equipment_id = e.equipment_id WHERE e.provider_id = ? " +
                     "UNION " +
                     "SELECT DISTINCT e.provider_id FROM booking b JOIN equipment e ON b.equipment_id = e.equipment_id WHERE b.renter_id = ?";
        return jdbcTemplate.queryForList(sql, Long.class, userId, userId);
    }
}
