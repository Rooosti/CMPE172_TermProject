package com.example.termproject.repository;

import com.example.termproject.model.Address;
import com.example.termproject.model.User;
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
public class JdbcUserRepository implements UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // This is the "Mapper" part of the Data Mapper pattern
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> User.builder()
            .userId(rs.getLong("user_id"))
            .username(rs.getString("username"))
            .password(rs.getString("password"))
            .firstName(rs.getString("first_name"))
            .middleName(rs.getString("middle_name"))
            .lastName(rs.getString("last_name"))
            .phoneNumber(rs.getString("phone_number"))
            .address(Address.builder()
                    .street(rs.getString("street"))
                    .city(rs.getString("city"))
                    .state(rs.getString("state"))
                    .zipCode(rs.getString("zip_code"))
                    .buildingNo(rs.getString("building_no"))
                    .build())
            .build();

    @Override
    public User save(User user) {
        String sql = "INSERT INTO user (username, password, first_name, middle_name, last_name, phone_number, street, city, state, zip_code, building_no) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFirstName());
            ps.setString(4, user.getMiddleName());
            ps.setString(5, user.getLastName());
            ps.setString(6, user.getPhoneNumber());
            ps.setString(7, user.getAddress().getStreet());
            ps.setString(8, user.getAddress().getCity());
            ps.setString(9, user.getAddress().getState());
            ps.setString(10, user.getAddress().getZipCode());
            ps.setString(11, user.getAddress().getBuildingNo());
            return ps;
        }, keyHolder);

        user.setUserId(keyHolder.getKey().longValue());
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM user WHERE user_id = ?";
        return jdbcTemplate.query(sql, userRowMapper, id).stream().findFirst();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM user WHERE username = ?";
        return jdbcTemplate.query(sql, userRowMapper, username).stream().findFirst();
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM user";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM user WHERE user_id = ?", id);
    }

    @Override
    public void addRole(Long userId, String roleName) {
        String sql = "INSERT INTO user_role (user_id, role_id) " +
                     "SELECT ?, role_id FROM role WHERE role_name = ?";
        jdbcTemplate.update(sql, userId, roleName);
    }

    @Override
    public void registerAsRenter(Long userId) {
        jdbcTemplate.update("INSERT IGNORE INTO renter (user_id) VALUES (?)", userId);
    }

    @Override
    public void registerAsProvider(Long userId) {
        jdbcTemplate.update("INSERT IGNORE INTO provider (user_id) VALUES (?)", userId);
    }

    @Override
    public List<String> findRolesByUserId(Long userId) {
        String sql = "SELECT r.role_name FROM role r " +
                     "JOIN user_role ur ON r.role_id = ur.role_id " +
                     "WHERE ur.user_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("role_name"), userId);
    }
}
