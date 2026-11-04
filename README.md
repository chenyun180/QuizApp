# 刷题（QuizApp）

一个用 Kotlin + Jetpack Compose 写的 Android 刷题 App。题库通过 **JSON 文件导入**，不需要联网、不需要任何系统权限，数据全部落在本机 SQLite 里。

包名 `com.alri.quiz`，最低 Android 8.0（API 26），编译/目标 API 35。

---

## 1. 五分钟跑起来

```bash
# 这个环境里工具链是手动装的，每次开新终端都要先导出这些变量
export JAVA_HOME=/opt/jdk
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH=/opt/jdk/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:$PATH

cd QuizApp
./gradlew :app:assembleDebug        # 出 debug 包，可直接装
./gradlew :app:assembleRelease      # 出签名 release 包
./gradlew test                      # 跑全部 JVM 单元测试
```

产物位置：

| 产物 | 路径 | 大小（当前） |
| --- | --- | --- |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` | ≈ 9.8 MB |
| Release APK（已签名） | `app/build/outputs/apk/release/app-release.apk` | ≈ 1.2 MB |

release 包体积小这么多，是因为开了 R8 代码混淆 + 资源压缩，把没被引用到的类、资源全删了。

---

## 2. 装到手机上

### 方式 A：数据线 + adb（推荐，能看日志）

1. 手机开「开发者选项」→ 打开「USB 调试」。
2. 连上电脑，`adb devices` 能看到设备。
3. `adb install -r app/build/outputs/apk/release/app-release.apk`

### 方式 B：直接把 APK 传到手机

1. 把 `app-release.apk` 发到手机（微信文件传输、网盘、数据线拷贝都行）。
2. 在手机上点开它，系统提示「为了安全，你的手机不允许安装未知来源的应用」。
3. 点「设置」→ 允许该来源（文件管理器 / 浏览器）安装应用 → 返回继续安装。
4. 如果弹出的拦截页写着 **"Play 保护机制已阻止安装"**，点「仍要安装」即可——这是 Google 的侧载提示，不是病毒警告。

> 国内厂商（小米、华为、OV）会额外弹「风险应用」提示，需要在系统设置里临时关闭「纯净模式」/「外部来源应用检查」。这是厂商策略，跟 APK 本身无关。

**装不上时的排查顺序**：
- `INSTALL_FAILED_OLDER_SDK` → 手机低于 Android 8.0。
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE` → 手机上已经有同包名但签名不同的版本（比如你之前装过别人给的包）。先 `adb uninstall com.alri.quiz` 再装，但注意这样做会清掉数据。
- `INSTALL_PARSE_FAILED_NO_CERTIFICATES` → APK 没签名（用错了产物，比如拿到了 `app-release-unsigned.apk`）。本项目已配置签名，正常不会遇到。

---

## 3. 关于签名（这一段最重要，别跳过）

Android 用签名来回答一个问题：「这个更新包，是不是同一个作者发的？」只有签名一致，系统才允许覆盖安装，用户数据才保得住。

- 密钥库：`../keystore/quiz-release.jks`（在工程目录**之外**）
- 别名：`quiz`
- 口令：见 `keystore.properties`

**必须做的两件事：**

1. **备份 `.jks` 文件和口令**，至少两处（比如密码管理器 + 移动硬盘）。丢了它，你就再也无法给已安装的用户发更新，只能让他们卸载重装、数据清零。
2. **别把 `.jks` 和 `keystore.properties` 提交到 Git**。工程里的 `.gitignore` 已经忽略了它们。口令泄露等于任何人都能冒充你发版。

想生成自己的密钥库：

```bash
keytool -genkeypair -v \
  -keystore ../keystore/quiz-release.jks \
  -alias quiz -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass '你的口令' -keypass '你的口令' \
  -dname "CN=你的名字, OU=Dev, O=YourOrg, L=Shanghai, ST=Shanghai, C=CN"
```

`-validity 10000`（约 27 年）是 Google Play 的要求：签名证书有效期必须越过 2033 年。

---

## 4. 题库 JSON 格式

导入入口：首页右下角「+」→ 选一个 `.json` 文件。用系统文件选择器（SAF），所以 App **不需要存储权限**。

最小可用的题库：

