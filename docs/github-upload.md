# GitHub 上传步骤

当前项目目录：

```powershell
C:\Users\Administrator\CareerPilot
```

## 1. 上传前检查

```powershell
cd C:\Users\Administrator\CareerPilot
git status --short --ignored
```

确认以下内容是 `!!` 忽略状态，不要上传：

- `backend/.env`
- `backend/data/`
- `backend/target/`
- `frontend/node_modules/`
- `frontend/dist/`

## 2. 创建 GitHub 仓库

在 GitHub 新建仓库，建议仓库名：

```text
CareerPilot
```

建议描述：

```text
AI 简历分析与模拟面试平台，基于 Spring Boot + Vue3 + OpenAI 兼容接口。
```

不要勾选自动生成 README，因为本地已经有 README。

## 3. 本地提交

```powershell
cd C:\Users\Administrator\CareerPilot
git add .
git status
git commit -m "Initial commit: CareerPilot AI resume platform"
```

## 4. 关联远程仓库

把下面地址换成你自己的 GitHub 仓库地址：

```powershell
git branch -M main
git remote add origin https://github.com/你的GitHub用户名/CareerPilot.git
git push -u origin main
```

## 5. 如果出现 safe.directory 提示

如果 Git 提示 `detected dubious ownership`，用管理员 PowerShell 执行：

```powershell
git config --global --add safe.directory C:/Users/Administrator/CareerPilot
```

然后重新执行 `git status`。

## 6. 上传后检查

打开 GitHub 仓库页面，重点确认：

- README 能正常显示截图。
- 没有上传 `backend/.env`。
- 没有上传 `node_modules`。
- 没有上传 `target`、`dist`、`backend/data`。
- `docs/images/` 中有 5 张项目截图。

