# -*- coding: utf-8 -*-
"""MyNote 使用统计 + 用户反馈 轻量后端（Flask）。

存储层双模式（自动切换）：
  - 设置环境变量 DATABASE_URL（Neon 等云 PostgreSQL 连接串）→ 用 PostgreSQL，数据永久保存
  - 未设置 DATABASE_URL → 用本地 SQLite，方便本地开发调试

接口：
  POST /api/ping        匿名使用上报（App 每次启动调用）
  POST /api/feedback    用户反馈提交
  GET  /api/stats       统计 JSON（需 X-Admin-Token 头或 ?token=）
  GET  /                统计看板网页（需登录 token）

本地运行（SQLite）：
  pip install -r requirements.txt
  MYNOTE_ADMIN_TOKEN=你的令牌 python app.py
  浏览器打开 http://127.0.0.1:5000/ （用 token 登录）

生产部署（Neon + Render）见同目录 README.md。
"""
import os
import time
from datetime import datetime, timedelta

from flask import Flask, jsonify, request, render_template_string, redirect

try:
    from zoneinfo import ZoneInfo
    TZ = ZoneInfo("Asia/Shanghai")
except Exception:
    TZ = None  # 回退到服务器本地时区

app = Flask(__name__)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.environ.get("MYNOTE_DB", os.path.join(BASE_DIR, "mynote_stats.db"))
ADMIN_TOKEN = os.environ.get("MYNOTE_ADMIN_TOKEN", "changeme")
app.secret_key = os.environ.get("MYNOTE_SECRET", "mynote-flask-secret")

# 存储层：优先 PostgreSQL（Neon），否则本地 SQLite
DATABASE_URL = os.environ.get("DATABASE_URL", "").strip()
USE_PG = bool(DATABASE_URL)
if USE_PG:
    import psycopg
    from psycopg.rows import dict_row
else:
    import sqlite3

MAX_LEN = 256


def get_db():
    if USE_PG:
        return psycopg.connect(DATABASE_URL, row_factory=dict_row)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA busy_timeout = 5000")
    return conn


def q(sql):
    """把 SQLite 风格 ? 占位符转成 PostgreSQL 的 %s（仅 PG 模式下）。"""
    return sql.replace("?", "%s") if USE_PG else sql


def init_db():
    conn = get_db()
    if USE_PG:
        # psycopg3 的 execute 一次只能执行一条语句，逐条执行
        for stmt in (
            "CREATE TABLE IF NOT EXISTS users ("
            "device_id TEXT PRIMARY KEY, first_seen BIGINT NOT NULL, last_seen BIGINT NOT NULL, "
            "launch_count INTEGER NOT NULL DEFAULT 1, app_version TEXT, android_version TEXT, device_model TEXT)",
            "CREATE TABLE IF NOT EXISTS events ("
            "id BIGSERIAL PRIMARY KEY, device_id TEXT NOT NULL, ts BIGINT NOT NULL, app_version TEXT)",
            "CREATE INDEX IF NOT EXISTS idx_events_ts ON events(ts)",
            "CREATE TABLE IF NOT EXISTS feedback ("
            "id BIGSERIAL PRIMARY KEY, device_id TEXT NOT NULL, title TEXT, content TEXT NOT NULL, "
            "contact TEXT, app_version TEXT, ts BIGINT NOT NULL)",
        ):
            conn.execute(stmt)
    else:
        conn.executescript("""
        CREATE TABLE IF NOT EXISTS users (
            device_id TEXT PRIMARY KEY,
            first_seen INTEGER NOT NULL,
            last_seen INTEGER NOT NULL,
            launch_count INTEGER NOT NULL DEFAULT 1,
            app_version TEXT,
            android_version TEXT,
            device_model TEXT
        );
        CREATE TABLE IF NOT EXISTS events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT NOT NULL,
            ts INTEGER NOT NULL,
            app_version TEXT
        );
        CREATE INDEX IF NOT EXISTS idx_events_ts ON events(ts);
        CREATE TABLE IF NOT EXISTS feedback (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT NOT NULL,
            title TEXT,
            content TEXT NOT NULL,
            contact TEXT,
            app_version TEXT,
            ts INTEGER NOT NULL
        );
        """)
    conn.commit()
    conn.close()


