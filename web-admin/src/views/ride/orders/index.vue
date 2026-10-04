<template>
  <el-card shadow="hover">
    <template #header>
      <div style="display: flex; align-items: center; gap: 12px">
        <span style="font-weight: 600">订单管理</span>
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 160px" @change="load(0)">
          <el-option v-for="s in STATUS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button type="primary" plain size="small" @click="load()">刷新</el-button>
        <span style="margin-left: auto; color: #909399; font-size: 12px">
          共 {{ total }} 单 · 数据持久化于 H2 文件库
        </span>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="订单号" width="110" />
      <el-table-column prop="passengerId" label="乘客" width="150" show-overflow-tooltip />
      <el-table-column label="行程" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.origin }} → {{ row.destination }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="tagType(row.status)" effect="light" size="small">{{ tagLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="driverId" label="司机" width="90" />
      <el-table-column label="里程" width="90">
        <template #default="{ row }">{{ row.distanceKm ? row.distanceKm.toFixed(1) + " km" : "-" }}</template>
      </el-table-column>
      <el-table-column label="价格" width="100">
        <template #default="{ row }">
          <b v-if="row.finalPrice > 0" style="color: #722ed1">¥{{ row.finalPrice }}</b>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="payMethod" label="支付" width="100" />
      <el-table-column label="下单时间" width="170">
        <template #default="{ row }">{{ (row.createdAt || "").replace("T", " ").slice(0, 19) }}</template>
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
import { onMounted, ref } from "vue";
import { getOrders } from "@/api/ride";

const STATUS = [
  { value: "", label: "全部" },
  { value: "CREATED", label: "已创建" },
  { value: "MATCHING", label: "派单中" },
  { value: "DISPATCHED", label: "已派单" },
  { value: "TRIP", label: "行程中" },
  { value: "PAID", label: "已支付" },
  { value: "COMPLETED", label: "已完成" },
  { value: "CANCELLED", label: "已取消" },
  { value: "RISK_REJECTED", label: "风控拦截" }
];
const TAG: Record<string, string> = {
  CREATED: "info", MATCHING: "warning", DISPATCHED: "primary", TRIP: "primary",
  PAID: "success", COMPLETED: "success", CANCELLED: "danger", RISK_REJECTED: "danger"
};
const LABEL: Record<string, string> = Object.fromEntries(STATUS.map(s => [s.value, s.label]));

const rows = ref<any[]>([]);
const total = ref(0);
const page = ref(0);
const pageSize = 10;
const status = ref("");
const loading = ref(false);

const tagType = (s: string) => (TAG[s] || "info") as any;
const tagLabel = (s: string) => LABEL[s] || s;

async function load(p = page.value) {
  page.value = p;
  loading.value = true;
  try {
    const res: any = await getOrders({ status: status.value, page: p, size: pageSize });
    rows.value = res?.content ?? [];
    total.value = res?.totalElements ?? 0;
  } finally {
    loading.value = false;
  }
}
onMounted(() => load(0));
</script>
