<template>
  <div class="panel-header">
    <div>
      <h2>{{ title }}</h2>
      <p>{{ subtitle }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    required: true
  }
})
</script>

<style scoped>
.panel-header {
  padding: 16px 18px;
  border-bottom: 1px solid #e4eaf3;
  background: #ffffff;
}

.panel-header h2 {
  margin: 0;
  color: #172033;
  font-size: 17px;
  letter-spacing: 0;
}

.panel-header p {
  margin: 5px 0 0;
  color: #7b8798;
  font-size: 12px;
}
</style>
