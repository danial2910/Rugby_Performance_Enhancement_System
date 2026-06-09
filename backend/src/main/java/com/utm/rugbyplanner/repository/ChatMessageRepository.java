package com.utm.rugbyplanner.repository;

import com.utm.rugbyplanner.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /** Full conversation history for a user, oldest first (for display) */
    List<ChatMessage> findByUserIdOrderByCreatedAtAsc(String userId);

    /**
     * Most recent N turns for a user, newest first — used to build
     * conversational context sent back to the AI model.
     */
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /** Remove the whole conversation (UC011 — clear chat) */
    void deleteByUserId(String userId);
}
