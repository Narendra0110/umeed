package com.umeed.chat;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
class ChatController {

    private final ChatService chatService;

    ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/api/v1/chats")
    List<ChatResponses.Conversation> list(@AuthenticationPrincipal Jwt jwt) {
        return chatService.listMine(jwt.getSubject());
    }

    @GetMapping("/api/v1/claims/{claimId}/messages")
    ChatResponses.Thread thread(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("claimId") UUID claimId,
            @RequestParam(name = "after", required = false) UUID after) {
        return chatService.thread(jwt.getSubject(), claimId, after);
    }

    @PostMapping("/api/v1/claims/{claimId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    ChatResponses.Message send(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("claimId") UUID claimId,
            @Valid @RequestBody SendMessageRequest request) {
        return chatService.send(jwt.getSubject(), claimId, request.body());
    }

    @PostMapping("/api/v1/claims/{claimId}/messages/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable("claimId") UUID claimId) {
        chatService.markRead(jwt.getSubject(), claimId);
    }
}