init_db()


def clean(value, n=MAX_LEN):
    if value is None:
        return ""
    return str(value).strip()[:n]


def day_bounds(now):
    """返回「今天 0 点」和「明天 0 点」的 epoch 秒（Asia/Shanghai）。"""
    if TZ:
        d = datetime.fromtimestamp(now, TZ)
    else:
        d = datetime.fromtimestamp(now)
    start = d.replace(hour=0, minute=0, second=0, microsecond=0)
    end = start + timedelta(days=1)
    return int(start.timestamp()), int(end.timestamp())


@app.route("/api/ping", methods=["POST"])
def ping():
    data = request.get_json(silent=True) or {}
    device_id = clean(data.get("device_id"))
    if not device_id:
        return jsonify({"ok": False, "error": "missing device_id"}), 400

    now = int(time.time())
    app_version = clean(data.get("app_version"), 32)
    android_version = clean(data.get("android_version"), 32)
    device_model = clean(data.get("device_model"), 64)

    conn = get_db()
    exists = conn.execute(
        q("SELECT 1 FROM users WHERE device_id = ?"), (device_id,)
    ).fetchone()
    is_new = exists is None
    if is_new:
        conn.execute(
            q("INSERT INTO users (device_id, first_seen, last_seen, launch_count, app_version, android_version, device_model) "
              "VALUES (?, ?, ?, 1, ?, ?, ?)"),
            (device_id, now, now, app_version, android_version, device_model),
        )
    else:
        conn.execute(
            q("UPDATE users SET last_seen = ?, launch_count = launch_count + 1, app_version = ?, android_version = ?, device_model = ? "
              "WHERE device_id = ?"),
            (now, app_version, android_version, device_model, device_id),
        )
    conn.execute(
        q("INSERT INTO events (device_id, ts, app_version) VALUES (?, ?, ?)"),
        (device_id, now, app_version),
    )
    conn.commit()
    conn.close()
    return jsonify({"ok": True, "is_new": is_new})


@app.route("/api/feedback", methods=["POST"])
def feedback():
    data = request.get_json(silent=True) or {}
    content = clean(data.get("content"), 4000)
    if not content:
        return jsonify({"ok": False, "error": "missing content"}), 400

    device_id = clean(data.get("device_id"))
    title = clean(data.get("title"), 200)
    contact = clean(data.get("contact"), 200)
    app_version = clean(data.get("app_version"), 32)

    conn = get_db()
    conn.execute(
        q("INSERT INTO feedback (device_id, title, content, contact, app_version, ts) VALUES (?, ?, ?, ?, ?, ?)"),
        (device_id, title, content, contact, app_version, int(time.time())),
    )
    conn.commit()
    conn.close()
    return jsonify({"ok": True})


