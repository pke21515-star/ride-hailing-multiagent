<template>
  <div>
    <!-- KPI 卡片 -->
    <el-row :gutter="16">
      <el-col v-for="k in kpiCards" :key="k.label" :xs="12" :sm="8" :md="6" :lg="3" style="margin-bottom: 16px">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-value">{{ k.value }}</div>
          <div class="kpi-label">{{ k.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 订单状态分布 -->
      <el-col :xs="24" :md="14">
        <el-card shadow="hover">
          <template #header>
            <span style="font-weight: 600">订单状态分布</span>
          </template>
          <div ref="barRef" style="height: 320px" />
        </el-card>
      </el-col>
      <!-- 系统概况 -->
      <el-col :xs="24" :md="10">
        <el-card shadow="hover">
          <template #header>
            <span style="font-weight: 600">调度系统概况</span>
            <el-tag style="float: right" type="success" effect="plain" size="small">
              <span class="dot" /> Agent 在线
            </el-tag>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="7-Agent Mesh">EventBus 事件驱动 · 主题订阅发布</el-descriptions-item>
            <el-descriptions-item label="EventBus 累计消息">
              <b style="color: #722ed1">{{ stats.eventBusMsg ?? 0 }}</b>
            </el-descriptions-item>
            <el-descriptions-item label="司机池">
              {{ stats.drivers ?? 0 }} 人 · 空闲 {{ stats.idleDrivers ?? 0 }} 人
            </el-descriptions-item>
            <el-descriptions-item label="平台用户">{{ stats.users ?? 0 }}（乘客自助注册）</el-descriptions-item>
            <el-descriptions-item label="持久化">H2 文件库 · 重启不丢（生产平替 MySQL）</el-descriptions-item>
            <el-descriptions-item label="API 文档">
              <el-link type="primary" href="/swagger-ui.html" target="_blank">Swagger / OpenAPI</el-link>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from "vue";
import * as echarts from "echarts";
import { getStats } from "@/api/ride";

const stats = ref<Record<string, number>>({});
const barRef = ref();
let chart: echarts.ECharts | null = null;
let timer: number | null = null;

const kpiCards = ref([
  { label: "累计订单", value: 0 },
  { label: "已完成", value: 0 },
  { label: "进行中", value: 0 },
  { label: "风控拦截", value: 0 },
  { label: "已取消", value: 0 },
  { label: "GMV（元）", value: 0 },
  { label: "客单价（元）", value: 0 },
  { label: "空闲司机", value: 0 }
]);

const STATUS = [
  ["COMPLETED", "已完成", "#52c41a"],
  ["DISPATCHED", "已派单", "#722ed1"],
  ["TRIP", "行程中", "#4096ff"],
  ["MATCHING", "派单中", "#faad14"],
  ["CANCELLED", "已取消", "#ff7a45"],
  ["RISK_REJECTED", "风控拦截", "#f5222d"]
];

async function load() {
  const s: any = await getStats();
  stats.value = s ?? {};
  const kv: Record<string, string> = {
    totalOrders: "累计订单", completed: "已完成", active: "进行中",
    riskRejected: "风控拦截", cancelled: "已取消", gmv: "GMV（元）",
    avgPrice: "客单价（元）", idleDrivers: "空闲司机"
  };
  kpiCards.value.forEach(c => {
    const key = Object.keys(kv).find(k => kv[k] === c.label);
    if (key) c.value = (s as any)?.[key] ?? 0;
  });
  renderBar();
}

function renderBar() {
  if (!barRef.value) return;
  if (!chart) chart = echarts.init(barRef.value);
  const data = STATUS.map(([key]) =>
    key === "COMPLETED" ? (stats.value.completed ?? 0)
    : key === "DISPATCHED" ? countStatus("DISPATCHED")
    : key === "TRIP" ? 0 // TRIP 并入 active 展示
    : key === "MATCHING" ? 0
    : key === "CANCELLED" ? (stats.value.cancelled ?? 0)
    : (stats.value.riskRejected ?? 0)
  );
  // active 里包含 DISPATCHED+TRIP：把 TRIP 拆出来显示不了就并入进行中
  chart.setOption({
    tooltip: { trigger: "axis" },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: "category", data: STATUS.map(([, n]) => n) },
    yAxis: { type: "value", minInterval: 1 },
    series: [{
      type: "bar",
      data: STATUS.map(([key, name], i) => ({
        value: name === "已派单" ? Math.max(0, (stats.value.active ?? 0)) : data[i],
        itemStyle: { color: STATUS[i][2], borderRadius: [6, 6, 0, 0] }
      })),
      barWidth: 34,
      label: { show: true, position: "top" }
    }]
  });
}

function countStatus(_s: string) { return 0; }

onMounted(() => {
  load();
  timer = window.setInterval(load, 5000);
  window.addEventListener("resize", resize);
});
onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
  window.removeEventListener("resize", resize);
  chart?.dispose();
  chart = null;
});
function resize() { chart?.resize(); }
</script>

<style scoped>
.kpi-card :deep(.el-card__body) { padding: 14px 16px; }
.kpi-value { font-size: 26px; font-weight: 700; color: #722ed1; font-family: Consolas, monospace; }
.kpi-label { font-size: 12px; color: #909399; margin-top: 4px; letter-spacing: 1px; }
.dot {
  display: inline-block; width: 7px; height: 7px; border-radius: 50%;
  background: #52c41a; margin-right: 4px; animation: pulse 1.6s infinite;
}
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(82, 196, 26, 0.5); }
  70% { box-shadow: 0 0 0 6px rgba(82, 196, 26, 0); }
  100% { box-shadow: 0 0 0 0 rgba(82, 196, 26, 0); }
}
</style>
