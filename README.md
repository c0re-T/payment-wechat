# payment-wechat

微信支付 / 支付宝支付的练手项目，前后端放在同一个仓库。

> 学习演示项目。目前只跑通了商品列表一条链路，下单、退款、账单下载的后端接口还没写。

## 目录结构

```
payment-wechat/
├── payment-demo-back/    Spring Boot 后端
│   └── src/main/java/com/ittxf/paymentwechat/
│       ├── common/config/     CorsConfig 跨域配置
│       ├── common/result/     R<T> 统一响应体 + ResultCodeEnum 状态码枚举
│       ├── controller/        ProductController
│       ├── entity/ mapper/    MyBatis-Plus 实体与 Mapper
│       └── service/           业务层
└── payment-demo-front/   Vue 3 前端
    └── src/
        ├── api/          按模块拆分的请求方法
        ├── assets/       样式与图片
        ├── components/   页头 / 页脚
        ├── router/       vue-router 4
        ├── utils/        axios 实例与拦截器
        └── views/        购买页 / 订单页 / 账单页
```

## 技术栈

| 端 | 依赖 |
| --- | --- |
| 后端 | Java 21、Spring Boot 3.5.8、Spring Web、MyBatis-Plus 3.5.17、MySQL、knife4j（OpenAPI 3 注解）、Lombok |
| 前端 | Vue 3.5、Vite 8、vue-router 4、Element Plus 2.14、axios 1.x、qrcode.vue 3 |

## 本地运行

后端：

1. 建库 `payment_demo`，导入 `t_product` / `t_order_info` / `t_payment_info` / `t_refund_info` 四张表。建库脚本在本仓库之外（原课程资料的 `sql脚本/payment_demo.sql`），需要自行准备。
2. 改 `payment-demo-back/src/main/resources/application.yaml` 里的 `datasource` 账号密码，别用示例值。
3. 启动（需 JDK 21）：`cd payment-demo-back && mvn spring-boot:run`，监听 `8090`。

前端：

```bash
cd payment-demo-front
npm install
npm run dev      # http://localhost:5173
npm run build    # 产物在 dist/
```

前端直连 `http://localhost:8090`（见 `src/utils/request.js` 的 `baseURL`），后端 `CorsConfig` 对 `/**` 放开任意来源并允许携带凭证——这是本地调试用的宽松配置，上真实环境要收紧到具体域名。

## 接口返回约定

所有接口统一返回 `R<T>`：

```json
{ "code": 200, "message": "操作成功", "data": [] }
```

- `code` 取自 `ResultCodeEnum`：`SUCCESS(200)` / `PARAM_ERROR(400)` / `FAIL(500)`。
- `data` 直接放资源本身，不再包一层 `productList` 之类的字段；将来需要分页时改成 MyBatis-Plus 的 `IPage`。
- 前端 `src/utils/request.js` 的响应拦截器按 `code !== 200` 弹 Element Plus 错误提示，并把 `R` 对象交给调用方（组件里取 `response.data`）。

## 当前完成度

| 功能 | 前端页面 | 后端接口 |
| --- | --- | --- |
| 商品列表 | 已跑通 | `GET /api/product/list` |
| 统一下单（扫码） | 已迁移，未验证 | 未实现 |
| 订单列表 / 状态轮询 | 已迁移，未验证 | 未实现 |
| 取消订单 / 退款 | 已迁移，未验证 | 未实现 |
| 下载账单 | 已迁移，未验证 | 未实现 |

## 备注

前端最初是 2021 年课程源码的 Vue 2 + vue-cli + element-ui 脚手架，现已整体迁移到 Vue 3 + Vite + Element Plus，组件统一用 `<script setup>` 组合式写法。站点品牌（logo、favicon、页头页脚文案）已换成「支付实验室」，与原课程无关。
