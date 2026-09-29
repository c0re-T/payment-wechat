<template>
  <!-- 公共底 -->
  <footer id="footer">
    <section class="container">
      <div class="f-top">
        <!-- 品牌介绍 -->
        <div class="f-brand">
          <h4 class="f-name">
            支付实验室
            <span>PAYMENT LAB</span>
          </h4>
          <p class="f-slogan">
            一个面向开发者的支付链路全栈学习项目，覆盖下单、签名、扫码支付、
            异步通知、订单查询、关单、退款与账单下载，帮助开发者用最短路径理解
            第三方支付的完整闭环。
          </p>
          <ul class="f-badges">
            <li>微信支付 APIv3</li>
            <li>支付宝开放平台</li>
            <li>Spring Boot 3</li>
            <li>Vue 3</li>
          </ul>
        </div>

        <!-- 功能导航 -->
        <div class="f-col">
          <h5 class="f-col-title">功能导航</h5>
          <ul class="f-col-list">
            <li><router-link to="/">购买课程</router-link></li>
            <li><router-link to="/orders">我的订单</router-link></li>
            <li><router-link to="/download">下载账单</router-link></li>
          </ul>
        </div>

        <!-- 开发者入口 -->
        <div class="f-col">
          <h5 class="f-col-title">开发者</h5>
          <ul class="f-col-list">
            <li><a href="https://pay.weixin.qq.com" title="微信支付商户平台" target="_blank">微信支付商户平台</a></li>
            <li><a href="https://pay.weixin.qq.com/doc/v3/merchant/4012062580" title="APIv3 开发指南" target="_blank">APIv3 开发指南</a></li>
            <li><a href="https://open.weixin.qq.com" title="微信开放平台" target="_blank">微信开放平台</a></li>
            <li><a href="https://open.alipay.com" title="支付宝开放平台" target="_blank">支付宝开放平台</a></li>
          </ul>
        </div>

        <!-- 联系方式 -->
        <div class="f-col">
          <h5 class="f-col-title">联系我们</h5>
          <ul class="f-contact">
            <li><span>客服热线</span><em>400-888-6666（示例）</em></li>
            <li><span>合作邮箱</span><em>hello@payment-lab.dev（示例）</em></li>
            <li><span>工作时间</span><em>周一至周五 09:00 - 18:00</em></li>
            <li><span>公司地址</span><em>北京市海淀区学院路 32 号 · 创新大厦 B 座 8 层（虚构）</em></li>
          </ul>
        </div>
      </div>

      <!-- 友情链接 -->
      <div class="f-links">
        <span class="f-links-label">友情链接</span>
        <ul class="flink-list">
          <li><a href="https://pay.weixin.qq.com" title="微信支付商户平台" target="_blank">微信支付商户平台</a></li>
          <li><a href="https://mp.weixin.qq.com" title="微信公众平台" target="_blank">微信公众平台</a></li>
          <li><a href="https://developers.weixin.qq.com" title="微信开放社区" target="_blank">微信开放社区</a></li>
          <li><a href="https://opendocs.alipay.com" title="支付宝开放文档" target="_blank">支付宝开放文档</a></li>
          <li><a href="https://spring.io/projects/spring-boot" title="Spring Boot" target="_blank">Spring Boot</a></li>
          <li><a href="https://cn.vuejs.org" title="Vue.js" target="_blank">Vue.js</a></li>
        </ul>
      </div>

      <!-- 版权与免责 -->
      <div class="b-foot">
        <section class="b-f-link">
          <a href="#" title="关于我们" target="_blank">关于我们</a>|
          <a href="#" title="联系我们" target="_blank">联系我们</a>|
          <a href="#" title="帮助中心" target="_blank">帮助中心</a>|
          <a href="#" title="开发者文档" target="_blank">开发者文档</a>|
          <a href="#" title="资源下载" target="_blank">资源下载</a>
        </section>
        <p class="f-copy">©2026 支付实验室 PAYMENT LAB · 保留所有权利 · 京ICP备00000000号-1（示例）</p>
        <p class="f-disclaim">
          声明：本站为个人学习与技术演示项目，不对外提供任何付费服务。页面中出现的机构名称、客服热线、
          邮箱、公司地址、备案号及友情链接均为虚构示例，与任何真实企业或组织无关，仅作界面排版演示之用。
        </p>
        <div class="clear"/>
      </div>
    </section>
  </footer>
</template>
