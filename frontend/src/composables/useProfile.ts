import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { authApi, type ChangePasswordRequest } from '@/api/auth'

/**
 * 个人资料 & 修改密码的共享逻辑，供个人设置页面和右上角抽屉共用。
 */
export function useProfile() {
  const authStore = useAuthStore()

  // ===== 个人资料 =====
  const profileFormRef = ref<FormInstance>()
  const profileLoading = ref(false)
  const profileForm = reactive({
    realName: '',
    email: '',
    department: ''
  })

  /** 同步 store 用户信息到表单 */
  function syncProfileFromStore() {
    const user = authStore.user
    if (user) {
      profileForm.realName = user.realName || ''
      profileForm.email = user.email || ''
      profileForm.department = user.department || ''
    }
  }

  /** 保存个人资料 */
  async function handleSaveProfile() {
    if (!profileFormRef.value) return
    await profileFormRef.value.validate(async (valid) => {
      if (!valid) return
      profileLoading.value = true
      try {
        const updated = await authApi.updateProfile({
          realName: profileForm.realName || undefined,
          email: profileForm.email || undefined,
          department: profileForm.department || undefined
        })
        authStore.user = updated
        localStorage.setItem('user', JSON.stringify(updated))
        ElMessage.success('个人资料已更新')
      } catch (e: any) {
        ElMessage.error(e.message || '更新失败')
      } finally {
        profileLoading.value = false
      }
    })
  }

  // ===== 修改密码 =====
  const passwordFormRef = ref<FormInstance>()
  const passwordLoading = ref(false)
  const passwordForm = reactive<ChangePasswordRequest & { confirmPassword: string }>({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })

  const passwordRules: FormRules = {
    oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
      { min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: '请确认新密码', trigger: 'blur' },
      {
        validator: (_rule, value, callback) => {
          if (value !== passwordForm.newPassword) {
            callback(new Error('两次输入的密码不一致'))
          } else {
            callback()
          }
        },
        trigger: 'blur'
      }
    ]
  }

  /** 修改密码成功后回调（由调用方定义，因为页面和抽屉行为不同） */
  let onPasswordSuccess: (() => void) | null = null

  /** 修改密码 */
  async function handleChangePassword() {
    if (!passwordFormRef.value) return
    await passwordFormRef.value.validate(async (valid) => {
      if (!valid) return
      passwordLoading.value = true
      try {
        await authApi.changePassword({
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        })
        ElMessage.success('密码修改成功')
        passwordForm.oldPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''
        onPasswordSuccess?.()
      } catch (e: any) {
        ElMessage.error(e.message || '修改失败')
      } finally {
        passwordLoading.value = false
      }
    })
  }

  /** 注册密码成功回调 */
  function setPasswordSuccessHandler(handler: () => void) {
    onPasswordSuccess = handler
  }

  return {
    // 个人资料
    profileFormRef,
    profileLoading,
    profileForm,
    syncProfileFromStore,
    handleSaveProfile,
    // 修改密码
    passwordFormRef,
    passwordLoading,
    passwordForm,
    passwordRules,
    handleChangePassword,
    setPasswordSuccessHandler
  }
}
