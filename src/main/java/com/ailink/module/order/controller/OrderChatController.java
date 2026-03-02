package com.ailink.module.order.controller;

import com.ailink.common.Result;
import com.ailink.module.order.dto.ChatMessageCreateRequest;
import com.ailink.module.order.service.ChatService;
import com.ailink.module.order.vo.ChatMessageVO;
import com.ailink.security.SecurityContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/order/{orderId}/chat")
public class OrderChatController {

    private final ChatService chatService;

    public OrderChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/messages")
    public Result<ChatMessageVO> sendMessage(@PathVariable Long orderId,
                                             @Valid @RequestBody ChatMessageCreateRequest request) {
        return Result.success(chatService.sendMessage(
                SecurityContextUtil.currentUserId(),
                orderId,
                request.getContent()));
    }

    @GetMapping("/messages")
    public Result<List<ChatMessageVO>> listMessages(@PathVariable Long orderId) {
        return Result.success(chatService.listMessages(SecurityContextUtil.currentUserId(), orderId));
    }
}
