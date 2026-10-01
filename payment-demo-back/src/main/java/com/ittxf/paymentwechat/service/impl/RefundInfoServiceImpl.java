package com.ittxf.paymentwechat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ittxf.paymentwechat.common.util.OrderNoUtils;
import com.ittxf.paymentwechat.entity.OrderInfo;
import com.ittxf.paymentwechat.entity.RefundInfo;
import com.ittxf.paymentwechat.mapper.RefundInfoMapper;
import com.ittxf.paymentwechat.service.OrderInfoService;
import com.ittxf.paymentwechat.service.RefundInfoService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo> implements RefundInfoService {

    private final OrderInfoService orderInfoService;
    private final ObjectMapper objectMapper;

    /**
     * 根据退款订单号更新退款信息
     * @param bodyAsString
     */
    @Override
    public void updateRefund(String bodyAsString) {
        try {
            // 解析 JSON 字符串为 Map
            Map<String, String> hashMap = objectMapper.readValue(bodyAsString, HashMap.class);

            // 设置要修改的字段
            RefundInfo refundInfo = new RefundInfo();
            refundInfo.setRefundId(hashMap.get("refund_id")); // 微信支付退款单号

            // 查询退款和申请退款中的返回参数
            if (hashMap.get("status") != null) {
                refundInfo.setRefundStatus(hashMap.get("status")); // 退款状态
                refundInfo.setContentReturn(bodyAsString);
            }

            // 退款回调中的回调参数
            if (hashMap.get("refund_status") != null) {
                refundInfo.setRefundStatus(hashMap.get("refund_status")); // 退款状态
                refundInfo.setContentNotify(bodyAsString);
            }

            // 将全部响应结果存入数据库的content字段
            refundInfo.setContentNotify(bodyAsString);

            // 更新退款单：按退款单编号作为条件，一次条件更新完成“定位 + 修改”
            this.lambdaUpdate()
                    .eq(RefundInfo::getRefundNo, hashMap.get("out_refund_no"))
                    .update(refundInfo);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据订单编号和退款原因创建退款单
     * @param orderNo
     * @param reason
     * @return
     */
    @Override
    public RefundInfo createRefundByOrderNo(String orderNo, String reason) {
        // 根据订单编号创建退款单
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);

        // 根据订单号生成退款订单
        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setOrderNo(orderNo);
        refundInfo.setRefundNo(OrderNoUtils.getRefundNo());
        refundInfo.setTotalFee(orderInfo.getTotalFee());
        refundInfo.setRefund(orderInfo.getTotalFee());
        refundInfo.setReason(reason);

        // 保存退款单
        this.save(refundInfo);
        return refundInfo;
    }
}
