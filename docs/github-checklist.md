# GitHub 上传前检查清单

## 一定不要上传

- `backend/.env`
- 任何真实 `AI_API_KEY`
- `frontend/node_modules/`
- `frontend/dist/`
- `backend/target/`
- `backend/data/`
- `*.log`
- 个人真实简历、身份证照片、隐私截图

## 当前 `.gitignore` 已覆盖

- `.env`
- `node_modules/`
- `dist/`
- `target/`
- `*.log`
- `backend/data/`
- `.idea/`
- `.vscode/`

## 上传前建议执行

```powershell
cd C:\Users\Administrator\CareerPilot
git status
```

重点检查：

- 是否出现 `.env`。
- 是否出现 `node_modules`。
- 是否出现 `target`。
- 是否出现 `backend/data`。
- 是否有真实手机号、Key、个人隐私截图。

## 推荐提交结构

```text
CareerPilot
├── backend
├── frontend
├── docs
│   ├── api.md
│   ├── database.md
│   ├── demo-script.md
│   ├── interview-guide.md
│   ├── github-checklist.md
│   └── project-status.md
├── .gitignore
└── README.md
```

## GitHub README 展示建议

上传后可以在 README 补充：

- 项目截图。
- 演示视频链接。
- 在线体验地址。
- 你的简历项目描述。

## 推荐截图

新建目录：

```text
docs/images/
```

建议放：

- `login.png`
- `workspace.png`
- `analysis.png`
- `resume-editor.png`
- `records.png`

