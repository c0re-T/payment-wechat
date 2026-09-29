package com.ittxf.paymentwechat.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举，维护本地库 t_order_info.order_status 字段的取值。
 *
 * <p>覆盖支付与退款的完整生命周期，前端订单列表根据状态文本渲染不同颜色的标签。</p>
 *
 * @author txf
 * @since 2026-09-28
 */
@AllArgsConstructor
@Getter
public enum OrderStatus {
    /**
     * 未支付
     */
    NOTPAY("未支付"),


    /**
     * 支付成功
     */
    SUCCESS("支付成功"),

    /**
     * 已关闭
     */
    CLOSED("超时已关闭"),

    /**
     * 已取消
     */
    CANCEL("用户已取消"),

    /**
     * 退款中
     */
    REFUND_PROCESSING("退款中"),

    /**
     * 已退款
     */
    REFUND_SUCCESS("已退款"),

    /**
     * 退款异常
     */
    REFUND_ABNORMAL("退款异常");

    /**
     * 状态文本，直接存入数据库并回传给前端展示
     */
    private final String type;
}
