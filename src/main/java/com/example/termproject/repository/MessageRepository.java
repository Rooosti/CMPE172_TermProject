package com.example.termproject.repository;

import com.example.termproject.model.ChatMessage;
import java.util.List;

public interface MessageRepository {
    ChatMessage save(ChatMessage message);
    List<ChatMessage> findChatHistory(Long userA, Long userB, Long equipmentId);
    List<Long> findInteractedUserIds(Long userId);
}
