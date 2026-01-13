# SQL测试用例文档

本文档包含各种SQL语句的测试用例，用于全面测试数据库系统的功能。

## 目录

1. [DDL操作（数据定义语言）](#ddl操作数据定义语言)
2. [DML操作（数据操纵语言）](#dml操作数据操纵语言)
3. [单表查询](#单表查询)
4. [多表查询（JOIN）](#多表查询join)
5. [WHERE条件测试](#where条件测试)
6. [边界情况和异常测试](#边界情况和异常测试)

---

## DDL操作（数据定义语言）

### 1. 创建表

#### 1.1 基本表创建
```sql
-- 创建学生表
CREATE TABLE students (
    id INT,
    name VARCHAR(50),
    age INT,
    gender VARCHAR(10),
    email VARCHAR(100)
);
```

#### 1.2 创建课程表
```sql
CREATE TABLE courses (
    course_id INT,
    course_name VARCHAR(100),
    teacher VARCHAR(50),
    credits INT
);
```

#### 1.3 创建选课表
```sql
CREATE TABLE enrollments (
    student_id INT,
    course_id INT,
    grade INT,
    enroll_date VARCHAR(20)
);
```

#### 1.4 创建员工表
```sql
CREATE TABLE employees (
    emp_id INT,
    emp_name VARCHAR(50),
    department VARCHAR(50),
    salary DOUBLE,
    hire_date VARCHAR(20)
);
```

#### 1.5 创建部门表
```sql
CREATE TABLE departments (
    dept_id INT,
    dept_name VARCHAR(50),
    location VARCHAR(100)
);
```

### 2. 修改表结构

#### 2.1 添加字段
```sql
ALTER TABLE students ADD COLUMN phone VARCHAR(20);
```

#### 2.2 删除字段
```sql
ALTER TABLE students DROP COLUMN phone;
```

#### 2.3 修改字段类型
```sql
ALTER TABLE students MODIFY COLUMN age VARCHAR(10);
```

#### 2.4 重命名字段
```sql
ALTER TABLE students RENAME COLUMN email TO email_address;
```

### 3. 删除表

```sql
DROP TABLE students;
DROP TABLE courses;
DROP TABLE enrollments;
```

---

## DML操作（数据操纵语言）

### 1. 插入数据

#### 1.1 插入所有字段
```sql
INSERT INTO students VALUES (1, 'Alice', 20, 'Female', 'alice@example.com');
INSERT INTO students VALUES (2, 'Bob', 21, 'Male', 'bob@example.com');
INSERT INTO students VALUES (3, 'Charlie', 19, 'Male', 'charlie@example.com');
INSERT INTO students VALUES (4, 'Diana', 22, 'Female', 'diana@example.com');
INSERT INTO students VALUES (5, 'Eve', 20, 'Female', 'eve@example.com');
```

#### 1.2 插入指定字段
```sql
INSERT INTO students (id, name, age) VALUES (6, 'Frank', 23);
INSERT INTO students (name, id, age) VALUES ('Grace', 7, 21);
```

#### 1.3 插入课程数据
```sql
INSERT INTO courses VALUES (101, 'Database Systems', 'Dr. Smith', 3);
INSERT INTO courses VALUES (102, 'Operating Systems', 'Dr. Johnson', 4);
INSERT INTO courses VALUES (103, 'Computer Networks', 'Dr. Williams', 3);
INSERT INTO courses VALUES (104, 'Data Structures', 'Dr. Brown', 4);
INSERT INTO courses VALUES (105, 'Algorithms', 'Dr. Davis', 3);
```

#### 1.4 插入选课数据
```sql
INSERT INTO enrollments VALUES (1, 101, 95, '2024-01-15');
INSERT INTO enrollments VALUES (1, 102, 88, '2024-01-16');
INSERT INTO enrollments VALUES (2, 101, 92, '2024-01-15');
INSERT INTO enrollments VALUES (2, 103, 85, '2024-01-17');
INSERT INTO enrollments VALUES (3, 101, 90, '2024-01-15');
INSERT INTO enrollments VALUES (3, 104, 87, '2024-01-18');
INSERT INTO enrollments VALUES (4, 102, 93, '2024-01-16');
INSERT INTO enrollments VALUES (4, 105, 89, '2024-01-19');
INSERT INTO enrollments VALUES (5, 103, 91, '2024-01-17');
INSERT INTO enrollments VALUES (5, 104, 86, '2024-01-18');
```

#### 1.5 插入员工数据
```sql
INSERT INTO employees VALUES (1, 'John', 'IT', 7500.50, '2020-01-10');
INSERT INTO employees VALUES (2, 'Jane', 'HR', 6500.00, '2021-03-15');
INSERT INTO employees VALUES (3, 'Mike', 'IT', 8000.75, '2019-06-20');
INSERT INTO employees VALUES (4, 'Sarah', 'Finance', 7000.25, '2022-01-05');
INSERT INTO employees VALUES (5, 'Tom', 'IT', 7200.00, '2021-09-12');
```

#### 1.6 插入部门数据
```sql
INSERT INTO departments VALUES (1, 'IT', 'Building A');
INSERT INTO departments VALUES (2, 'HR', 'Building B');
INSERT INTO departments VALUES (3, 'Finance', 'Building A');
INSERT INTO departments VALUES (4, 'Marketing', 'Building C');
```

### 2. 更新数据

#### 2.1 更新单个字段
```sql
UPDATE students SET age = 21 WHERE id = 1;
```

#### 2.2 更新多个字段
```sql
UPDATE students SET age = 22, email = 'newemail@example.com' WHERE id = 2;
```

#### 2.3 更新所有记录
```sql
UPDATE students SET age = age + 1;
```

#### 2.4 带条件的更新
```sql
UPDATE students SET age = 25 WHERE age < 20;
UPDATE enrollments SET grade = 100 WHERE grade >= 90;
```

### 3. 删除数据

#### 3.1 删除指定记录
```sql
DELETE FROM students WHERE id = 5;
```

#### 3.2 删除满足条件的记录
```sql
DELETE FROM students WHERE age < 20;
DELETE FROM enrollments WHERE grade < 60;
```

#### 3.3 删除所有记录
```sql
DELETE FROM students;
```

---

## 单表查询

### 1. 基本查询

#### 1.1 查询所有字段
```sql
SELECT * FROM students;
```

#### 1.2 查询指定字段
```sql
SELECT id, name FROM students;
SELECT name, age, email FROM students;
```

#### 1.3 查询单个字段
```sql
SELECT name FROM students;
```

### 2. WHERE条件查询

#### 2.1 等值查询
```sql
SELECT * FROM students WHERE id = 1;
SELECT * FROM students WHERE name = 'Alice';
SELECT * FROM students WHERE age = 20;
```

#### 2.2 不等值查询
```sql
SELECT * FROM students WHERE id != 1;
SELECT * FROM students WHERE age <> 20;
```

#### 2.3 比较查询
```sql
SELECT * FROM students WHERE age > 20;
SELECT * FROM students WHERE age < 22;
SELECT * FROM students WHERE age >= 21;
SELECT * FROM students WHERE age <= 20;
```

#### 2.4 多个AND条件
```sql
SELECT * FROM students WHERE age > 20 AND gender = 'Female';
SELECT * FROM students WHERE id > 2 AND age < 22;
SELECT * FROM students WHERE age >= 20 AND age <= 21;
SELECT name, age FROM students WHERE id > 1 AND age > 19;
```

#### 2.5 多个OR条件（如果支持）
```sql
SELECT * FROM students WHERE age = 20 OR age = 21;
SELECT * FROM students WHERE id = 1 OR id = 3;
```

#### 2.6 混合AND和OR条件（如果支持）
```sql
SELECT * FROM students WHERE (age > 20 AND gender = 'Female') OR id = 1;
```

#### 2.7 LIKE模糊查询
```sql
SELECT * FROM students WHERE name LIKE 'A%';
SELECT * FROM students WHERE email LIKE '%@example.com';
SELECT * FROM students WHERE name LIKE '%li%';
```

#### 2.8 NULL值查询
```sql
SELECT * FROM students WHERE email IS NULL;
SELECT * FROM students WHERE email IS NOT NULL;
```

### 3. 数字类型查询

#### 3.1 整数比较
```sql
SELECT * FROM enrollments WHERE grade >= 90;
SELECT * FROM enrollments WHERE grade < 85;
SELECT * FROM enrollments WHERE grade = 95;
```

#### 3.2 浮点数比较
```sql
SELECT * FROM employees WHERE salary > 7000.0;
SELECT * FROM employees WHERE salary <= 7500.50;
```

---

## 多表查询（JOIN）

### 1. 显式JOIN

#### 1.1 内连接（INNER JOIN）
```sql
SELECT students.name, courses.course_name, enrollments.grade
FROM students
INNER JOIN enrollments ON students.id = enrollments.student_id
INNER JOIN courses ON enrollments.course_id = courses.course_id;
```

#### 1.2 带表别名
```sql
SELECT s.name, c.course_name, e.grade
FROM students s
JOIN enrollments e ON s.id = e.student_id
JOIN courses c ON e.course_id = c.course_id;
```

#### 1.3 使用AS关键字
```sql
SELECT s.name, c.course_name, e.grade
FROM students AS s
JOIN enrollments AS e ON s.id = e.student_id
JOIN courses AS c ON e.course_id = c.course_id;
```

### 2. 隐式JOIN（逗号连接）

#### 2.1 两表隐式JOIN
```sql
SELECT students.name, enrollments.grade
FROM students, enrollments
WHERE students.id = enrollments.student_id;
```

#### 2.2 三表隐式JOIN
```sql
SELECT students.name, courses.course_name, enrollments.grade
FROM students, enrollments, courses
WHERE students.id = enrollments.student_id
  AND enrollments.course_id = courses.course_id;
```

#### 2.3 隐式JOIN带WHERE条件
```sql
SELECT students.name 
FROM students, enrollments
WHERE students.id = enrollments.student_id
  AND enrollments.grade >= 90;
```

#### 2.4 隐式JOIN多个AND条件
```sql
SELECT students.name, courses.course_name, enrollments.grade
FROM students, enrollments, courses
WHERE students.id = enrollments.student_id
  AND enrollments.course_id = courses.course_id
  AND enrollments.grade >= 90
  AND students.age > 20;
```

#### 2.5 隐式JOIN带表别名
```sql
SELECT s.name, c.course_name, e.grade
FROM students s, enrollments e, courses c
WHERE s.id = e.student_id
  AND e.course_id = c.course_id
  AND e.grade >= 90;
```

### 3. 复杂JOIN查询

#### 3.1 多表连接带多个条件
```sql
SELECT s.name, c.course_name, e.grade, c.teacher
FROM students s
JOIN enrollments e ON s.id = e.student_id
JOIN courses c ON e.course_id = c.course_id
WHERE e.grade >= 90 AND s.age > 20;
```

#### 3.2 员工和部门连接
```sql
SELECT e.emp_name, d.dept_name, e.salary
FROM employees e, departments d
WHERE e.department = d.dept_name
  AND e.salary > 7000.0;
```

#### 3.3 使用表名前缀
```sql
SELECT students.name, enrollments.grade
FROM students, enrollments
WHERE students.id = enrollments.student_id
  AND students.age >= 20
  AND enrollments.grade >= 85;
```

### 4. 列名格式测试

#### 4.1 使用表名.列名
```sql
SELECT students.name, enrollments.grade
FROM students, enrollments
WHERE students.id = enrollments.student_id;
```

#### 4.2 使用别名.列名
```sql
SELECT s.name, e.grade
FROM students s, enrollments e
WHERE s.id = e.student_id;
```

#### 4.3 混合使用
```sql
SELECT students.name, e.grade, courses.course_name
FROM students, enrollments e, courses
WHERE students.id = e.student_id
  AND e.course_id = courses.course_id;
```

---

## WHERE条件测试

### 1. 单个条件

#### 1.1 等值条件
```sql
SELECT * FROM students WHERE id = 1;
SELECT * FROM students WHERE name = 'Alice';
```

#### 1.2 不等值条件
```sql
SELECT * FROM students WHERE id != 1;
SELECT * FROM students WHERE age <> 20;
```

#### 1.3 比较条件
```sql
SELECT * FROM students WHERE age > 20;
SELECT * FROM students WHERE age < 22;
SELECT * FROM students WHERE age >= 21;
SELECT * FROM students WHERE age <= 20;
```

### 2. 多个AND条件

#### 2.1 两个AND条件
```sql
SELECT * FROM students WHERE id > 1 AND age > 20;
SELECT * FROM students WHERE age >= 20 AND age <= 21;
SELECT name, age FROM students WHERE id > 1 AND age > 19;
```

#### 2.2 三个AND条件
```sql
SELECT * FROM students WHERE id > 1 AND age > 20 AND gender = 'Female';
SELECT * FROM enrollments WHERE student_id > 1 AND course_id > 101 AND grade >= 90;
```

#### 2.3 多表AND条件
```sql
SELECT students.name 
FROM students, enrollments
WHERE students.id = enrollments.student_id
  AND enrollments.grade >= 90;
```

#### 2.4 复杂AND条件
```sql
SELECT s.name, c.course_name, e.grade
FROM students s, enrollments e, courses c
WHERE s.id = e.student_id
  AND e.course_id = c.course_id
  AND e.grade >= 90
  AND s.age > 20
  AND c.credits >= 3;
```

### 3. 字符串条件

#### 3.1 字符串等值
```sql
SELECT * FROM students WHERE name = 'Alice';
SELECT * FROM students WHERE gender = 'Female';
```

#### 3.2 字符串比较
```sql
SELECT * FROM students WHERE name > 'Bob';
SELECT * FROM students WHERE name < 'Charlie';
```

#### 3.3 LIKE模式匹配
```sql
SELECT * FROM students WHERE name LIKE 'A%';
SELECT * FROM students WHERE name LIKE '%e';
SELECT * FROM students WHERE name LIKE '%li%';
SELECT * FROM students WHERE email LIKE '%@example.com';
```

### 4. 数字条件

#### 4.1 整数条件
```sql
SELECT * FROM students WHERE age = 20;
SELECT * FROM students WHERE age > 20;
SELECT * FROM students WHERE age >= 21;
SELECT * FROM enrollments WHERE grade >= 90;
SELECT * FROM enrollments WHERE grade < 85;
```

#### 4.2 浮点数条件
```sql
SELECT * FROM employees WHERE salary = 7500.50;
SELECT * FROM employees WHERE salary > 7000.0;
SELECT * FROM employees WHERE salary <= 7500.50;
```

#### 4.3 数字类型混合比较
```sql
-- 测试Integer和Double之间的比较
SELECT * FROM employees WHERE salary >= 7000;
SELECT * FROM employees WHERE salary > 7000;
```

---

## 边界情况和异常测试

### 1. 空表查询
```sql
-- 在空表上查询
SELECT * FROM students;
SELECT name FROM students WHERE id = 1;
```

### 2. 单条记录查询
```sql
-- 只有一条记录的表
SELECT * FROM students WHERE id = 1;
```

### 3. 不存在的记录
```sql
SELECT * FROM students WHERE id = 999;
SELECT * FROM students WHERE name = 'NonExistent';
```

### 4. 数据类型测试

#### 4.1 整数类型
```sql
SELECT * FROM students WHERE id = 1;
SELECT * FROM students WHERE age = 20;
SELECT * FROM enrollments WHERE grade = 95;
```

#### 4.2 字符串类型
```sql
SELECT * FROM students WHERE name = 'Alice';
SELECT * FROM students WHERE email = 'alice@example.com';
```

#### 4.3 浮点数类型
```sql
SELECT * FROM employees WHERE salary = 7500.50;
SELECT * FROM employees WHERE salary > 7000.0;
```

### 5. 特殊字符测试
```sql
-- 包含特殊字符的字符串
INSERT INTO students VALUES (10, "O'Brien", 25, 'Male', "test@example.com");
SELECT * FROM students WHERE name = "O'Brien";
```

### 6. 大小写测试
```sql
SELECT * FROM students WHERE name = 'Alice';
SELECT * FROM students WHERE name = 'alice';
SELECT * FROM students WHERE name = 'ALICE';
```

### 7. 长字符串测试
```sql
INSERT INTO students VALUES (11, 'VeryLongNameThatExceedsNormalLength', 20, 'Male', 'long@example.com');
SELECT * FROM students WHERE name = 'VeryLongNameThatExceedsNormalLength';
```

### 8. 负数测试
```sql
INSERT INTO students VALUES (12, 'Negative', -1, 'Male', 'neg@example.com');
SELECT * FROM students WHERE age < 0;
```

### 9. 零值测试
```sql
INSERT INTO students VALUES (13, 'Zero', 0, 'Male', 'zero@example.com');
SELECT * FROM students WHERE age = 0;
```

### 10. 大数字测试
```sql
INSERT INTO students VALUES (999999, 'LargeID', 20, 'Male', 'large@example.com');
SELECT * FROM students WHERE id = 999999;
```

---

## 综合测试场景

### 场景1：查询高分学生
```sql
SELECT s.name, c.course_name, e.grade
FROM students s, enrollments e, courses c
WHERE s.id = e.student_id
  AND e.course_id = c.course_id
  AND e.grade >= 90
  AND s.age >= 20;
```

### 场景2：查询IT部门高薪员工
```sql
SELECT e.emp_name, e.salary, d.location
FROM employees e, departments d
WHERE e.department = d.dept_name
  AND e.department = 'IT'
  AND e.salary > 7000.0;
```

### 场景3：查询选课数量
```sql
SELECT s.name, COUNT(*) as course_count
FROM students s, enrollments e
WHERE s.id = e.student_id
GROUP BY s.name;
```

### 场景4：查询特定课程的学生
```sql
SELECT s.name, e.grade
FROM students s
JOIN enrollments e ON s.id = e.student_id
WHERE e.course_id = 101
  AND e.grade >= 85;
```

### 场景5：多条件组合查询
```sql
SELECT s.name, c.course_name, e.grade, c.teacher
FROM students s, enrollments e, courses c
WHERE s.id = e.student_id
  AND e.course_id = c.course_id
  AND s.age >= 20
  AND s.age <= 22
  AND e.grade >= 90
  AND c.credits >= 3;
```

---

## 测试建议

### 测试顺序
1. **先测试DDL操作**：创建表、修改表、删除表
2. **再测试DML操作**：插入数据、更新数据、删除数据
3. **然后测试单表查询**：基本查询、WHERE条件
4. **最后测试多表查询**：JOIN、隐式JOIN、复杂查询

### 测试重点
1. **数据类型兼容性**：确保Integer和Double可以正确比较
2. **WHERE条件**：单个条件、多个AND条件
3. **表别名**：支持表别名和表名.列名格式
4. **隐式JOIN**：逗号连接的表和WHERE中的连接条件
5. **边界情况**：空表、单条记录、不存在的记录

### 预期结果检查
- 查询结果是否正确
- 数据类型是否正确处理
- 错误信息是否清晰
- 性能是否可接受

---

*文档版本：1.0*  
*最后更新：2024年*

