package com.ailink.module.order.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.enums.OrderStatus;
import com.ailink.common.exception.BizException;
import com.ailink.module.order.entity.ChatMessage;
import com.ailink.module.order.entity.Order;
import com.ailink.module.order.mapper.ChatMessageMapper;
import com.ailink.module.order.mapper.OrderMapper;
import com.ailink.module.order.service.ChatService;
import com.ailink.module.order.vo.ChatMessageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Pattern WECHAT_PATTERN = Pattern.compile("(?i)(微信|wx|weixin|v信)[\\s:：-]*[a-z0-9_-]{5,}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(?:\\+?\\d[\\d\\s-]{6,}\\d)(?!\\d)");
    private static final String WARNING_MESSAGE = "为保障权益，请在平台内完成交易";

    private final ChatMessageMapper chatMessageMapper;
    private final OrderMapper orderMapper;

    public ChatServiceImpl(ChatMessageMapper chatMessageMapper, OrderMapper orderMapper) {
        this.chatMessageMapper = chatMessageMapper;
        this.orderMapper = orderMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendMessage(Long userId, Long orderId, String content) {
        Order order = findAndCheckParticipant(userId, orderId);
        OrderStatus status = parseStatus(order.getStatus());
        if (!status.canChat()) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "chat is available only after match unlocked");
        }

        boolean warning = containsRiskText(content);
        ChatMessage message = new ChatMessage();
        message.setOrderId(orderId);
        message.setSenderId(userId);
        message.setContent(content);
        message.setWarning(warning ? 1 : 0);
        message.setWarningMessage(warning ? WARNING_MESSAGE : null);
        chatMessageMapper.insert(message);
        return toVO(message);
    }

    @Override
    public List<ChatMessageVO> listMessages(Long userId, Long orderId) {
        findAndCheckParticipant(userId, orderId);
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getOrderId, orderId)
                        .orderByAsc(ChatMessage::getCreatedTime))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private boolean containsRiskText(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        return WECHAT_PATTERN.matcher(content).find()
                || EMAIL_PATTERN.matcher(content).find()
                || PHONE_PATTERN.matcher(content).find();
    }

    private Order findAndCheckParticipant(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "order not found");
        }
        if (!userId.equals(order.getEmployerId()) && !userId.equals(order.getWorkerUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only order participants can access chat");
        }
        return order;
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status);
        } catch (Exception e) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "unknown order status: " + status);
        }
    }

    private ChatMessageVO toVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setOrderId(message.getOrderId());
        vo.setSenderId(message.getSenderId());
        vo.setContent(message.getContent());
        vo.setWarning(message.getWarning() != null && message.getWarning() == 1);
        vo.setWarningMessage(message.getWarningMessage());
        vo.setCreatedTime(message.getCreatedTime());
        return vo;
    }
}
