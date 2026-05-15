# 心屿 - 心灵港湾

一款面向大学生的心理健康辅助 Web 应用平台，集 AI 情感对话、心情记录追踪、标准化心理量表测评、匿名社区分享、情感互助匹配、学习辅助工具于一体。

## 功能模块

| 模块 | 说明 |
|------|------|
| AI 情感助手 | 基于小米 MiMo 大模型的 AI 对话，温暖陪伴 |
| 情绪中心 | 心情转盘 + 情绪趋势图 + 美食推荐 |
| 心理测评 | PHQ-9 / GAD-7 / PSS / 睡眠质量 4 份标准量表 |
| 社区广场 | 匿名树洞倾诉 + 情感互助匹配聊天 |
| 学习广场 | 笔记 / 番茄钟 / 课表管理 |
| 治愈信箱 | 给未来的自己写一封信 |
| 虚拟桌宠 | 陪伴式互动，等级与情绪联动 |
| 管理后台 | 用户管理 / 数据统计 / 内容审核 |

## 技术栈

**前端**
- HTML5 / CSS3 / JavaScript (ES6+)
- 单页应用 (SPA)，无框架依赖
- 响应式设计，适配手机和桌面端

**后端**
- Python 3.12 + Flask
- SQLite3 数据库
- bcrypt 密码加密
- Flask-Session / Flask-Limiter
- 约 45 个 RESTful API 接口

**AI**
- 小米 MiMo v2.5 大语言模型 API
- System Prompt 角色设定
- 情绪风险检测与专业建议引导

## 快速开始

```bash
# 1. 安装依赖
cd backend
pip install -r requirements.txt

# 2. 启动后端
python app.py

# 3. 打开浏览器访问
# http://localhost:5000
```

## 项目结构

```
xinyu/
├── backend/                # Flask 后端
│   ├── app.py             # 主应用 (约 45 个 API)
│   ├── requirements.txt   # Python 依赖
│   ├── static/            # 静态资源
│   └── templates/         # HTML 模板
├── frontend/               # Web 前端
│   ├── index.html         # 主页面
│   ├── script.js          # 核心逻辑
│   ├── style.css          # 样式
│   └── db/                # 本地数据
├── android/                # Android 客户端
├── docs/                   # 项目文档
│   ├── 作品说明.md
│   ├── 技术文档.md
│   ├── AI技术说明.md
│   ├── 技术路线.md
│   └── 项目经历.md
└── README.md
```

## 核心 API 接口

| 模块 | 接口数 | 说明 |
|------|--------|------|
| 用户管理 | 8 | 注册、登录、验证、安全问题 |
| 心情管理 | 2 | 记录与查询 |
| AI 对话 | 2 | 发送消息、获取历史 |
| 社区功能 | 8 | 帖子 CRUD、点赞、评论 |
| 情感互助 | 10 | 档案、匹配、聊天、举报 |
| 测评系统 | 2 | 结果同步、历史查询 |
| 管理后台 | 12 | 数据统计、用户/内容管理 |

## 数据库

共 14 个核心数据表：

- `server_users` — 用户表
- `moods` — 心情记录
- `checkins` — 打卡记录
- `chat_history` — AI 对话历史
- `anonymous_posts` — 匿名帖子
- `anonymous_replies` — 帖子评论
- `anonymous_likes` — 点赞记录
- `user_devices` — 设备用户
- `user_events` — 用户事件
- `test_results_sync` — 测评结果
- `support_profiles` — 互助档案
- `support_matches` — 互助匹配
- `support_messages` — 互助消息
- `students` — 学生信息

## 部署

- Ubuntu Linux 22.04 LTS
- Nginx 反向代理
- Gunicorn WSGI 服务器
- Systemd 服务管理

## 文档

详细文档见 `docs/` 目录：
- [作品说明](docs/作品说明.md)
- [技术文档](docs/技术文档.md)
- [AI 技术说明](docs/AI技术说明.md)
- [技术路线](docs/技术路线.md)
- [项目经历](docs/项目经历.md)

## 让每一颗心都被温柔以待
