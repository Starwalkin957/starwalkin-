# 物品管理 App (ItemManager)

一款用于管理个人物品的原生 Android 客户端 App，帮助用户记录、查找和整理自己的物品。

## 功能特性

### 基础功能（已实现）

- **物品 CRUD**：支持物品的添加、查看、编辑和删除
- **物品信息**：包含名称、分类、描述、存放位置、数量，并支持图片
- **本地持久化**：使用 Room 数据库保存数据，重新打开 App 后仍能正常查看
- **分类筛选**：按物品分类进行筛选，分类从已有数据动态生成
- **搜索功能**：按名称或描述模糊搜索物品
- **完整操作流程**：列表 → 详情 → 编辑/删除 → 返回，流程闭环

### 技术栈

| 技术 | 说明 |
|------|------|
| **Kotlin** | 开发语言 |
| **Jetpack Compose** | 声明式 UI 框架 |
| **Material 3** | 设计系统 |
| **Room** | 本地数据库持久化 |
| **Navigation Compose** | 页面导航 |
| **ViewModel** | UI 状态管理 |
| **Kotlin Coroutines + Flow** | 异步编程与响应式数据流 |
| **Coil** | 图片加载库 |
| **MVVM** | 架构模式 |

## 项目结构

```
app/src/main/java/com/example/itemmanager/
├── MainActivity.kt                 # 应用入口
├── data/
│   ├── local/
│   │   ├── ItemEntity.kt           # 物品数据实体（Room 表）
│   │   ├── ItemDao.kt              # 数据访问对象（CRUD 接口）
│   │   └── ItemDatabase.kt         # 数据库单例
│   └── repository/
│       └── ItemRepository.kt       # 数据仓库（UI 层与数据层中介）
├── navigation/
│   └── AppNavigation.kt            # 导航图定义
├── ui/
│   ├── theme/                      # 主题、颜色、字体
│   ├── list/                       # 物品列表页
│   │   ├── ItemListScreen.kt
│   │   └── ItemListViewModel.kt
│   ├── detail/                     # 物品详情页
│   │   ├── ItemDetailScreen.kt
│   │   └── ItemDetailViewModel.kt
│   └── edit/                       # 添加/编辑页
│       ├── ItemEditScreen.kt
│       └── ItemEditViewModel.kt
└── util/
    └── ImageUtils.kt               # 图片处理工具
```

## 构建与运行

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.2+

### 构建步骤

1. 克隆仓库到本地
2. 用 Android Studio 打开项目（选择 `ItemManager` 目录）
3. 等待 Gradle 同步完成
4. 连接 Android 设备或启动模拟器（API 24+）
5. 点击 Run 按钮或执行 `./gradlew installDebug`

### 生成 Gradle Wrapper

如果项目缺少 `gradlew` 文件，在项目根目录执行：

```bash
gradle wrapper --gradle-version 8.2
```

## 数据存储说明

- **结构化数据**：存储在 Room SQLite 数据库中（`item_database`）
- **图片文件**：复制到 App 私有目录 `filesDir/images/` 下，以时间戳命名
- 删除物品时会同步删除关联的图片文件
- 支持 Android 自动备份（包含数据库和图片目录）

## 进阶功能（可扩展方向）

- [ ] 物品到期、保修时间提醒（AlarmManager + Notification）
- [ ] 标签系统、多维度排序、统计仪表盘
- [ ] 调用相机拍照、系统分享、桌面 Widget
- [ ] 大量物品分页加载、图片压缩优化、数据导出/导入

## License

MIT License
