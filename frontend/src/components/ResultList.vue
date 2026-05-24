<template>
  <section class="result-list" :class="`result-list--${type}`">
    <div v-if="items.length === 0" class="empty-line">暂无内容</div>
    <ul v-else>
      <li v-for="item in items" :key="item">
        <span></span>
        <p>{{ item }}</p>
      </li>
    </ul>
  </section>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
  },
  type: {
    type: String,
    default: 'primary'
  }
})
</script>

<style scoped>
ul {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

li {
  display: grid;
  grid-template-columns: 8px 1fr;
  gap: 10px;
  align-items: start;
  padding: 12px;
  border: 1px solid #dfe7f2;
  border-radius: 10px;
  background: #ffffff;
}

li span {
  width: 7px;
  height: 7px;
  margin-top: 8px;
  border-radius: 999px;
  background: #409eff;
}

li p {
  margin: 0;
  color: #344258;
  font-size: 13px;
  line-height: 1.75;
}

.result-list--success li span {
  background: #67c23a;
}

.result-list--warning li span {
  background: #e6a23c;
}

.result-list--info li span {
  background: #8a95a8;
}

.empty-line {
  padding: 14px;
  border: 1px dashed #d5deea;
  border-radius: 10px;
  color: #8792a3;
  text-align: center;
}
</style>