```json
{
  "bankId": "android-basic",
  "title": "Android 基础",
  "questions": [
    {
      "id": "q1",
      "type": "single",
      "stem": "Activity 已经对用户可见、但还没有进入可交互状态时，最后回调的是哪一个？",
      "options": [
        { "key": "A", "text": "onStart" },
        { "key": "B", "text": "onResume" }
      ],
      "answers": ["A"],
      "explanation": "可见但不可交互对应 started 状态，由 onStart 进入；onResume 之后才进入前台可交互的 resumed 状态。"
    }
  ]
}
```

### 字段说明

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `schemaVersion` | 否 | 目前只认 `1`，省略视作 1 |
| `bankId` | **是** | 题库唯一标识，只能含字母数字 `._-`。**再次导入同一个 bankId = 覆盖更新该题库** |
| `title` | **是** | 显示名 |
| `version` | 否 | 题库自身的版本号，仅作记录 |
| `questions` | **是** | 至少一题 |
| `questions[].id` | **是** | 题目 id，在题库内唯一 |
| `questions[].type` | 否 | `single` / `multiple` / `true_false`，省略按 `single` |
| `questions[].stem` | **是** | 题干 |
| `questions[].options` | 是（判断题可省） | 至少 2 个，`key` 不能重复 |
| `questions[].answers` | **是** | 正确选项的 key 列表，不能为空，必须都能在 options 里找到 |
| `questions[].explanation` | 否 | 解析，答完显示 |
| `questions[].tags` | 否 | 标签数组，用于按标签刷题 |
| `questions[].image` | 否 | 图片引用，当前版本保留字段未渲染 |

判断题可以不写 `options`，默认 `T` = 正确、`F` = 错误；`answers` 也可以直接写 `true`/`false`/`正确`/`错误`，解析器会归一化。

### 内置题库

`app/src/main/assets/samples/sample_bank.json` 是随包装进去的「Android 基础 · 30 题」（16 单选 / 8 多选 / 6 判断，每题都带解析和标签），首次启动自动导入，只导一次。这份题库的副本也放在 `core/data/src/test/resources/banks/android-basics-30.json`，由 `AndroidBasicsBankTest` 用真解析器跑一遍——题库只要有一处不合契约，单元测试就先红，不会等到用户装上去看到空题库。

### 校验是严格的，而且是「全量报错」

解析器开了 `ignoreUnknownKeys = false`，写错字段名（比如把 `stem` 写成 `title`）会直接报错，不会被静默忽略。所有问题一次性列在导入页上，一条都不吞。这是故意的：题库动辄几百题，报一个改一个会疯。

---

## 5. 工程结构

```
QuizApp/
├── app/                    # 应用壳：Application、MainActivity、导航图、签名配置、图标
├── core/
│   ├── model/              # 纯 Kotlin，无 Android 依赖：Question / QuizBank / 判分逻辑
│   ├── database/           # Room：实体、DAO、TypeConverter、Schema 导出
│   ├── data/               # Repository + JSON 解析器，UI 与数据库之间唯一的接口层
│   └── ui/                 # 设计系统：主题、颜色、通用组件、路由常量
├── feature/
│   ├── quiz/               # 刷题、题库列表、错题本、收藏夹、设置
│   └── import/             # 题库导入页
└── gradle/libs.versions.toml   # 所有依赖版本集中在这里
```

### 为什么这么分层

- **`core:model` 是纯 JVM 模块。** 判分逻辑（单选/多选/判断题怎么算对）不依赖 Android，所以能用最快的 JVM 单元测试跑，一秒钟几十个用例。这是判分这种「错了就全错」的逻辑最该被覆盖的地方。
- **`core:data` 是唯一的接缝。** 上层只认 `QuizRepository` 接口，不认识 Room，也不认识 JSON。以后想把题库改成从网络下载、或者换成别的数据库，只改这一层。
- **`app` 是唯一知道所有 feature 的模块**，导航图放在这里。feature 之间不互相依赖，加一个新页面不用动别的 feature。
- **依赖方向单向**：feature → core，core:data → core:database → core:model。没有环。

### 技术选型

| 关注点 | 选择 | 原因 |
| --- | --- | --- |
| UI | Jetpack Compose + Material 3 | 官方主推，界面即状态，没有 XML 布局和 findViewById |
| 状态 | ViewModel + StateFlow | 屏幕旋转、进程被杀后状态不丢 |
| 依赖注入 | Hilt | 编译期检查依赖图，比手写单例好维护 |
| 数据库 | Room | 编译期校验 SQL，导出 schema 支持迁移测试 |
| JSON | kotlinx.serialization | 纯 Kotlin，不用反射，R8 混淆后也安全 |
| 导航 | Navigation Compose | 单 Activity + 路由 |
| 异步 | Coroutines + Flow | 数据库变更自动推送 UI |

