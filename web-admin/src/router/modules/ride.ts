const Layout = () => import("@/layout/index.vue");

export default {
  path: "/ride",
  name: "Ride",
  component: Layout,
  redirect: "/ride/dashboard",
  meta: {
    icon: "ep/van",
    title: "调度中心",
    rank: 1
  },
  children: [
    {
      path: "/ride/dashboard",
      name: "RideDashboard",
      component: () => import("@/views/ride/dashboard/index.vue"),
      meta: { title: "运营看板", icon: "ep/data-analysis" }
    },
    {
      path: "/ride/orders",
      name: "RideOrders",
      component: () => import("@/views/ride/orders/index.vue"),
      meta: { title: "订单管理", icon: "ep/tickets" }
    },
    {
      path: "/ride/drivers",
      name: "RideDrivers",
      component: () => import("@/views/ride/drivers/index.vue"),
      meta: { title: "司机管理", icon: "ep/user" }
    },
    {
      path: "/ride/blacklist",
      name: "RideBlacklist",
      component: () => import("@/views/ride/blacklist/index.vue"),
      meta: { title: "风控黑名单", icon: "ep/lock" }
    },
    {
      path: "/ride/events",
      name: "RideEvents",
      component: () => import("@/views/ride/events/index.vue"),
      meta: { title: "Agent事件审计", icon: "ep/bell" }
    }
  ]
} satisfies RouteConfigsTable;
