# DBMS 项目分工表（7人团队 - 快速参考）

## 📋 分工概览

| 成员 | 负责模块 | 主要文件 | 工作量 | 优先级 |
|------|---------|---------|--------|--------|
| **成员1** | 数据模型层 | FieldType, Field, Table, Record, Database | ⭐⭐ | 高 |
| **成员2** | 存储层 | FileFormat, BinarySerializer, DBFFileManager, DATFileManager | ⭐⭐⭐ | 高 |
| **成员3** | SQL解析层 | SQLLexer, SQLParser | ⭐⭐⭐⭐ | 高 |
| **成员4** | DDL执行引擎 + 工具类 | DDLExecutor, TypeConverter, Validator, 异常类 | ⭐⭐⭐ | 高 |
| **成员5** | DML执行引擎 | DMLExecutor | ⭐⭐⭐ | 中 |
| **成员6** | 查询执行引擎 + SQL统一入口 | QueryExecutor, SQLExecutor | ⭐⭐⭐⭐ | 中 |
| **成员7** | 用户界面 + 文档 | MainFrame, 所有文档 | ⭐⭐⭐ | 中 |

---

## 🎯 详细分工

### 👤 成员1：数据模型层（Model Layer）
```
src/main/java/com/dbms/model/
├── FieldType.java      (字段类型枚举)
├── Field.java          (字段定义)
├── Table.java          (表结构)
├── Record.java         (记录)
└── Database.java       (数据库)
```
**核心任务**：定义所有数据模型的基础结构

---

### 👤 成员2：存储层（Storage Layer）
```
src/main/java/com/dbms/storage/
├── FileFormat.java         (文件格式常量)
├── BinarySerializer.java   (二进制序列化)
├── DBFFileManager.java     (.dbf文件管理)
└── DATFileManager.java     (.dat文件管理)
```
**核心任务**：实现文件I/O和序列化

---

### 👤 成员3：SQL解析层（Parser Layer）
```
src/main/java/com/dbms/parser/
├── SQLLexer.java      (词法分析)
└── SQLParser.java     (语法分析)
```
**核心任务**：将SQL文本解析为可执行对象

---

### 👤 成员4：DDL执行引擎 + 工具类
```
src/main/java/com/dbms/engine/
└── DDLExecutor.java   (DDL执行器)

src/main/java/com/dbms/util/
├── TypeConverter.java    (类型转换)
├── Validator.java        (数据验证)
├── DBMSException.java    (异常类)
└── SQLException.java     (SQL异常)
```
**核心任务**：执行DDL语句 + 提供工具类支持

---

### 👤 成员5：DML执行引擎
```
src/main/java/com/dbms/engine/
└── DMLExecutor.java   (DML执行器)
```
**核心任务**：执行INSERT、UPDATE、DELETE语句

---

### 👤 成员6：查询执行引擎 + SQL统一入口
```
src/main/java/com/dbms/engine/
├── QueryExecutor.java   (查询执行器)
└── SQLExecutor.java     (SQL统一入口)
```
**核心任务**：执行SELECT查询 + 协调所有执行器

---

### 👤 成员7：用户界面 + 文档
```
src/main/java/com/dbms/ui/
└── MainFrame.java   (主窗口)

docs/
├── README.md
├── TECHNICAL_DOCUMENTATION.md
└── TEAM_DIVISION.md
```
**核心任务**：实现GUI界面 + 维护文档

---

## 🔄 开发顺序

### 阶段1：基础搭建（第1-2周）
1. ✅ 成员1：数据模型层
2. ✅ 成员2：存储层（与成员1协作）
3. ✅ 成员4：工具类

### 阶段2：核心功能（第3-4周）
4. ✅ 成员3：SQL解析层
5. ✅ 成员4：DDL执行引擎
6. ✅ 成员5：DML执行引擎
7. ✅ 成员6：查询执行引擎

### 阶段3：集成完善（第5-6周）
8. ✅ 成员7：UI界面
9. ✅ 全体：集成测试
10. ✅ 成员7：文档完善

---

## 🔗 模块依赖关系

```
数据模型层 (成员1)
    ↓
存储层 (成员2) ← 工具类 (成员4)
    ↓
SQL解析层 (成员3)
    ↓
执行引擎层 (成员4,5,6)
    ↓
用户界面层 (成员7)
```

---

## ⚠️ 关键接口约定

### 数据模型层 → 存储层
- `Table` 和 `Record` 必须实现 `Serializable`
- `Database` 提供 `getTable(String name)` 方法

### 存储层 → 执行引擎
- `DBFFileManager.readDatabaseFile()` 返回 `Database` 对象
- `DATFileManager.readAllRecords()` 返回 `List<Record>`

### 解析层 → 执行引擎
- `SQLParser.parse()` 返回 `SQLStatement` 子类对象
- 各执行器接收对应的Statement对象

### 执行引擎 → UI
- `SQLExecutor.execute(String sql)` 返回执行结果字符串

---

## 📞 协作要点

1. **接口变更**：任何接口变更必须提前通知相关成员
2. **代码提交**：提交前进行本地测试，提交信息清晰
3. **进度同步**：每周进行进度同步，及时识别风险
4. **代码评审**：关键模块完成后进行代码评审

---

## 📚 相关文档

- 详细分工说明：`docs/TEAM_DIVISION.md`
- 技术文档：`docs/TECHNICAL_DOCUMENTATION.md`
- 项目说明：`README.md`

---

*快速参考版本 - 详细说明请查看 TEAM_DIVISION.md*

