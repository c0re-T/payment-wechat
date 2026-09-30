package com.ittxf.paymentwechat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ittxf.paymentwechat.common.enums.OrderStatus;
import com.ittxf.paymentwechat.common.util.OrderNoUtils;
import com.ittxf.paymentwechat.entity.OrderInfo;
import com.ittxf.paymentwechat.entity.Product;
import com.ittxf.paymentwechat.mapper.OrderInfoMapper;
import com.ittxf.paymentwechat.mapper.ProductMapper;
import com.ittxf.paymentwechat.service.OrderInfoService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
     * @return 已入库的订单对象，含生成的 orderNo
     */
    @Override
    public OrderInfo createOrderByProductId(Long productId) {

        // 查找已存在但未支付的订单
        OrderInfo orderInfo = this.getNoPayOrderByProductId(productId);
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
     * 根据商品 ID 获取未支付的订单
     * 防止重复下单
     * @param productId
     * @return
     */
    private OrderInfo getNoPayOrderByProductId(Long productId) {
        LambdaQueryWrapper<OrderInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(OrderInfo::getProductId, productId)
                .eq(OrderInfo::getOrderStatus, OrderStatus.NOTPAY.getType());
                // .eq(OrderInfo::getUserId, userId); // 实际开发中可能需要用户 ID 进行更细粒度的控制
        return baseMapper.selectOne(lambdaQueryWrapper);
    }
}
