<template>
  <!-- 公共头 -->
  <header id="header">
    <section class="container">
      <h1 id="logo">
        <a href="/" title="支付实验室">
          <img src="../assets/img/logo.png" width="100%" alt="支付实验室">
        </a>
      </h1>
      <div>
        <ul class="nav">
          <!-- vue-router 4 移除了 router-link 的 tag 属性，改用 custom 自定义渲染 -->
          <router-link to="/" custom v-slot="{ href, navigate, isExactActive }">
            <li :class="{ current: isExactActive }"><a :href="href" @click="navigate">购买课程</a></li>
          </router-link>
          <router-link to="/orders" custom v-slot="{ href, navigate, isActive }">
            <li :class="{ current: isActive }"><a :href="href" @click="navigate">我的订单</a></li>
          </router-link>
          <router-link to="/download" custom v-slot="{ href, navigate, isActive }">
            <li :class="{ current: isActive }"><a :href="href" @click="navigate">下载账单</a></li>
          </router-link>
        </ul>
      </div>
      <div class="clear"></div>
    </section>
  </header>
</template>