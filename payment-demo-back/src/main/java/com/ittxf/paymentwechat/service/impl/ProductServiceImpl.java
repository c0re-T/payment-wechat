package com.ittxf.paymentwechat.service.impl;

import com.ittxf.paymentwechat.entity.Product;
import com.ittxf.paymentwechat.mapper.ProductMapper;
import com.ittxf.paymentwechat.service.ProductService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

}
