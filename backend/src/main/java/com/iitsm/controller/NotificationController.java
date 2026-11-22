package com.iitsm.controller;

import com.iitsm.dto.response.NotificationResponse;
import com.iitsm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(Authentication auth) {
        return ResponseEntity.ok(notificationService.getNotificationsForUser(auth.getName()));
    }

    @GetMapping("/undelivered")
    public ResponseEntity<List<NotificationResponse>> getUndelivered() {
        return ResponseEntity.ok(notificationService.getUndelivered());
    }
}
