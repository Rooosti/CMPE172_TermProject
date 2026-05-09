package com.example.termproject.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private Long messageId;
    private Long senderId;
    private Long receiverId;
    private Long equipmentId;
    private String content;
    private LocalDateTime timestamp;
    
    // For UI display
    private String senderUsername;
}
