import { reactive } from "vue";
import type { FormRules } from "element-plus";

/** 登录校验（与后端口径一致：密码 ≥6 位即可；复杂度要求仅用于注册场景） */
const loginRules = reactive<FormRules>({
  password: [
    {
      validator: (rule, value, callback) => {
        if (value === "") {
          callback(new Error("请输入密码"));
        } else if (String(value).length < 6) {
          callback(new Error("密码至少 6 位"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ]
});

export { loginRules };
