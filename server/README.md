# MyNote 统计 + 反馈后端

一个单文件 Flask 轻量后端，给 MyNote 安卓 App 提供：

- **匿名使用统计**：App 每次启动上报一条匿名记录，后端统计累计用户 / 日活 / 月活 / 版本与机型分布
- **用户反馈收集**：App 内反馈表单提交到此，看板里统一查看

数据层**双模式自动切换**：

- 设置环境变量 `DATABASE_URL`（Neon 等云 PostgreSQL）→ 数据永久保存（生产推荐）
- 未设置 → 本地 SQLite（本地开发调试用）

## 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ping` | 匿名使用上报（body: `device_id`, `app_version`, `android_version`, `device_model`） |
| POST | `/api/feedback` | 反馈提交（body: `device_id`, `title`, `content`, `contact`, `app_version`） |
| GET  | `/api/stats` | 统计 JSON（需 token） |
| GET  | `/` | 统计看板网页（需 token 登录） |

## 本地运行（SQLite，无需数据库）

```bash
cd server
python -m venv venv
# Windows: venv\Scripts\activate    Linux/macOS: source venv/bin/activate
pip install -r requirements.txt
set MYNOTE_ADMIN_TOKEN=你的令牌        # Windows
# export MYNOTE_ADMIN_TOKEN=你的令牌    # Linux/macOS
python app.py
```

浏览器打开 `http://127.0.0.1:5000/`，输入令牌即可看到看板。

## 生产部署：Neon（免费数据库）+ Render（免费托管）

免费版 Render 的本地磁盘是临时的，SQLite 数据会被清空，所以生产环境用 **Neon 免费 PostgreSQL** 存数据。两者都免费、都不需要信用卡。

### 第一步：注册 Neon，建一个免费数据库

1. 打开 [neon.com](https://neon.com) → **Sign in**（可用 GitHub 账号）。
2. 创建一个新 **Project**，地区选离你近的（如新加坡）。
3. 创建后，Neon 会给你一个**连接串**（Connection string），形如：

   ```
   postgresql://用户名:密码@xxxxx.neon.tech/neondb?sslmode=require
   ```

   在项目页点 **Connect** → 复制 **Connection string**，先存起来，下一步用。

> Neon 免费额度：每项目 1GB 存储 + 100 CU 小时/月，空闲 5 分钟自动休眠（冷启动几百毫秒），对轻量统计完全够用，数据**永不过期**。

### 第二步：部署到 Render

1. 把 `server/` 目录推到 GitHub（和 App 同一个仓库即可）。
2. 登录 [render.com](https://render.com)（用 GitHub 账号）→ New → **Web Service** → 连接你的仓库。
3. 关键配置：

   | 配置项 | 值 |
   |--------|-----|
   | Root Directory | `server` |
   | Runtime | Python 3 |
   | Build Command | `pip install -r requirements.txt` |
   | Start Command | `gunicorn app:app --bind 0.0.0.0:$PORT` |
   | Instance Type | **Free** |
   | 环境变量 `MYNOTE_ADMIN_TOKEN` | 你的管理令牌（看板登录密码） |
   | 环境变量 `DATABASE_URL` | 第一步复制的 Neon 连接串 |

4. 点 **Create Web Service**，等 1~2 分钟构建完成，拿到 `https://xxx.onrender.com` 地址。

> 提示：Render 免费实例闲置约 15 分钟会休眠，首次请求会慢几十秒（冷启动），不影响数据正确性（数据在 Neon 里，不会丢）。

### 第三步：验证 + 接入 App

1. 浏览器打开 `https://你的地址/?token=你的令牌`，能打开看板即成功。
2. 把地址填到 App 端（改一处）：`app/src/main/java/com/mynote/android/util/UsageReporter.kt` 的 `SERVER_URL` 常量，改成 `https://你的地址`（末尾不要带 `/`）。
3. 重新打包 App 发布。

## 环境变量

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `MYNOTE_ADMIN_TOKEN` | 建议 | `changeme` | 看板/统计接口的管理令牌，**务必改成你自己的** |
| `DATABASE_URL` | 生产必填 | 空 | PostgreSQL 连接串（Neon），设置后启用 PG 持久化 |
| `MYNOTE_DB` | 否 | `./mynote_stats.db` | 仅 SQLite 模式生效，SQLite 文件路径 |
| `MYNOTE_SECRET` | 否 | 内置值 | Flask session 密钥 |
| `PORT` | 否 | `5000` | 监听端口 |

## 隐私说明

- App 上报的内容**全部匿名**：一个随机生成的 UUID（设备标识，与任何账号/IMEI/手机号无关）、App 版本号、Android 系统版本、设备型号。
- **不采集**：位置、通讯录、IMEI/MAC、账号、笔记内容等任何个人敏感信息。
- 反馈内容由用户主动填写，可能包含联系方式（可选），仅用于回复用户。
- 建议在 App 的隐私政策 / 免责声明中补充一句「本应用会上报匿名使用统计（设备型号、系统版本），用于改进产品」。
- App 端提供「使用统计」开关，默认开启，用户可随时关闭。
