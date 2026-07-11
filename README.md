# MyNote (Android 原生版)

从 uni-app 项目迁移而来的 Android 原生应用,使用 Kotlin + MVVM + Room + Material 3。

## 技术栈

- **语言**: Kotlin 1.9.24
- **构建**: Gradle 8.9 + AGP 8.5.2 + KSP
- **架构**: MVVM + ViewBinding
- **数据库**: Room 2.6.1
- **UI**: Material Components 3
- **异步**: Kotlin Coroutines
- **网络**: OkHttp 4.12
- **指纹**: AndroidX Biometric
- **最低 SDK**: 21 (Android 5.0)

## 工程结构

```
MyNote-Android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mynote/android/
│       │   ├── MyNoteApp.kt              # Application
│       │   ├── data/                     # 数据层
│       │   │   ├── entity/               # Room Entity
│       │   │   │   ├── ParentCategory.kt
│       │   │   │   ├── SubCategory.kt
│       │   │   │   ├── Note.kt
│       │   │   │   └── ContentItem.kt
│       │   │   ├── dao/                  # Room DAO
│       │   │   │   ├── CategoryDao.kt
│       │   │   │   └── NoteDao.kt
│       │   │   └── AppDatabase.kt
│       │   ├── security/
│       │   │   └── LockManager.kt        # 自动锁定
│       │   ├── ui/
│       │   │   ├── base/
│       │   │   │   ├── BaseActivity.kt   # 自动锁屏基类
│       │   │   │   └── ActivityStack.kt
│       │   │   ├── lock/LockActivity.kt  # 锁屏页
│       │   │   ├── main/MainActivity.kt  # 主页
│       │   │   ├── edit/EditActivity.kt  # 编辑页
│       │   │   └── settings/SettingsActivity.kt
│       │   └── util/Prefs.kt             # SharedPreferences
│       └── res/
│           ├── layout/                   # 4 个 Activity 布局
│           ├── values/                   # colors/strings/themes
│           ├── drawable/
│           └── mipmap-anydpi-v26/        # 启动图标
├── build.gradle.kts                      # 项目级
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/gradle-wrapper.properties
```

## 如何打开

1. 打开 Android Studio
2. File → Open → 选择 `F:\NOTE\MyNote-Android` 目录
3. 等待 Gradle 同步完成(首次会下载依赖)
4. 连接真机或模拟器,点 Run 运行

## 迁移进度

### 已完成(第一阶段骨架)
- [x] 工程结构与 Gradle 配置
- [x] AndroidManifest(权限、4 个 Activity)
- [x] Room 数据层(ParentCategory/SubCategory/Note/ContentItem + DAO)
- [x] Application + BaseActivity(自动锁屏框架)
- [x] 4 个 Activity 骨架(可运行)
- [x] 资源文件(主题、颜色、字符串、布局、图标)

### 待实现(后续阶段)
- [ ] 主页:主题分类网格 + 笔记列表 + 搜索 + 导入导出
- [ ] 编辑页:富文本 + 录音/拍照/视频 + 形状手势
- [ ] 设置页:密码管理 + 指纹 + 防卸载(DevicePolicyManager) + 备份
- [ ] 锁屏页:星空动画 + 自定义数字键盘
- [ ] 数据迁移工具(从 uni-app setStorage 导入)

## 与原 uni-app 项目的对应关系

| uni-app | Android 原生 |
|---------|-------------|
| `uni.setStorageSync('notes', ...)` | Room `notes` 表 |
| `uni.setStorageSync('parentCategories', ...)` | Room `parent_categories` + `sub_categories` 表 |
| `note.contentList` 数组 | Room `content_items` 表 |
| `plus.fingerprint.authenticate` | AndroidX BiometricPrompt |
| `uni.getRecorderManager` | MediaRecorder |
| `plus.io.*` 文件操作 | SAF / MediaStore |
| `plus.android.importClass` Intent 桥接 | 原生 Intent(无需桥接) |
| 防卸载原生插件 aar | 直接 DevicePolicyManager |
| App.vue onShow/onHide 自动锁定 | BaseActivity + LockManager |
| pages.json 路由 | AndroidManifest Activity 注册 |
