import { http } from "@/utils/http";
import { getToken } from "@/utils/auth";

export type UserResult = {
  success: boolean;
  data: {
    avatar: string;
    username: string;
    nickname: string;
    roles: Array<string>;
    permissions: Array<string>;
    accessToken: string;
    refreshToken: string;
    expires: Date;
  };
};

export type RefreshTokenResult = {
  success: boolean;
  data: {
    accessToken: string;
    refreshToken: string;
    expires: Date;
  };
};

/** 登录（对接 Spring Boot /api/auth/login：{phone, password} → {token, role, name}）
 *  pure-admin 表单字段为 username → 映射为后端的 phone（账号即手机号/账号名） */
export const getLogin = (data?: object) => {
  const payload = {
    phone: (data as any)?.username ?? (data as any)?.phone ?? "",
    password: (data as any)?.password ?? ""
  };
  return http.request<UserResult>("post", "/api/auth/login", { data: payload }).then((res: any) => {
    // 适配 pure-admin 的认证数据结构
    if (res && res.token) {
      return {
        success: true,
        data: {
          avatar: "",
          username: String(res.userId ?? res.name),
          nickname: res.name,
          roles: [res.role === "ADMIN" ? "admin" : res.role === "DRIVER" ? "driver" : "passenger"],
          permissions: ["*:*:*"],
          accessToken: res.token,
          refreshToken: res.token,
          expires: new Date(Date.now() + 7 * 24 * 3600 * 1000) as any
        }
      } as unknown as UserResult;
    }
    return { success: false, data: null } as unknown as UserResult;
  });
};

/** 刷新 token（后端为长效 HMAC 令牌，直接复用当前 token） */
export const refreshTokenApi = (data?: object) => {
  const current = getToken();
  return Promise.resolve({
    success: true,
    data: {
      accessToken: current?.accessToken ?? "",
      refreshToken: current?.refreshToken ?? "",
      expires: new Date(Date.now() + 7 * 24 * 3600 * 1000) as any
    }
  } as unknown as RefreshTokenResult);
};
