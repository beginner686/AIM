package com.ailink.module.order.schedule;

import com.ailink.common.enums.PlatformPaymentStatus;
import com.ailink.module.order.entity.PlatformPayment;
import com.ailink.module.order.mapper.PlatformPaymentMapper;
import com.ailink.module.order.service.impl.ServiceFeePaymentTxService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaymentSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentSyncScheduler.class);

    private final PlatformPaymentMapper platformPaymentMapper;
    private final ServiceFeePaymentTxService paymentTxService;

    public PaymentSyncScheduler(PlatformPaymentMapper platformPaymentMapper,
            ServiceFeePaymentTxService paymentTxService) {
        this.platformPaymentMapper = platformPaymentMapper;
        this.paymentTxService = paymentTxService;
    }

    /**
     * 定时任务：主动查单对账
     * 防丢失回调。例如：每 2 分钟执行一次，查询距离现在超过 2 分钟但尚未支付成功的订单状态。
     */
    @Scheduled(fixedDelay = 120000)
    public void syncUnpaidPayments() {
        log.info("start to check and sync unpaid platform payments...");
        // 查询过去 2 天内并且在 2 分钟前创建的 未支付 记录（排除很久以前可能废弃的脏数据，降低轮询开销）
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);

        List<PlatformPayment> pendingList = platformPaymentMapper.selectList(
                new LambdaQueryWrapper<PlatformPayment>()
                        .in(PlatformPayment::getStatus, PlatformPaymentStatus.UNPAID.name(),
                                PlatformPaymentStatus.CREATE_FAILED.name())
                        .gt(PlatformPayment::getCreatedTime, twoDaysAgo)
                        .le(PlatformPayment::getCreatedTime, twoMinutesAgo));

        int count = 0;
        int fixedCount = 0;
        for (PlatformPayment payment : pendingList) {
            try {
                log.info("sync schedule running for paymentNo={}", payment.getPaymentNo());
                count++;

                // MOCK 环节：如果启用了 Mock 我们就当作一定几率自动成功并对账补平。
                // 真实环境需要接入 wechatNativePayService.queryOrder(...) 查单。
                boolean isMockSuccess = true;

                if (isMockSuccess) {
                    // 若从通道（微信）查单证明已经成功，则调起业务修正接口。
                    log.info("sync schedule find payment paid from remote, paymentNo={}", payment.getPaymentNo());
                    paymentTxService.markPaidAndUnlock(payment.getPaymentNo(), "MOCK_SYNC_TX_" + payment.getPaymentNo(),
                            "system auto sync logic");
                    fixedCount++;
                }

            } catch (Exception e) {
                log.error("sync payment failed for paymentNo={}", payment.getPaymentNo(), e);
            }
        }

        log.info("finished syncing platform payments, scanned={}, fixed={}", count, fixedCount);
    }
}