def compute_stats():
    conn = get_db()
    now = int(time.time())
    day_start, day_end = day_bounds(now)
    week_start = day_start - 6 * 86400  # 最近 7 天（含今天）
    month_start = day_start - 29 * 86400  # 最近 30 天

    total_users = conn.execute(q("SELECT COUNT(*) AS c FROM users")).fetchone()["c"]
    new_today = conn.execute(
        q("SELECT COUNT(*) AS c FROM users WHERE first_seen >= ?"), (day_start,)
    ).fetchone()["c"]
    new_week = conn.execute(
        q("SELECT COUNT(*) AS c FROM users WHERE first_seen >= ?"), (week_start,)
    ).fetchone()["c"]
    dau = conn.execute(
        q("SELECT COUNT(DISTINCT device_id) AS c FROM events WHERE ts >= ?"), (day_start,)
    ).fetchone()["c"]
    mau = conn.execute(
        q("SELECT COUNT(DISTINCT device_id) AS c FROM events WHERE ts >= ?"), (month_start,)
    ).fetchone()["c"]

    by_version = [dict(r) for r in conn.execute(
        q("SELECT app_version AS v, COUNT(*) AS c FROM users "
          "WHERE app_version IS NOT NULL AND app_version != '' "
          "GROUP BY app_version ORDER BY c DESC")
    ).fetchall()]

    by_android = [dict(r) for r in conn.execute(
        q("SELECT android_version AS v, COUNT(*) AS c FROM users "
          "WHERE android_version IS NOT NULL AND android_version != '' "
          "GROUP BY android_version ORDER BY c DESC")
    ).fetchall()]

    by_model = [dict(r) for r in conn.execute(
        q("SELECT device_model AS v, COUNT(*) AS c FROM users "
          "WHERE device_model IS NOT NULL AND device_model != '' "
          "GROUP BY device_model ORDER BY c DESC LIMIT 10")
    ).fetchall()]

    # 最近 30 天每日活跃（用于趋势）
    daily = []
    for i in range(29, -1, -1):
        s = day_start - i * 86400
        e = s + 86400
        cnt = conn.execute(
            q("SELECT COUNT(DISTINCT device_id) AS c FROM events WHERE ts >= ? AND ts < ?"), (s, e)
        ).fetchone()["c"]
        label = datetime.fromtimestamp(s, TZ).strftime("%m-%d") if TZ else datetime.fromtimestamp(s).strftime("%m-%d")
        daily.append({"date": label, "count": cnt})

    feedbacks = [dict(r) for r in conn.execute(
        q("SELECT * FROM feedback ORDER BY ts DESC LIMIT 50")
    ).fetchall()]
    conn.close()

    def fmt_ts(ts):
        try:
            if TZ:
                return datetime.fromtimestamp(int(ts), TZ).strftime("%Y-%m-%d %H:%M")
            return datetime.fromtimestamp(int(ts)).strftime("%Y-%m-%d %H:%M")
        except Exception:
            return ""

    return {
        "total_users": total_users,
        "new_today": new_today,
        "new_week": new_week,
        "dau": dau,
        "mau": mau,
        "by_version": by_version,
        "by_android": by_android,
        "by_model": by_model,
        "daily": daily,
        "feedbacks": [
            {
                "title": f.get("title") or "(无标题)",
                "content": f.get("content") or "",
                "contact": f.get("contact") or "",
                "app_version": f.get("app_version") or "",
                "ts": fmt_ts(f.get("ts")),
            }
            for f in feedbacks
        ],
    }


def authorized():
    token = request.args.get("token") or request.headers.get("X-Admin-Token")
    return token == ADMIN_TOKEN


@app.route("/api/stats")
def stats():
    if not authorized():
        return jsonify({"error": "unauthorized"}), 401
    return jsonify(compute_stats())


