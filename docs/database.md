# 数据库设计与 MySQL 迁移说明

## 当前数据库方案

项目当前默认使用 H2 文件数据库，方便本地快速运行和招聘会现场演示。

配置位置：

```text
backend/src/main/resources/application.yml
```

默认连接：

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/careerpilot;MODE=MySQL;DATABASE_TO_LOWER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update
```

说明：

- `MODE=MySQL`：让 H2 尽量兼容 MySQL 语法。
- `ddl-auto=update`：由 JPA 根据实体自动更新表结构。
- 数据文件位于 `backend/data/`，该目录已加入 `.gitignore`，不建议上传 GitHub。

## 核心表

### 1. `app_user`

用户表，用于注册登录和历史记录用户隔离。

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键，自增 |
| `username` | VARCHAR(40) | 用户名，唯一 |
| `phone` | VARCHAR(20) | 手机号，唯一 |
| `password_hash` | VARCHAR(255) | 加盐哈希后的密码 |
| `salt` | VARCHAR(255) | 密码盐值 |
| `created_at` | DATETIME | 创建时间 |

### 2. `analysis_record`

分析记录表，用于保存 AI 简历分析结果。

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键，自增 |
| `resume_name` | VARCHAR(255) | 简历名称 |
| `user_id` | BIGINT | 用户 ID，未登录时可为空 |
| `match_score` | INT | 岗位匹配分数 |
| `summary` | VARCHAR(1000) | 分析摘要 |
| `resume_text` | TEXT | 简历正文 |
| `job_description` | TEXT | 岗位 JD |
| `result_json` | TEXT | 完整分析结果 JSON |
| `created_at` | DATETIME | 创建时间 |

## MySQL 建表 SQL

如果后续切换 MySQL，可以使用以下结构作为第一版：

```sql
CREATE TABLE app_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(40) NOT NULL UNIQUE,
  phone VARCHAR(20) UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  salt VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE analysis_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resume_name VARCHAR(255),
  user_id BIGINT,
  match_score INT,
  summary VARCHAR(1000),
  resume_text TEXT,
  job_description TEXT,
  result_json TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_analysis_user_created (user_id, created_at DESC),
  INDEX idx_analysis_created (created_at DESC)
);
```

## MySQL 迁移步骤

### 1. 添加 MySQL 驱动

在 `backend/pom.xml` 中加入：

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. 修改数据库配置

建议通过环境变量管理：

```yaml
spring:
  datasource:
    url: ${MYSQL_URL:jdbc:mysql://localhost:3306/careerpilot?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:123456}
```

### 3. 创建数据库

```sql
CREATE DATABASE careerpilot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 4. 推荐生产配置

开发阶段可以继续使用：

```yaml
spring.jpa.hibernate.ddl-auto: update
```

正式部署建议改为：

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

并使用 SQL 脚本或迁移工具维护表结构。

## 面试讲解要点

- 当前项目为了演示方便使用 H2 文件数据库，但实体和 JPA 设计已经按关系型数据库建模。
- `analysis_record` 通过 `user_id` 和 `created_at` 支持“按用户查询最近记录”的业务场景。
- 切换 MySQL 的核心工作是引入驱动、修改数据源配置、创建数据库和索引。
- 如果数据量增加，可以重点优化 `idx_analysis_user_created`，因为历史记录列表通常按用户和时间倒序查询。

