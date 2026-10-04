<template>
  <el-card shadow="hover">
    <template #header>
      <div style="display: flex; align-items: center; gap: 10px">
        <span style="font-weight: 600">风控黑名单</span>
        <el-tag type="danger" effect="plain" size="small">命中的乘客下单将被风控Agent直接拦截</el-tag>
      </div>
    </template>

    <div style="display: flex; gap: 10px; margin-bottom: 14px">
      <el-input v-model="pid" placeholder="乘客标识（如 passenger-996）" style="width: 280px" />
      <el-input v-model="reason" placeholder="拉黑原因（可选）" style="width: 280px" />
      <el-button type="danger" @click="add">加入黑名单</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="passengerId" label="乘客标识" width="200" />
      <el-table-column prop="reason" label="原因" min-width="200" />
      <el-table-column label="加入时间" width="180">
        <template #default="{ row }">{{ (row.createdAt || "").replace("T", " ").slice(0, 19) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="del(row.passengerId)">移出</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { getBlacklist, addBlacklist, delBlacklist } from "@/api/ride";

const rows = ref<any[]>([]);
const loading = ref(false);
const pid = ref("");
const reason = ref("");

async function load() {
  loading.value = true;
  try {
    rows.value = (await getBlacklist()) ?? [];
  } finally {
    loading.value = false;
  }
}

async function add() {
  if (!pid.value.trim()) return ElMessage.warning("请输入乘客标识");
  const res: any = await addBlacklist({ passengerId: pid.value.trim(), reason: reason.value });
  if (res?.error) return ElMessage.error(res.error);
  ElMessage.success("已加入黑名单");
  pid.value = "";
  reason.value = "";
  load();
}

async function del(id: string) {
  await delBlacklist(id);
  ElMessage.success("已移出");
  load();
}

onMounted(load);
</script>
