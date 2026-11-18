package com.umeed.chat;

import com.umeed.claims.ClaimParties;
import com.umeed.claims.ClaimService;
import com.umeed.users.UserResponse;
import com.umeed.users.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Private messages between a donor and the receiver whose claim was accepted. A conversation belongs to
 * one accepted claim. It can be written while the pickup is in progress and read afterwards.
 */
@Service
public class ChatService {

    private static final String UNKNOWN_NAME = "Umeed user";

    private final ChatMessageRepository chatMessageRepository;
    private final ClaimService claimService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    ChatService(
            ChatMessageRepository chatMessageRepository,
            ClaimService claimService,
            UserService userService,
            ApplicationEventPublisher eventPublisher) {
        this.chatMessageRepository = chatMessageRepository;
        this.claimService = claimService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * The caller's conversations: every accepted claim they are part of, and any earlier one that has
     * messages. Most recent activity first; conversations with no messages yet come first of all.
     */
    @Transactional(readOnly = true)
    public List<ChatResponses.Conversation> listMine(String firebaseUid) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        Set<UUID> involved = claimService.claimIdsInvolving(caller.id());
        if (involved.isEmpty()) {
            return List.of();
        }
        Set<UUID> withMessages = chatMessageRepository.findClaimIdsWithMessages(involved);
        Map<UUID, ClaimParties> parties = claimService.partiesOf(involved);

        List<ClaimParties> conversations = new ArrayList<>();
        Set<UUID> otherIds = new HashSet<>();
        for (ClaimParties claim : parties.values()) {
            if ("ACCEPTED".equals(claim.claimStatus()) || withMessages.contains(claim.claimId())) {
                conversations.add(claim);
                otherIds.add(claim.otherParty(caller.id()));
            }
        }
        Map<UUID, String> names = userService.displayNamesByIds(otherIds);

        List<ChatResponses.Conversation> result = new ArrayList<>();
        for (ClaimParties claim : conversations) {
            ChatMessage last = chatMessageRepository
                    .findFirstByClaimIdOrderByCreatedAtDescIdDesc(claim.claimId())
                    .orElse(null);
            result.add(new ChatResponses.Conversation(
                    claim.claimId(),
                    claim.donationId(),
                    claim.donationTitle(),
                    names.getOrDefault(claim.otherParty(caller.id()), UNKNOWN_NAME),
                    claim.pickupInProgress(),
                    last == null ? null : last.getBody(),
                    last != null && last.getSenderId().equals(caller.id()),
                    last == null ? null : last.getCreatedAt(),
                    chatMessageRepository.countUnread(claim.claimId(), caller.id())));
        }
        result.sort(Comparator.comparing(
                ChatResponses.Conversation::lastMessageAt,
                Comparator.nullsFirst(Comparator.<Instant>reverseOrder())));
        return result;
    }

    /**
     * One conversation. With {@code afterId}, only the messages after that one, so the app can check
     * often without downloading everything again.
     */
    @Transactional(readOnly = true)
    public ChatResponses.Thread thread(String firebaseUid, UUID claimId, UUID afterId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        ClaimParties claim = requireReadable(caller.id(), claimId);
        List<ChatMessage> messages;
        if (afterId == null) {
            messages = chatMessageRepository.findByClaimIdOrderByCreatedAtAscIdAsc(claimId);
        } else {
            ChatMessage after = chatMessageRepository.findById(afterId)
                    .filter(found -> found.getClaimId().equals(claimId))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Unknown message in this conversation"));
            messages = chatMessageRepository.findAfter(claimId, after.getCreatedAt(), after.getId());
        }
        return new ChatResponses.Thread(
                claimId,
                claim.donationId(),
                claim.donationTitle(),
                nameOf(claim.otherParty(caller.id())),
                claim.pickupInProgress(),
                messages.stream().map(message -> ChatResponses.Message.from(message, caller.id())).toList());
    }

    /**
     * Sends a message. Only the donor and the accepted receiver, and only while the pickup is in progress.
     */
    @Transactional
    public ChatResponses.Message send(String firebaseUid, UUID claimId, String body) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        ClaimParties claim = requireReadable(caller.id(), claimId);
        if (!claim.pickupInProgress()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This chat is closed because the pickup is finished or cancelled");
        }
        ChatMessage message = chatMessageRepository.saveAndFlush(ChatMessage.of(claimId, caller.id(), body));
        eventPublisher.publishEvent(new ChatMessageSentEvent(
                claimId,
                claim.donationId(),
                claim.donationTitle(),
                caller.id(),
                caller.displayName(),
                claim.otherParty(caller.id())));
        return ChatResponses.Message.from(message, caller.id());
    }

    /**
     * Marks the other person's messages in this conversation as read by the caller.
     */
    @Transactional
    public void markRead(String firebaseUid, UUID claimId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        requireReadable(caller.id(), claimId);
        chatMessageRepository.markRead(claimId, caller.id(), Instant.now());
    }

    /**
     * For account deletion: removes every message the user wrote.
     */
    @Transactional
    public void removeMessagesBy(UUID userId) {
        chatMessageRepository.deleteBySenderId(userId);
    }

    /**
     * Only the two people in the handover may see a conversation, and only one that exists: the claim
     * is accepted now, or messages were written while it was.
     */
    private ClaimParties requireReadable(UUID callerId, UUID claimId) {
        ClaimParties claim = claimService.partiesOf(List.of(claimId)).get(claimId);
        if (claim == null) {
            throw chatNotFound();
        }
        if (!claim.involves(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the donor and the receiver can see this chat");
        }
        if (!"ACCEPTED".equals(claim.claimStatus()) && !chatMessageRepository.existsByClaimId(claimId)) {
            throw chatNotFound();
        }
        return claim;
    }

    private String nameOf(UUID userId) {
        return userService.displayNamesByIds(List.of(userId)).getOrDefault(userId, UNKNOWN_NAME);
    }

    private static ResponseStatusException chatNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found");
    }
}
