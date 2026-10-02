package com.ittxf.payment.service.impl;

import com.ittxf.payment.entity.Product;
import com.ittxf.payment.mapper.ProductMapper;
import com.ittxf.payment.service.ProductService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

}
