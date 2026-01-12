# DBMS 项目分工方案（7人团队）

## 分工原则

1. **模块独立性**：每个成员负责相对独立的模块，减少代码冲突
2. **工作量均衡**：根据模块复杂度合理分配任务
3. **技能匹配**：考虑不同模块的技术要求
4. **协作便利**：相关模块分配给相邻成员，便于沟通

---

## 详细分工

### 👤 成员1：数据模型层（Model Layer）

**负责模块**：`src/main/java/com/dbms/model/`

**具体文件**：
- `FieldType.java` - 字段类型枚举
- `Field.java` - 字段定义类
- `Table.java` - 表结构类
- `Record.java` - 记录类
- `Database.java` - 数据库类

**主要职责**：
- 设计和实现核心数据模型
- 定义字段类型、表结构、记录的数据结构
- 实现数据模型的基本操作方法（添加字段、获取字段等）
- 确保数据模型的序列化支持

**技术要点**：
- Java 基础类和枚举
- 序列化接口（Serializable）
- 集合类（List, Map）的使用
- 数据验证逻辑

**工作量评估**：⭐⭐（中等，基础但重要）

**与其他模块的接口**：
- 与存储层：提供可序列化的数据模型
- 与执行引擎：提供表结构和记录对象

---

### 👤 成员2：存储层（Storage Layer）

**负责模块**：`src/main/java/com/dbms/storage/`

**具体文件**：
- `FileFormat.java` - 文件格式常量定义
- `BinarySerializer.java` - 二进制序列化工具
- `DBFFileManager.java` - .dbf文件管理器（表结构文件）
- `DATFileManager.java` - .dat文件管理器（数据文件）

**主要职责**：
- 设计自定义文件格式（.dbf 和 .dat）
- 实现二进制序列化/反序列化
- 实现表结构文件的读写操作
- 实现数据记录的读写操作
- 处理文件I/O异常

**技术要点**：
- `RandomAccessFile` 的使用
- `DataInputStream` / `DataOutputStream` 的使用
- 二进制文件格式设计
- UTF-8 字符串编码处理
- 大端序（Big-Endian）字节序

**工作量评估**：⭐⭐⭐（较高，核心存储逻辑）

**与其他模块的接口**：
- 与数据模型层：序列化/反序列化模型对象
- 与执行引擎：提供文件读写接口

---

### 👤 成员3：SQL解析层（Parser Layer）

**负责模块**：`src/main/java/com/dbms/parser/`

**具体文件**：
- `SQLLexer.java` - SQL词法分析器
- `SQLParser.java` - SQL语法分析器

**主要职责**：
- 实现SQL词法分析（Token识别）
- 实现SQL语法分析（递归下降解析器）
- 解析各种SQL语句（CREATE, ALTER, DROP, INSERT, UPDATE, DELETE, SELECT）
- 构建抽象语法树（AST）
- 错误处理和异常报告

**技术要点**：
- 词法分析（有限状态自动机）
- 语法分析（递归下降解析）
- 抽象语法树（AST）设计
- 错误定位和报告

**工作量评估**：⭐⭐⭐⭐（很高，最复杂的模块之一）

**与其他模块的接口**：
- 与执行引擎：提供解析后的SQL语句对象
- 与工具类：使用SQLException报告错误

---

### 👤 成员4：DDL执行引擎 + 工具类（DDL Executor + Utilities）

**负责模块**：
- `src/main/java/com/dbms/engine/DDLExecutor.java`
- `src/main/java/com/dbms/util/`

**具体文件**：
- `DDLExecutor.java` - DDL语句执行器
- `TypeConverter.java` - 类型转换工具
- `Validator.java` - 数据验证工具
- `DBMSException.java` - DBMS异常类
- `SQLException.java` - SQL异常类

**主要职责**：
- 实现DDL语句的执行（CREATE TABLE, ALTER TABLE, DROP TABLE, RENAME TABLE）
- 实现类型转换（字符串到各种数据类型）
- 实现数据验证（NOT NULL, PRIMARY KEY等约束）
- 定义异常类体系

**技术要点**：
- DDL操作的业务逻辑
- 类型转换和验证
- 异常处理机制
- 与存储层的协作

**工作量评估**：⭐⭐⭐（较高）

**与其他模块的接口**：
- 与数据模型层：创建和修改Table对象
- 与存储层：调用文件管理器保存表结构
- 与SQL解析层：接收解析后的DDL语句

---

### 👤 成员5：DML执行引擎（DML Executor）

**负责模块**：`src/main/java/com/dbms/engine/DMLExecutor.java`

**具体文件**：
- `DMLExecutor.java` - DML语句执行器

**主要职责**：
- 实现INSERT语句的执行
- 实现UPDATE语句的执行
- 实现DELETE语句的执行
- 实现WHERE条件的匹配逻辑
- 处理数据验证和类型转换

**技术要点**：
- DML操作的业务逻辑
- WHERE条件的解析和匹配
- 记录的位置定位和更新
- 逻辑删除的实现

**工作量评估**：⭐⭐⭐（较高）

**与其他模块的接口**：
- 与数据模型层：操作Record对象
- 与存储层：调用文件管理器读写记录
- 与工具类：使用TypeConverter和Validator
- 与SQL解析层：接收解析后的DML语句

---

### 👤 成员6：查询执行引擎 + SQL统一入口（Query Executor + SQL Executor）

**负责模块**：
- `src/main/java/com/dbms/engine/QueryExecutor.java`
- `src/main/java/com/dbms/engine/SQLExecutor.java`

