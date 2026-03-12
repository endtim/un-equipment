import { createApp } from 'vue'
import ElementPlus, { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import store from './store'
import { installGlobalErrorHandlers } from './utils/error-center'
import './styles.css'

const app = createApp(App)

app.use(ElementPlus, { locale: zhCn })
app.use(router)
app.use(store)
installGlobalErrorHandlers({ app, router })
app.config.globalProperties.$message = ElMessage
app.mount('#app')
