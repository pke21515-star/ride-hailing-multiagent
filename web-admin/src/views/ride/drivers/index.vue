<template>
  <el-card shadow="hover">
    <template #header>
      <div style="display: flex; align-items: center">
        <span style="font-weight: 600">司机管理</span>
        <span style="margin-left: auto; color: #909399; font-size: 12px">
          匹配Agent 打分维度：距离 45% · 服务分 25% · 接单率 20% · 均衡 10%
        </span>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="工号" width="90" />
      <el-table-column prop="name" label="司机" width="120" />
      <el-table-column label="服务分" width="200">
        <template #default="{ row }">
          <el-progress :percentage="(row.rating / 5) * 100" :stroke-width="10" :show-text="false" />
          <span style="font-size: 12px; color: #606266">{{ row.rating }} / 5.0</span>
        </template>
      </el-table-column>
      <el-table-column label="接单率" width="100">
        <template #default="{ row }">{{ (row.acceptRate * 100).toFixed(0) }}%</template>
      </el-table-column>
      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <el-tag :type="row.idle ? 'success' : 'warning'" size="small">
            {{ row.idle ? "空闲" : "服务中" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="todayOrders" label="今日订单" width="100" />
      <el-table-column label="司机端账号" min-width="160">
        <template #default="{ row }">
          <el-tag effect="plain" size="small">{{ row.id }}</el-tag>
          <span style="color: #909399; font-size: 12px; margin-left: 6px">初始密码 123456</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { getDrivers } from "@/api/ride";

const rows = ref<any[]>([]);
const loading = ref(false);

onMounted(async () => {
  loading.value = true;
  try {
    rows.value = (await getDrivers()) ?? [];
  } finally {
    loading.value = false;
  }
});
</script>
