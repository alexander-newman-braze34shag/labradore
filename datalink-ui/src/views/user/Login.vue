<template>
  <div class='main'>
    <a-form
      id='formLogin'
      class='user-layout-login'
      ref='formLogin'
      :form='form'
      @submit='handleSubmit'
    >
      <a-tabs
        :activeKey='customActiveKey'
        :tabBarStyle="{ textAlign: 'center', borderBottom: 'unset' }"
        @change='handleTabClick'
      >
        <a-tab-pane key='tab1' tab='登录'>
          <a-alert
            v-if='isLoginError'
            type='error'
            showIcon
            style='margin-bottom: 24px;'
            message='账号或密码错误'
          />
          <a-form-item>
            <a-input
              size='large'
              type='text'
              placeholder='请输入账号'
              v-decorator="[
                'username',
                {rules: [{ required: true, message: '请输入账号' }, { validator: handleUsernameOrEmail }], validateTrigger: 'change'}
              ]"
            >
              <a-icon slot='prefix' type='user' :style="{ color: 'rgba(0,0,0,.25)' }" />
            </a-input>
          </a-form-item>

          <a-form-item>
            <a-input-password
              size='large'
              placeholder='请输入密码'
              v-decorator="[
                'password',
                {rules: [{ required: true, message: '请输入密码' }], validateTrigger: 'blur'}
              ]"
            >
              <a-icon slot='prefix' type='lock' :style="{ color: 'rgba(0,0,0,.25)' }" />
            </a-input-password>
          </a-form-item>
        </a-tab-pane>
      </a-tabs>

      <!--
            <a-form-item>
              <a-checkbox
                v-decorator="['rememberMe', { valuePropName: 'checked' }]"
              >{{ $t('user.login.remember-me') }}</a-checkbox>
            </a-form-item>
      -->

      <a-form-item style='margin-top:24px'>
        <a-button
          size='large'
          type='primary'
          htmlType='submit'
          class='login-button'
          :loading='state.loginBtn'
          :disabled='state.loginBtn'
        >登录
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script>
// import md5 from 'md5'
import { mapActions } from 'vuex'
import { timeFix } from '@/utils/util'
import bcrypt from 'bcryptjs'

export default {
  data() {
    return {
      customActiveKey: 'tab1',
      loginBtn: false,
      // login type: 0 email, 1 username, 2 telephone
      loginType: 0,
      isLoginError: false,
      requiredTwoStepCaptcha: false,
      stepCaptchaVisible: false,
      form: this.$form.createForm(this),
      state: {
        time: 60,
        loginBtn: false,
        // login type: 0 email, 1 username, 2 telephone
        loginType: 0,
        smsSendBtn: false
      }
    }
  },
  created() {
  },
  methods: {
    ...mapActions(['Login', 'Logout']),
    // handler
    handleUsernameOrEmail(rule, value, callback) {
      const { state } = this
      const regex = /^([a-zA-Z0-9_-])+@([a-zA-Z0-9_-])+((\.[a-zA-Z0-9_-]{2,3}){1,2})$/
      if (regex.test(value)) {
        state.loginType = 0
      } else {
        state.loginType = 1
      }
      callback()
    },
    handleTabClick(key) {
      this.customActiveKey = key
      // this.form.resetFields()
    },
    handleSubmit(e) {
      e.preventDefault()
      const {
        form: { validateFields },
        state,
        customActiveKey,
        Login
      } = this

      state.loginBtn = true

      const validateFieldsKey = customActiveKey === 'tab1' ? ['username', 'password'] : ['mobile', 'captcha']

      validateFields(validateFieldsKey, { force: true }, (err, values) => {
        if (!err) {
          console.log('login form', values)
          const loginParams = { ...values }
          delete loginParams.username
          loginParams[!state.loginType ? 'email' : 'username'] = values.username
          loginParams.password = values.password
          Login(loginParams)
            .then(res => this.loginSuccess(res))
            .catch(err => this.requestFailed(err))
            .finally(() => {
              state.loginBtn = false
            })
        } else {
          setTimeout(() => {
            state.loginBtn = false
          }, 600)
        }
      })
    },
    getPassword(password) {
      return bcrypt.hashSync(password, bcrypt.genSaltSync(-1))
    },
    loginSuccess(res) {
      this.$router.push({ path: '/' })
      // 延迟 1 秒显示欢迎信息
     // setTimeout(() => {
        this.$notification.success({
          message: '登录成功',
          description: `${timeFix()}，欢迎回来`,
          duration: 1.5
        })
    //  }, 1000)
      this.isLoginError = false
    },
    requestFailed(err) {
      this.isLoginError = true
      this.$notification['error']({
        message: '错误',
        description: ((err.response || {}).data || {}).message || '请求出现错误，请稍后再试',
        duration: 2
      })
    }
  }
}
</script>

<style lang='less' scoped>
.user-layout-login {
  label {
    font-size: 14px;
  }

  .getCaptcha {
    display: block;
    width: 100%;
    height: 40px;
  }

  .forge-password {
    font-size: 14px;
  }

  button.login-button {
    padding: 0 15px;
    font-size: 16px;
    height: 40px;
    width: 100%;
  }

  .user-login-other {
    text-align: left;
    margin-top: 24px;
    line-height: 22px;

    .item-icon {
      font-size: 24px;
      color: rgba(0, 0, 0, 0.2);
      margin-left: 16px;
      vertical-align: middle;
      cursor: pointer;
      transition: color 0.3s;

      &:hover {
        color: #1890ff;
      }
    }

    .register {
      float: right;
    }
  }
}
</style>