---

## 6. 几个实现上的关键决定

**错题本 = 「最近一次作答是错的」**，不是「曾经错过」。所以你把错题重做对了，它自动从错题本消失。想保留「历史错题」的话，`answer_records` 表里每一次作答都留着，随时能加查询。

**导入是幂等的**：`bankId` 是主键，整库覆盖在**一个事务**里完成（先删旧题、再插新题）。中途失败整体回滚，不会留下半个题库。

**刷题进度存在 `SavedStateHandle` 里**，但用自定义文本编码而不是直接序列化整个列表。因为 Bundle 有约 1 MB 上限，几百道题的对象图会触发 `TransactionTooLargeException`——这是新手最容易踩、也最难查的崩溃之一。

**没有动态取色。** Material You 的动态取色很漂亮，但会把「正确绿 / 错误红」这些有语义的颜色也染掉。答题 App 的对错反馈必须稳定，所以配色是我们自己定死的。

**零权限。** 导入用 SAF（`ActivityResultContracts.OpenDocument`），是用户主动选文件，系统把内容句柄交给 App，不需要 `READ_EXTERNAL_STORAGE`。清单里除了 AndroidX 自动加的那个内部广播权限，没有任何 `uses-permission`。

---

## 7. 测试

```bash
./gradlew test                                  # 53 个 JVM 单元测试（debug + release 各跑一遍，共 90 次执行）
./gradlew :core:database:assembleDebugAndroidTest   # 编译仪器测试（不需要设备）
./gradlew :core:database:connectedDebugAndroidTest  # 真跑仪器测试（需要设备/模拟器）
```

- `app` — 8 个内置题库写入策略测试：全新安装该导入、老用户升级要把 v1.0.0 的 14 题旧示例换成新的、用户自己导入的题库绝不能被删、用户删过示例后不能反复塞回来。
- `core:model` — 16 个判分测试：单选、多选（集合完全相等才算对）、判断题、空答案、分母为零。
- `core:data` — 21 个 JSON 解析测试：正确路径 + 17 种错误输入，包括未知字段、重复选项 key、答案引用不存在的 key、多选只给一个答案等。
- `core:data` — 8 个内置题库回归测试：用真解析器解析打包进 APK 的那份 30 题 JSON，校验通过、题数与题型分布、id 集合、每题解析长度、标签词表、选项与答案一致性。
- `core:database` — 10 个仪器测试：真实 SQLite 上的导入、重复导入覆盖、级联删除、错题本最新记录语义、收藏幂等、标签 JOIN 查询。

---

## 8. 常见问题

**`SDK location not found`** — 缺 `local.properties`，或者里面 `sdk.dir` 指错了。这个文件是机器相关的，不进 Git。

**`Unsupported class file major version` / 一堆 Kotlin 编译报错** — JDK 版本不对。本项目要 **JDK 17**，用 `java -version` 确认。

**`Failed to install the following Android SDK packages ... licenses`** — 需要接受 SDK 许可：`yes | sdkmanager --licenses`。

**改了版本目录里的 AGP 版本后各种诡异错误** — AGP、Gradle、Kotlin、KSP 四者的版本是绑死的。本项目是 Gradle 8.9 + AGP 8.7.3 + Kotlin 2.0.21 + KSP 2.0.21-1.0.28，升一个要查兼容表，别单独升。

**构建机器内存不够、OOM** — `gradle.properties` 里 `org.gradle.jvmargs` 已经把堆限到 2560 MB，并限制 `workers.max=2`。内存更小的机器可以再往下调，或者加 `--no-daemon`。

**release 包运行崩溃、debug 不崩** — 99% 是 R8 把反射用到的类删了或改名了。看 `app/build/outputs/mapping/release/mapping.txt` 还原堆栈，然后往 `app/proguard/rules.pro` 加 keep 规则。

---

## 9. 接下来可以做什么

- 题库版本更新提示（比对 `version` 字段）
- 按章节/标签的组合筛选
- 答题计时、每日目标、连续打卡
- 题目图片渲染（`image` 字段已预留）
- 云端同步（需要账号体系，会引入网络权限和后端）
- 上架 Google Play：需要出 `.aab`（`./gradlew :app:bundleRelease`）、隐私政策、商店素材
