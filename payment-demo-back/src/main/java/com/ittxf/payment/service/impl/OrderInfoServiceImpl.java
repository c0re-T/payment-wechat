package com.ittxf.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ittxf.payment.common.enums.OrderStatus;
import com.ittxf.payment.common.util.OrderNoUtils;
import com.ittxf.payment.entity.OrderInfo;
import com.ittxf.payment.entity.Product;
import com.ittxf.payment.mapper.OrderInfoMapper;
import com.ittxf.payment.mapper.ProductMapper;
import com.ittxf.payment.service.OrderInfoService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 订单业务实现。
 *
 * <p>继承 MyBatis-Plus 的 {@code ServiceImpl}，直接获得单表 CRUD 能力，
 * 本类只保留需要结合商品表加工的建单逻辑。</p>
 *
 * @author txf
 * @since 2026-09-28
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    /**
     * 商品 Mapper，建单时需要用它查出商品名称与价格
     */
    private final ProductMapper productMapper;
    /**
     * 订单 Mapper，虽然继承了 ServiceImpl 已经有 baseMapper
     */
    // private final OrderInfoMapper orderInfoMapper;

    /**
     * 根据商品 ID 创建订单并落库
     *
     * <p>金额、标题均取自商品表，避开由前端传入被篡改的风险；
     * 初始状态固定为未支付，待微信回调或查单后再更新。</p>
     *
     * @param productId 商品 ID
     * @param type
     * @return 已入库的订单对象，含生成的 orderNo
     */
    @Override
    public OrderInfo createOrderByProductId(Long productId, String type) {

        // 查找已存在但未支付的订单（限同一支付渠道，避免微信建的单被支付宝流程复用、串了 codeUrl）
        OrderInfo orderInfo = this.getNoPayOrderByProductId(productId, type);
        // 如果已存在未支付的订单，则返回该订单直接截断方法进行后续操作
        if (orderInfo != null) return orderInfo;

        // 获取商品信息
        Product product = productMapper.selectById(productId);

        // 生成订单
        orderInfo = new OrderInfo();
        orderInfo.setProductId(productId);
        orderInfo.setTitle(product.getTitle());
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo()); // 生成订单号，即微信的 out_trade_no
        orderInfo.setTotalFee(product.getPrice()); // 设置订单金额（单位：分）
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType()); // 设置订单状态为未支付
        orderInfo.setPaymentType(type); // 设置支付类型
        baseMapper.insert(orderInfo);

        // 返回已入库的订单对象
        return orderInfo;
    }

    /**
     * 保存订单二维码地址
     * @param orderNo
     * @param codeUrl
     */
    @Override
    public void saveCodeUrl(String orderNo, String codeUrl) {
        LambdaQueryWrapper<OrderInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCodeUrl(codeUrl);
        baseMapper.update(orderInfo, lambdaQueryWrapper);
        log.info("保存订单二维码地址：orderNo={}, codeUrl={}", orderNo, codeUrl);
    }

    /**
     * 根据创建时间降序排列，获取订单列表
     * @return
     */
    @Override
    public List<OrderInfo> listOrderByCreateTimeDesc() {
        /*LambdaQueryWrapper<OrderInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.orderByDesc(OrderInfo::getCreateTime);
        return baseMapper.selectList(lambdaQueryWrapper);*/

        // this 指的就是 OrderInfoServiceImpl 这个对象实例。而这个类继承（extends）了 ServiceImpl。
        // 所以，this 不仅拥有你自己写的方法，还继承了一堆父类的方法，其中就包括 lambdaQuery()
        // this.lambdaQuery()：调用父类方法，返回一个 LambdaQueryChainWrapper<OrderInfo> 对象。
        // .orderByDesc(OrderInfo::getCreateTime)：这是 LambdaQueryChainWrapper 对象的方法。它把排序条件加进去后，返回自己（return this）。
        // .list()：这是 LambdaQueryChainWrapper 对象的终结方法。它的底层相当于执行了 baseMapper.selectList(wrapper)，执行 SQL，返回 List<OrderInfo> 结果。
        return this.lambdaQuery()
                .orderByDesc(OrderInfo::getCreateTime)
                .list();
    }

    /**
     * 根据订单号更新订单状态
     * @param outTradeNo
     * @param orderStatus
     */
    @Override
    public void updateStatusByOrderNo(String outTradeNo, OrderStatus orderStatus) {
        log.info("更新订单状态：outTradeNo={}, orderStatus={}", outTradeNo, orderStatus.getType());

        this.lambdaUpdate().eq(OrderInfo::getOrderNo, outTradeNo)
                .set(OrderInfo::getOrderStatus, orderStatus.getType())
                .update();
    }

    /**
     * 根据订单号获取订单
     * @param orderNo
     * @return
     */
    @Override
    public OrderInfo getOrderByOrderNo(String orderNo) {
        return this.lambdaQuery()
                .eq(OrderInfo::getOrderNo, orderNo)
                .one();
    }

    /**
     * 根据订单创建时间超过 i 分钟获取未支付订单列表
     * @param i
     * @return
     */
    @Override
    public List<OrderInfo> getNoPayOrderByDuration(int i, String paymentType) {
        log.info("根据订单创建时间获取未支付订单列表：i={}, paymentType={}", i, paymentType);

        Instant minus = Instant.now().minus(Duration.ofMinutes(i));

        return this.lambdaQuery()
                .eq(OrderInfo::getOrderStatus, OrderStatus.NOTPAY.getType())
                .eq(OrderInfo::getPaymentType, paymentType)
                .le(OrderInfo::getCreateTime, minus)
                .list();
    }

    /**
     * 根据订单号获取订单状态
     * @param outTradeNo
     * @return
     */
    @Override
    public String getOrderStatus(String outTradeNo) {
        OrderInfo orderInfo = this.lambdaQuery()
                .eq(OrderInfo::getOrderNo, outTradeNo)
                .one();
        if (orderInfo == null) {
            return null;
        }
        return orderInfo.getOrderStatus();
    }

    /**
     * 根据商品 ID 与支付渠道获取未支付的订单
     * 防止重复下单，同时按渠道隔离：微信单不会被支付宝流程复用，反之亦然
     * @param productId 商品 ID
     * @param type 支付渠道（PayType.getType()）
     * @return 未支付订单，无则 null
     */
    private OrderInfo getNoPayOrderByProductId(Long productId, String type) {
        LambdaQueryWrapper<OrderInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(OrderInfo::getProductId, productId)
                .eq(OrderInfo::getOrderStatus, OrderStatus.NOTPAY.getType())
                .eq(OrderInfo::getPaymentType, type);
                // .eq(OrderInfo::getUserId, userId); // 实际开发中可能需要用户 ID 进行更细粒度的控制
        return baseMapper.selectOne(lambdaQueryWrapper);
    }
}
