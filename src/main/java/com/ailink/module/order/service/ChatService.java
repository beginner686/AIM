package com.ailink.module.order.service;

import com.ailink.module.order.vo.ChatMessageVO;

import java.util.List;

public interface ChatService {

    ChatMessageVO sendMessage(Long userId, Long orderId, String content);

    List<ChatMessageVO> listMessages(Long userId, Long orderId);
}
