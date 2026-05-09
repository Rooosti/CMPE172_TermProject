package com.example.termproject.repository;

import com.example.termproject.model.ChatMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class JdbcMessageRepository implements MessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ChatMessage> messageRowMapper = (rs, rowNum) -> ChatMessage.builder()
            .messageId(rs.getLong("message_id"))
            .senderId(rs.getLong("sender_id"))
            .receiverId(rs.getLong("receiver_id"))
            .equipmentId(rs.getLong("equipment_id"))
            .content(rs.getString("content"))
            .timestamp(rs.getTimestamp("timestamp").toLocalDateTime())
            .senderUsername(rs.getString("username"))
            .build();

    @Override
    public ChatMessage save(ChatMessage message) {
        String sql = "INSERT INTO message (sender_id, receiver_id, equipment_id, content) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, message.getSenderId());
            ps.setLong(2, message.getReceiverId());
            ps.setLong(3, message.getEquipmentId());
            ps.setString(4, message.getContent());
            return ps;
        }, keyHolder);

        message.setMessageId(keyHolder.getKey().longValue());
        return message;
    }

    @Override
    public List<ChatMessage> findChatHistory(Long userA, Long userB, Long equipmentId) {
        String sql = "SELECT m.*, u.username FROM message m " +
                     "JOIN user u ON m.sender_id = u.user_id " +
                     "WHERE ((m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?)) " +
                     "AND m.equipment_id = ? ORDER BY m.timestamp ASC";
        return jdbcTemplate.query(sql, messageRowMapper, userA, userB, userB, userA, equipmentId);
    }

    @Override
    public List<Long> findInteractedUserIds(Long userId) {
        String sql = "SELECT DISTINCT sender_id FROM message WHERE receiver_id = ? " +
                     "UNION " +
                     "SELECT DISTINCT receiver_id FROM message WHERE sender_id = ?";
        return jdbcTemplate.queryForList(sql, Long.class, userId, userId);
    }
}