**具体文件**：
- `QueryExecutor.java` - 查询语句执行器
- `SQLExecutor.java` - SQL统一执行入口

**主要职责**：
- 实现SELECT语句的执行
- 实现单表查询
- 实现多表JOIN查询（嵌套循环连接）
- 实现WHERE条件过滤
- 实现字段投影（SELECT子句）
- 实现SQL语句的统一分发（根据SQL类型调用对应的执行器）

**技术要点**：
- 查询算法（全表扫描、嵌套循环连接）
- 条件匹配和过滤
- 结果集构建
- 表别名处理
- 多执行器的协调

**工作量评估**：⭐⭐⭐⭐（很高，特别是JOIN查询）

**与其他模块的接口**：
- 与数据模型层：读取Record对象
- 与存储层：调用文件管理器读取记录
- 与SQL解析层：接收解析后的SELECT语句
- 与其他执行器：通过SQLExecutor协调

---

### 👤 成员7：用户界面层 + 文档（UI Layer + Documentation）

**负责模块**：
- `src/main/java/com/dbms/ui/MainFrame.java`
- `docs/` 目录下的所有文档

**具体文件**：
- `MainFrame.java` - 主窗口UI
- `README.md` - 项目说明文档
- `docs/TECHNICAL_DOCUMENTATION.md` - 技术文档
- 其他文档文件

**主要职责**：
- 设计和实现Swing图形界面
- 实现SQL编辑器
- 实现表列表、表结构、表数据的展示
- 实现查询结果的展示
- 处理用户交互事件
- 维护和更新项目文档
- 编写使用说明和示例

**技术要点**：
- Java Swing GUI编程
- 布局管理器（JSplitPane, BorderLayout等）
- 事件处理（ActionListener, ListSelectionListener）
- 表格组件（JTable, DefaultTableModel）
- 字体和样式处理
- Markdown文档编写

**工作量评估**：⭐⭐⭐（较高，UI代码量大）

**与其他模块的接口**：
- 与所有执行器：调用SQLExecutor执行SQL
- 与数据模型层：显示Table和Record数据
- 与存储层：间接通过执行器访问

---

## 协作流程

### 开发顺序建议

1. **第一阶段（基础搭建）**：
   - 成员1：完成数据模型层
   - 成员2：完成存储层（与成员1协作定义序列化接口）
   - 成员4：完成工具类

2. **第二阶段（核心功能）**：
   - 成员3：完成SQL解析层
   - 成员4：完成DDL执行引擎
   - 成员5：完成DML执行引擎
   - 成员6：完成查询执行引擎和SQL统一入口

3. **第三阶段（集成测试）**：
   - 成员7：完成UI界面
   - 所有成员：集成测试和Bug修复
   - 成员7：完善文档

### 接口约定

**数据模型层接口**：
- `Table` 类需要实现 `Serializable`
- `Record` 类需要提供 `getValue()` 和 `setValue()` 方法
- `Database` 类需要提供 `getTable()` 和 `addTable()` 方法

**存储层接口**：
- `DBFFileManager` 需要提供 `readDatabaseFile()` 和 `createDatabaseFile()` 方法
- `DATFileManager` 需要提供 `readAllRecords()` 和 `appendRecord()` 方法

**解析层接口**：
- `SQLParser` 需要返回统一的语句对象（如 `CreateTableStatement`, `SelectStatement` 等）

**执行引擎接口**：
- 所有执行器需要统一的异常处理
- `SQLExecutor` 作为统一入口，负责分发SQL语句

---

## 代码规范

1. **命名规范**：
   - 类名：大驼峰（PascalCase）
   - 方法名：小驼峰（camelCase）
   - 常量：全大写下划线分隔（UPPER_SNAKE_CASE）

2. **注释要求**：
   - 所有公共类和方法必须有JavaDoc注释
   - 复杂逻辑必须有行内注释

3. **异常处理**：
   - 使用自定义异常（`DBMSException`, `SQLException`）
   - 异常信息要清晰明确

4. **代码提交**：
   - 每次提交前进行本地测试
   - 提交信息要清晰描述改动内容

---

## 测试建议

### 单元测试
- 每个模块完成后，编写对应的单元测试
- 测试用例覆盖正常流程和异常流程

### 集成测试
- 各模块完成后，进行端到端测试
- 测试完整的SQL语句执行流程

### 测试用例示例
- CREATE TABLE → INSERT → SELECT → UPDATE → DELETE
- 多表JOIN查询
- 异常SQL语句的错误处理

---

## 时间安排建议

### 第1-2周：基础模块
- 成员1：数据模型层
- 成员2：存储层
- 成员4：工具类

### 第3-4周：核心功能
- 成员3：SQL解析层
- 成员4：DDL执行引擎
- 成员5：DML执行引擎
- 成员6：查询执行引擎

### 第5-6周：集成与完善
- 成员7：UI界面
- 所有成员：集成测试
- 成员7：文档完善

---

## 常见问题处理

### 代码冲突
- 使用Git分支开发，定期合并
- 接口变更需要提前通知相关成员

### 接口不一致
- 定期进行代码评审
- 使用接口文档明确约定

### 进度不一致
- 每周进行进度同步
- 提前识别风险模块，及时调整分工

---

## 联系方式与协作工具

- **代码仓库**：Git（建议使用GitHub/GitLab）
- **文档协作**：Markdown文件，建议使用Git管理
- **沟通工具**：建议使用微信群/QQ群/钉钉等
- **项目管理**：建议使用Trello/Notion等工具跟踪进度

---

*最后更新：2024年*

