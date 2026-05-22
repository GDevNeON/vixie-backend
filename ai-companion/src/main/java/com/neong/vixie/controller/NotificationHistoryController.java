package com.neong.vixie.controller;

import com.neong.vixie.dto.NotificationHistoryItem;
import com.neong.vixie.service.NotificationHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications/history")
@RequiredArgsConstructor
public class NotificationHistoryController {

    private final NotificationHistoryService notificationHistoryService;

    @GetMapping
    public ResponseEntity<List<NotificationHistoryItem>> getHistory(Principal principal) {
        return ResponseEntity.ok(notificationHistoryService.getHistory(principal.getName()));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Principal principal) {
        notificationHistoryService.markAllAsRead(principal.getName());
        return ResponseEntity.ok().build();
    }
}