DASHBOARD_HTML = r"""
<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>MyNote 使用统计</title>
<style>
  :root { --bg:#f5f5f9; --card:#ffffff; --text:#1f1f2e; --muted:#8a8aa0; --accent:#7C4DFF; --border:#ececf3; }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: -apple-system, "Segoe UI", "Microsoft YaHei", sans-serif; background: var(--bg); color: var(--text); padding: 24px; }
  h1 { font-size: 20px; margin-bottom: 4px; }
  .sub { color: var(--muted); font-size: 13px; margin-bottom: 20px; }
  .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 14px; margin-bottom: 24px; }
  .card { background: var(--card); border: 1px solid var(--border); border-radius: 14px; padding: 18px; }
  .card .num { font-size: 30px; font-weight: 700; color: var(--accent); }
  .card .lbl { color: var(--muted); font-size: 13px; margin-top: 4px; }
  h2 { font-size: 16px; margin: 24px 0 12px; }
  table { width: 100%; border-collapse: collapse; background: var(--card); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
  th, td { text-align: left; padding: 10px 14px; border-bottom: 1px solid var(--border); font-size: 13px; }
  th { background: #fafaff; color: var(--muted); font-weight: 600; }
  tr:last-child td { border-bottom: none; }
  .fb-content { white-space: pre-wrap; max-width: 560px; }
  .empty { color: var(--muted); font-size: 13px; padding: 12px 0; }
</style>
</head>
<body>
  <h1>MyNote 使用统计</h1>
  <div class="sub">仅匿名统计，不含任何个人身份信息</div>

  <div class="grid">
    <div class="card"><div class="num">{{ s.total_users }}</div><div class="lbl">累计用户</div></div>
    <div class="card"><div class="num">{{ s.dau }}</div><div class="lbl">今日活跃</div></div>
    <div class="card"><div class="num">{{ s.mau }}</div><div class="lbl">近30天活跃</div></div>
    <div class="card"><div class="num">{{ s.new_today }}</div><div class="lbl">今日新增</div></div>
    <div class="card"><div class="num">{{ s.new_week }}</div><div class="lbl">近7天新增</div></div>
  </div>

  <h2>近30天活跃趋势</h2>
  {% if s.daily %}
  <div class="grid">
    {% for d in s.daily %}
    <div class="card"><div class="num" style="font-size:22px">{{ d.count }}</div><div class="lbl">{{ d.date }}</div></div>
    {% endfor %}
  </div>
  {% else %}
  <div class="empty">暂无数据</div>
  {% endif %}

  <h2>版本分布</h2>
  {% if s.by_version %}
  <table><tr><th>版本</th><th>用户数</th></tr>
    {% for v in s.by_version %}<tr><td>{{ v.v }}</td><td>{{ v.c }}</td></tr>{% endfor %}
  </table>
  {% else %}<div class="empty">暂无数据</div>{% endif %}

  <h2>机型分布（Top 10）</h2>
  {% if s.by_model %}
  <table><tr><th>机型</th><th>用户数</th></tr>
    {% for v in s.by_model %}<tr><td>{{ v.v }}</td><td>{{ v.c }}</td></tr>{% endfor %}
  </table>
  {% else %}<div class="empty">暂无数据</div>{% endif %}

  <h2>Android 版本分布</h2>
  {% if s.by_android %}
  <table><tr><th>Android</th><th>用户数</th></tr>
    {% for v in s.by_android %}<tr><td>{{ v.v }}</td><td>{{ v.c }}</td></tr>{% endfor %}
  </table>
  {% else %}<div class="empty">暂无数据</div>{% endif %}

  <h2>用户反馈（最近 50 条）</h2>
  {% if s.feedbacks %}
  <table><tr><th>时间</th><th>版本</th><th>标题</th><th>内容</th><th>联系方式</th></tr>
    {% for f in s.feedbacks %}
    <tr>
      <td style="white-space:nowrap">{{ f.ts }}</td>
      <td>{{ f.app_version }}</td>
      <td>{{ f.title }}</td>
      <td class="fb-content">{{ f.content }}</td>
      <td>{{ f.contact }}</td>
    </tr>
    {% endfor %}
  </table>
  {% else %}<div class="empty">暂无反馈</div>{% endif %}
</body>
</html>
"""

LOGIN_HTML = r"""
<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>登录 - MyNote 统计</title>
<style>
  body { font-family: -apple-system, "Segoe UI", "Microsoft YaHei", sans-serif; background:#f5f5f9; display:flex; align-items:center; justify-content:center; height:100vh; }
  form { background:#fff; padding:28px; border-radius:14px; border:1px solid #ececf3; width: 300px; }
  h1 { font-size:18px; margin-bottom:16px; }
  input { width:100%; padding:10px; border:1px solid #ddd; border-radius:8px; margin-bottom:12px; font-size:14px; }
  button { width:100%; padding:10px; background:#7C4DFF; color:#fff; border:none; border-radius:8px; font-size:14px; cursor:pointer; }
  .err { color:#e53935; font-size:12px; margin-bottom:8px; }
</style>
</head>
<body>
  <form method="post" action="/login">
    <h1>MyNote 统计看板</h1>
    {% if err %}<div class="err">{{ err }}</div>{% endif %}
    <input type="password" name="token" placeholder="输入管理令牌" autofocus>
    <button type="submit">登录</button>
  </form>
</body>
</html>
"""


@app.route("/", methods=["GET"])
def dashboard():
    if not authorized():
        return render_template_string(LOGIN_HTML, err=None)
    return render_template_string(DASHBOARD_HTML, s=compute_stats())


@app.route("/login", methods=["POST"])
def login():
    token = request.form.get("token", "")
    if token == ADMIN_TOKEN:
        return redirect("/?token=" + token)
    return render_template_string(LOGIN_HTML, err="令牌错误，请重试")


@app.route("/healthz")
def healthz():
    return "ok"


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port, debug=False)
