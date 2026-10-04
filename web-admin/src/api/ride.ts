import { http } from "@/utils/http";

type Resp<T = any> = Promise<T>;

/** 运营看板统计 */
export const getStats = (): Resp => http.request("get", "/api/admin/stats");

/** 订单分页（status 可选） */
export const getOrders = (params: { status?: string; page?: number; size?: number }): Resp =>
  http.request("get", "/api/admin/orders", { params });

/** 司机列表 */
export const getDrivers = (): Resp => http.request("get", "/api/admin/drivers");

/** 黑名单 */
export const getBlacklist = (): Resp => http.request("get", "/api/admin/blacklist");
export const addBlacklist = (data: { passengerId: string; reason?: string }): Resp =>
  http.request("post", "/api/admin/blacklist", { data });
export const delBlacklist = (passengerId: string): Resp =>
  http.request("delete", `/api/admin/blacklist/${passengerId}`);

/** 事件审计分页 */
export const getEvents = (params: { page?: number; size?: number }): Resp =>
  http.request("get", "/api/admin/events", { params });

/** Agent 事件流（实时，演示控制台同款） */
export const getLiveEvents = (): Resp => http.request("get", "/api/events");
