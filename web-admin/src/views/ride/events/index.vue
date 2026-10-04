<template>
  <el-card shadow="hover">
    <template #header>
      <div style="display: flex; align-items: center; gap: 12px">
        <span style="font-weight: 600">Agent 事件审计</span>
        <el-tag effect="plain" size="small">EventBus 全量消息 · 持久化于 agent_events 表</el-tag>
        <el-switch v-model="live" active-text="实时滚动" style="margin-left: auto" @change="toggleLive" />
        <el-button type="primary" plain size="small" @click="load(0)">刷新</el-button>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading" stripe size="small">
      <el-table-column prop="id" label="#" width="70" />
      <el-table-column label="主题" width="170">
        <template #default="{ row }">
          <b :style="{ color: topicColor(row.topic) }">{{ row.topic }}</b>
        </template>
      </el-table-column>
      <el-table-column prop="orderId" label="订单" width="110" />
      <el-table-column prop="payload" label="载荷" min-width="320" show-overflow-tooltip />
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ formatTs(row.ts) }}</template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 14px; justify-content: flex-end"
      background
      layout="prev, pager, next, total"
      :total="total"
      :page-size="pageSize"
      :current-page="page + 1"
      @current-change="(p: number) => load(p - 1)"
    />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from "vue";
import { getEvents } from "@/api/ride";

const rows = ref<any[]>([]);
const total = ref(0);
const page = ref(0);
const pageSize = 15;
const loading = ref(false);
const live = ref(false);
let timer: number | null = null;

const COLORS: Record<string, string> = {
  "order.created": "#4096ff", "risk.passed": "#52c41a", "risk.rejected": "#f5222d",
  "order.matching": "#faad14", "driver.matched": "#722ed1", "route.planned": "#1677ff",
  "price.quoted": "#d46b08", "order.paid": "#52c41a", "order.completed": "#52c41a",
  "order.started": "#1677ff", "order.cancelled": "#ff7a45"
};
const topicColor = (t: string) => COLORS[t] || "#909399";
const formatTs = (ts: number) => new Date(ts).toLocaleString("zh-CN", { hour12: false });

async function load(p = page.value) {
  page.value = p;
  loading.value = true;
  try {
    const res: any = await getEvents({ page: p, size: pageSize });
    rows.value = res?.content ?? [];
    total.value = res?.totalElements ?? 0;
  } finally {
    loading.value = false;
  }
}

function toggleLive(v: boolean) {
  if (timer) { clearInterval(timer); timer = null; }
  if (v) timer = window.setInterval(() => load(0), 2000);
}

onMounted(() => load(0));
onBeforeUnmount(() => { if (timer) clearInterval(timer); });
</script>
