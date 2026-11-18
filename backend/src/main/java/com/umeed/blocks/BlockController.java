package com.umeed.blocks;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/blocks")
class BlockController {

    private final BlockService blockService;

    BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @GetMapping
    List<BlockedUserResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return blockService.listMine(jwt.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void block(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BlockRequest request) {
        blockService.block(jwt.getSubject(), request.userId());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unblock(@AuthenticationPrincipal Jwt jwt, @PathVariable("userId") UUID userId) {
        blockService.unblock(jwt.getSubject(), userId);
    }
}
