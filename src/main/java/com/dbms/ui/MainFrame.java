package com.dbms.ui;

import com.dbms.engine.*;
import com.dbms.model.Database;
import com.dbms.model.Table;
import com.dbms.model.User;
import com.dbms.storage.DBFFileManager;
import com.dbms.util.BackupManager;
import com.dbms.util.SQLException;
import com.dbms.util.UserManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.ButtonModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 主窗口
 */
public class MainFrame extends JFrame {
    
    private Database database;
    private DDLExecutor ddlExecutor;
    private DMLExecutor dmlExecutor;
    private QueryExecutor queryExecutor;
    private SQLExecutor sqlExecutor;
    private UserManager userManager;
    
    private String dbFilePath;
    private String datFilePath;
    
    // UI组件
    private JTextArea sqlEditor;
    private JButton executeButton;
    private JTextArea resultArea;
    private JList<String> tableList;
    private DefaultListModel<String> tableListModel;
    private JTable structureTable;
    private DefaultTableModel structureTableModel;
    private JTable dataTable;
    private DefaultTableModel dataTableModel;
    private JTable indexTable;
    private DefaultTableModel indexTableModel;
    
    public MainFrame() {
        super("DBMS - 数据库管理系统");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1500, 950);
        setLocationRelativeTo(null);
        
        // 设置窗口图标和样式
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // 设置全局UI属性
            UIManager.put("TabbedPane.selected", new Color(70, 130, 180));
            // 设置默认字体，支持中文
            Font defaultFont = getDefaultFont();
            UIManager.put("Label.font", defaultFont);
            UIManager.put("Button.font", defaultFont);
            UIManager.put("TextField.font", defaultFont);
            UIManager.put("TextArea.font", defaultFont);
            UIManager.put("Table.font", defaultFont);
            UIManager.put("List.font", defaultFont);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        initializeDatabase();
        createUI();
        loadDatabase();
    }
    
    /**
     * 获取支持中文的默认字体
     */
    private Font getDefaultFont() {
        String[] fontNames = {"Microsoft YaHei", "SimHei", "SimSun", "宋体", "黑体", "Dialog"};
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();
        
        for (String fontName : fontNames) {
            for (String available : availableFonts) {
                if (available.equals(fontName)) {
                    return new Font(fontName, Font.PLAIN, 12);
                }
            }
        }
        // 如果都找不到，使用系统默认字体
        return new Font(Font.DIALOG, Font.PLAIN, 12);
    }
    
    private void initializeDatabase() {
        // 默认数据库文件路径
        dbFilePath = "database.dbf";
        datFilePath = "database.dat";
        
        database = new Database("MyDatabase");
        database.setDbFilePath(dbFilePath);
        database.setDatFilePath(datFilePath);
        
        ddlExecutor = new DDLExecutor(database, dbFilePath, datFilePath);
        dmlExecutor = new DMLExecutor(ddlExecutor, datFilePath);
        queryExecutor = new QueryExecutor(ddlExecutor, datFilePath);
        sqlExecutor = new SQLExecutor(ddlExecutor, dmlExecutor, queryExecutor);
        userManager = sqlExecutor.getUserManager(); // 从SQLExecutor获取UserManager实例
        updateWindowTitle(); // 更新窗口标题
    }
    
    private void createUI() {
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 左侧面板：表列表和表结构
        JPanel leftPanel = createLeftPanel();
        
        // 中间面板：SQL编辑器和结果
        JPanel centerPanel = createCenterPanel();
        
        // 右侧面板：数据表格
        JPanel rightPanel = createRightPanel();
        
        // 使用JSplitPane分割
        JSplitPane splitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerPanel);
        splitPane1.setDividerLocation(300);
        splitPane1.setResizeWeight(0.25);
        
        JSplitPane splitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitPane1, rightPanel);
        splitPane2.setDividerLocation(800);
        splitPane2.setResizeWeight(0.67);
        
        mainPanel.add(splitPane2, BorderLayout.CENTER);
        
        // 菜单栏
        setJMenuBar(createMenuBar());
        
        add(mainPanel);
    }
    
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.setBackground(new Color(250, 250, 255));
        
        // 表列表标题面板
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(new Color(70, 130, 180));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel tableListLabel = new JLabel("数据库表");
        Font titleFont2 = getDefaultFont().deriveFont(Font.BOLD, 15f);
        tableListLabel.setFont(titleFont2);
        tableListLabel.setForeground(Color.WHITE);
        titlePanel.add(tableListLabel);
        
        tableListModel = new DefaultListModel<>();
        tableList = new JList<>(tableListModel);
        tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableList.setFont(getDefaultFont().deriveFont(Font.PLAIN, 13f));
        tableList.setBackground(new Color(255, 255, 255));
        tableList.setSelectionBackground(new Color(100, 149, 237));
        tableList.setSelectionForeground(Color.WHITE);
        tableList.setFixedCellHeight(32);
        tableList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedTable = tableList.getSelectedValue();
                if (selectedTable != null) {
                    showTableStructure(selectedTable);
                    showTableData(selectedTable);
                }
            }
        });
        
        JScrollPane tableListScroll = new JScrollPane(tableList);
        tableListScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        tableListScroll.setBackground(Color.WHITE);
        
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(tableListScroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.setBackground(new Color(250, 250, 255));
        
        // SQL编辑器标题面板
        JPanel sqlTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        sqlTitlePanel.setBackground(new Color(70, 130, 180));
        sqlTitlePanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel sqlLabel = new JLabel("SQL 编辑器");
        Font titleFont = getDefaultFont().deriveFont(Font.BOLD, 15f);
        sqlLabel.setFont(titleFont);
        sqlLabel.setForeground(Color.WHITE);
        sqlTitlePanel.add(sqlLabel);
        
        sqlEditor = new JTextArea(15, 50);
        // 优先使用支持中文的等宽字体
        Font editorFont = getDefaultFont().deriveFont(Font.PLAIN, 13f);
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] fonts = ge.getAvailableFontFamilyNames();
            
            // 优先查找支持中文的等宽字体
            String[] preferredFonts = {
                "Microsoft YaHei Mono", "NSimSun", "SimSun", 
                "Microsoft YaHei", "SimHei", "SimSun", "宋体"
            };
            
            for (String fontName : preferredFonts) {
                for (String font : fonts) {
                    if (font.equals(fontName)) {
                        editorFont = new Font(fontName, Font.PLAIN, 13);
                        break;
                    }
                }
                if (!editorFont.getFamily().equals(getDefaultFont().getFamily())) {
                    break; // 找到了合适的字体
                }
            }
        } catch (Exception e) {
            // 使用默认字体
            editorFont = getDefaultFont().deriveFont(Font.PLAIN, 13f);
        }
        sqlEditor.setFont(editorFont);
        sqlEditor.setTabSize(4);
        sqlEditor.setBackground(new Color(253, 253, 255));
        sqlEditor.setForeground(new Color(30, 30, 30));
        sqlEditor.setCaretColor(new Color(70, 130, 180));
        sqlEditor.setSelectedTextColor(Color.WHITE);
        sqlEditor.setSelectionColor(new Color(100, 149, 237));
        // 添加示例SQL提示（注意：取消注释才能执行）
        sqlEditor.setText("-- 示例SQL语句（可以删除后输入自己的SQL）\n" +
                "-- 创建表:\n" +
                "CREATE TABLE students (\n" +
                "    id INT PRIMARY KEY NOT NULL,\n" +
                "    name VARCHAR(50) NOT NULL,\n" +
                "    age INT\n" +
                ");\n\n" +
                "-- 插入数据（取消下面的注释来执行）:\n" +
                "INSERT INTO students VALUES (1, 'Alice', 20);\n\n" +
                "-- 查询数据（取消下面的注释来执行）:\n" +
                "SELECT * FROM students;");
        JScrollPane sqlScroll = new JScrollPane(sqlEditor);
        sqlScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        
        // SQL编辑器面板
        JPanel sqlEditorPanel = new JPanel(new BorderLayout(0, 0));
        sqlEditorPanel.setBackground(new Color(250, 250, 255));
        sqlEditorPanel.add(sqlTitlePanel, BorderLayout.NORTH);
        sqlEditorPanel.add(sqlScroll, BorderLayout.CENTER);
        
        // 执行按钮 - 使用自定义渲染确保文字清晰可见
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        buttonPanel.setBackground(new Color(250, 250, 255));
        executeButton = new JButton("执行 SQL (F5)");
        Font buttonFont = getDefaultFont().deriveFont(Font.BOLD, 17f);
        executeButton.setFont(buttonFont);
        executeButton.setPreferredSize(new Dimension(260, 55));
        // 使用深色背景和白色文字，确保最大对比度
        executeButton.setBackground(new Color(0, 60, 140));
        executeButton.setForeground(Color.WHITE);
        executeButton.setFocusPainted(false);
        executeButton.setOpaque(true);
        executeButton.setContentAreaFilled(true);
        executeButton.setBorderPainted(true);
        executeButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 40, 100), 3),
            BorderFactory.createEmptyBorder(15, 35, 15, 35)
        ));
        // 自定义按钮渲染，确保文字始终清晰
        executeButton.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                ButtonModel model = b.getModel();
                
                // 绘制背景
                if (model.isPressed()) {
                    g.setColor(new Color(0, 40, 100));
                } else if (model.isRollover()) {
                    g.setColor(new Color(20, 100, 200));
                } else {
                    g.setColor(new Color(0, 60, 140));
                }
                g.fillRect(0, 0, c.getWidth(), c.getHeight());
                
                // 绘制文字
                g.setColor(Color.WHITE);
                FontMetrics fm = g.getFontMetrics(b.getFont());
                String text = b.getText();
                int x = (c.getWidth() - fm.stringWidth(text)) / 2;
                int y = (c.getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g.setFont(b.getFont());
                g.drawString(text, x, y);
            }
        });
        executeButton.addActionListener(e -> executeSQL());
        executeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                executeButton.repaint();
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                executeButton.repaint();
            }
        });
        // 添加键盘快捷键
        sqlEditor.getInputMap().put(KeyStroke.getKeyStroke("F5"), "execute");
        sqlEditor.getActionMap().put("execute", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                executeSQL();
            }
        });
        buttonPanel.add(executeButton);
        
        // 结果区域标题面板
        JPanel resultTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        resultTitlePanel.setBackground(new Color(70, 130, 180));
        resultTitlePanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel resultLabel = new JLabel("执行结果");
        Font titleFont3 = getDefaultFont().deriveFont(Font.BOLD, 15f);
        resultLabel.setFont(titleFont3);
        resultLabel.setForeground(Color.WHITE);
        resultTitlePanel.add(resultLabel);
        
        resultArea = new JTextArea(10, 50);
        // 使用支持中文的字体
        Font resultFont = getDefaultFont().deriveFont(Font.PLAIN, 13f);
        resultArea.setFont(resultFont);
        resultArea.setEditable(false);
        resultArea.setBackground(new Color(253, 253, 255));
        resultArea.setForeground(new Color(40, 40, 40));
        resultArea.setText("欢迎使用 DBMS 数据库管理系统！\n\n" +
                "使用说明：\n" +
                "   1. 在SQL编辑器中输入SQL语句\n" +
                "   2. 点击'执行 SQL'按钮或按F5执行\n" +
                "   3. 执行结果会显示在此区域\n" +
                "   4. 左侧显示数据库中的表列表\n" +
                "   5. 右侧显示选中表的结构和数据\n\n" +
                "支持的SQL语句：\n" +
                "   • CREATE TABLE: 创建表\n" +
                "   • ALTER TABLE: 修改表结构\n" +
                "   • DROP TABLE: 删除表\n" +
                "   • INSERT: 插入数据\n" +
                "   • UPDATE: 更新数据\n" +
                "   • DELETE: 删除数据\n" +
                "   • SELECT: 查询数据");
        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        
        // 结果面板
        JPanel resultPanel = new JPanel(new BorderLayout(0, 0));
        resultPanel.setBackground(new Color(250, 250, 255));
        resultPanel.add(resultTitlePanel, BorderLayout.NORTH);
        resultPanel.add(resultScroll, BorderLayout.CENTER);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sqlEditorPanel, resultPanel);
        splitPane.setDividerLocation(380);
        splitPane.setResizeWeight(0.55);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerSize(6);
        
        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.setBackground(new Color(250, 250, 255));
        
        // 表结构标题面板
        JPanel structureTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        structureTitlePanel.setBackground(new Color(70, 130, 180));
        structureTitlePanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel structureLabel = new JLabel("表结构");
        Font titleFont4 = getDefaultFont().deriveFont(Font.BOLD, 15f);
        structureLabel.setFont(titleFont4);
        structureLabel.setForeground(Color.WHITE);
        structureTitlePanel.add(structureLabel);
        
        structureTableModel = new DefaultTableModel(
            new Object[]{"字段名", "类型", "长度", "主键", "可空", "索引"}, 0);
        structureTable = new JTable(structureTableModel);
        structureTable.setFont(getDefaultFont().deriveFont(Font.PLAIN, 12f));
        structureTable.setRowHeight(28);
        structureTable.setEnabled(false);
        structureTable.setBackground(new Color(255, 255, 255));
        structureTable.setGridColor(new Color(240, 240, 240));
        structureTable.setShowGrid(true);
        structureTable.setIntercellSpacing(new Dimension(1, 1));
        structureTable.getTableHeader().setFont(getDefaultFont().deriveFont(Font.BOLD, 14f));
        // 使用更深的蓝色，提高对比度
        structureTable.getTableHeader().setBackground(new Color(50, 100, 180));
        structureTable.getTableHeader().setForeground(Color.WHITE);
        structureTable.getTableHeader().setPreferredSize(new Dimension(0, 38));
        structureTable.getTableHeader().setReorderingAllowed(false);
        // 确保表头文字清晰可见
        structureTable.getTableHeader().setOpaque(true);
        // 自定义表头渲染器，确保文字清晰
        structureTable.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(new Color(50, 100, 180));
                setForeground(Color.WHITE);
                setFont(getDefaultFont().deriveFont(Font.BOLD, 14f));
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                return this;
            }
        });
        JScrollPane structureScroll = new JScrollPane(structureTable);
        structureScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        
        // 索引列表标题面板
        JPanel indexTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        indexTitlePanel.setBackground(new Color(70, 130, 180));
        indexTitlePanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel indexLabel = new JLabel("索引列表");
        Font titleFont6 = getDefaultFont().deriveFont(Font.BOLD, 15f);
        indexLabel.setFont(titleFont6);
        indexLabel.setForeground(Color.WHITE);
        indexTitlePanel.add(indexLabel);
        
        indexTableModel = new DefaultTableModel(
            new Object[]{"索引名", "字段名", "唯一索引"}, 0);
        indexTable = new JTable(indexTableModel);
        indexTable.setFont(getDefaultFont().deriveFont(Font.PLAIN, 12f));
        indexTable.setRowHeight(28);
        indexTable.setEnabled(false);
        indexTable.setBackground(new Color(255, 255, 255));
        indexTable.setGridColor(new Color(240, 240, 240));
        indexTable.setShowGrid(true);
        indexTable.setIntercellSpacing(new Dimension(1, 1));
        indexTable.getTableHeader().setFont(getDefaultFont().deriveFont(Font.BOLD, 14f));
        indexTable.getTableHeader().setBackground(new Color(50, 100, 180));
        indexTable.getTableHeader().setForeground(Color.WHITE);
        indexTable.getTableHeader().setPreferredSize(new Dimension(0, 38));
        indexTable.getTableHeader().setReorderingAllowed(false);
        indexTable.getTableHeader().setOpaque(true);
        indexTable.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(new Color(50, 100, 180));
                setForeground(Color.WHITE);
                setFont(getDefaultFont().deriveFont(Font.BOLD, 14f));
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                return this;
            }
        });
        JScrollPane indexScroll = new JScrollPane(indexTable);
        indexScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        
        JPanel indexPanel = new JPanel(new BorderLayout(0, 0));
        indexPanel.setBackground(new Color(250, 250, 255));
        indexPanel.add(indexTitlePanel, BorderLayout.NORTH);
        indexPanel.add(indexScroll, BorderLayout.CENTER);
        
        // 表结构面板（包含字段表格和索引表格）
        JSplitPane structureSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, structureScroll, indexPanel);
        structureSplitPane.setDividerLocation(200);
        structureSplitPane.setResizeWeight(0.6);
        structureSplitPane.setBorder(BorderFactory.createEmptyBorder());
        structureSplitPane.setDividerSize(6);
        
        JPanel structurePanel = new JPanel(new BorderLayout(0, 0));
        structurePanel.setBackground(new Color(250, 250, 255));
        structurePanel.add(structureTitlePanel, BorderLayout.NORTH);
        structurePanel.add(structureSplitPane, BorderLayout.CENTER);
        
        // 数据表格标题面板
        JPanel dataTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dataTitlePanel.setBackground(new Color(70, 130, 180));
        dataTitlePanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel dataLabel = new JLabel("表数据");
        Font titleFont5 = getDefaultFont().deriveFont(Font.BOLD, 15f);
        dataLabel.setFont(titleFont5);
        dataLabel.setForeground(Color.WHITE);
        dataTitlePanel.add(dataLabel);
        
        dataTableModel = new DefaultTableModel();
        dataTable = new JTable(dataTableModel);
        dataTable.setFont(getDefaultFont().deriveFont(Font.PLAIN, 12f));
        dataTable.setRowHeight(28);
        dataTable.setBackground(new Color(255, 255, 255));
        dataTable.setGridColor(new Color(240, 240, 240));
        dataTable.setShowGrid(true);
        dataTable.setIntercellSpacing(new Dimension(1, 1));
        dataTable.setSelectionBackground(new Color(230, 240, 255));
        dataTable.setSelectionForeground(new Color(30, 30, 30));
        dataTable.getTableHeader().setFont(getDefaultFont().deriveFont(Font.BOLD, 14f));
        // 使用更深的蓝色，提高对比度
        dataTable.getTableHeader().setBackground(new Color(50, 100, 180));
        dataTable.getTableHeader().setForeground(Color.WHITE);
        dataTable.getTableHeader().setPreferredSize(new Dimension(0, 38));
        dataTable.getTableHeader().setReorderingAllowed(false);
        // 确保表头文字清晰可见
        dataTable.getTableHeader().setOpaque(true);
        // 自定义表头渲染器，确保文字清晰
        dataTable.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(new Color(50, 100, 180));
                setForeground(Color.WHITE);
                setFont(getDefaultFont().deriveFont(Font.BOLD, 14f));
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                return this;
            }
        });
        // 设置交替行颜色
        dataTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                return this;
            }
        });
        JScrollPane dataScroll = new JScrollPane(dataTable);
        dataScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        
        JPanel dataPanel = new JPanel(new BorderLayout(0, 0));
        dataPanel.setBackground(new Color(250, 250, 255));
        dataPanel.add(dataTitlePanel, BorderLayout.NORTH);
        dataPanel.add(dataScroll, BorderLayout.CENTER);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, structurePanel, dataPanel);
        splitPane.setDividerLocation(280);
        splitPane.setResizeWeight(0.38);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerSize(6);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(240, 240, 245));
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 210)));
        
        // File菜单
        JMenu fileMenu = new JMenu("文件");
        fileMenu.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        fileMenu.setForeground(new Color(50, 50, 50));
        
        JMenuItem newItem = new JMenuItem("新建数据库");
        newItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        newItem.addActionListener(e -> newDatabase());
        
        JMenuItem openItem = new JMenuItem("打开数据库");
        openItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        openItem.addActionListener(e -> openDatabase());
        
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        fileMenu.addSeparator();
        
        JMenuItem backupItem = new JMenuItem("备份数据库");
        backupItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        backupItem.addActionListener(e -> backupDatabase());
        fileMenu.add(backupItem);
        
        JMenuItem restoreItem = new JMenuItem("恢复数据库");
        restoreItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        restoreItem.addActionListener(e -> restoreDatabase());
        fileMenu.add(restoreItem);
        
        menuBar.add(fileMenu);
        
        // 用户管理菜单
        JMenu userMenu = new JMenu("用户管理");
        userMenu.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        userMenu.setForeground(new Color(50, 50, 50));
        
        JMenuItem loginItem = new JMenuItem("用户登录");
        loginItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        loginItem.addActionListener(e -> showLoginDialog());
        userMenu.add(loginItem);
        
        JMenuItem logoutItem = new JMenuItem("用户登出");
        logoutItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        logoutItem.addActionListener(e -> logoutUser());
        userMenu.add(logoutItem);
        
        userMenu.addSeparator();
        
        JMenuItem userManageItem = new JMenuItem("查看用户列表");
        userManageItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        userManageItem.addActionListener(e -> showUserManagementDialog());
        userMenu.add(userManageItem);
        
        menuBar.add(userMenu);
        
        return menuBar;
    }
    
    /**
     * 显示用户管理对话框
     */
    private void showUserManagementDialog() {
        JDialog dialog = new JDialog(this, "用户管理", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // 标题
        JLabel titleLabel = new JLabel("用户列表");
        Font titleFont = getDefaultFont().deriveFont(Font.BOLD, 16f);
        titleLabel.setFont(titleFont);
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 用户表格
        String[] columnNames = {"用户名", "权限"};
        DefaultTableModel userTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 表格不可编辑
            }
        };
        JTable userTable = new JTable(userTableModel);
        userTable.setFont(getDefaultFont().deriveFont(Font.PLAIN, 12f));
        userTable.setRowHeight(28);
        userTable.getTableHeader().setFont(getDefaultFont().deriveFont(Font.BOLD, 13f));
        userTable.getTableHeader().setBackground(new Color(70, 130, 180));
        userTable.getTableHeader().setForeground(Color.BLACK);
        userTable.setGridColor(new Color(240, 240, 240));
        userTable.setShowGrid(true);
        
        // 填充用户数据
        Map<String, User> users = userManager.getAllUsers();
        for (User user : users.values()) {
            String username = user.getUsername();
            Set<String> permissions = user.getPermissions();
            String permissionsStr;
            if (permissions.contains("ALL")) {
                permissionsStr = "ALL (所有权限)";
            } else if (permissions.isEmpty()) {
                permissionsStr = "无权限";
            } else {
                permissionsStr = String.join(", ", permissions);
            }
            userTableModel.addRow(new Object[]{username, permissionsStr});
        }
        
        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 底部按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("刷新");
        refreshButton.setFont(getDefaultFont().deriveFont(Font.PLAIN, 12f));
        refreshButton.addActionListener(e -> {
            // 重新加载用户数据
            userTableModel.setRowCount(0);
            Map<String, User> refreshedUsers = userManager.getAllUsers();
            for (User user : refreshedUsers.values()) {
                String username = user.getUsername();
                Set<String> permissions = user.getPermissions();
                String permissionsStr;
                if (permissions.contains("ALL")) {
                    permissionsStr = "ALL (所有权限)";
                } else if (permissions.isEmpty()) {
                    permissionsStr = "无权限";
                } else {
                    permissionsStr = String.join(", ", permissions);
                }
                userTableModel.addRow(new Object[]{username, permissionsStr});
            }
        });
        
        JButton closeButton = new JButton("关闭");
        closeButton.setFont(getDefaultFont().deriveFont(Font.PLAIN, 12f));
        closeButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    /**
     * 显示用户登录对话框
     */
    private void showLoginDialog() {
        JDialog dialog = new JDialog(this, "用户登录", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 输入面板
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 用户名
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel usernameLabel = new JLabel("用户名:");
        usernameLabel.setFont(getDefaultFont().deriveFont(Font.PLAIN, 12f));
        inputPanel.add(usernameLabel, gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JTextField usernameField = new JTextField(20);
        usernameField.setFont(getDefaultFont().deriveFont(Font.PLAIN, 12f));
        inputPanel.add(usernameField, gbc);
        
        // 密码
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel passwordLabel = new JLabel("密码:");
        passwordLabel.setFont(getDefaultFont().deriveFont(Font.PLAIN, 12f));
        inputPanel.add(passwordLabel, gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setFont(getDefaultFont().deriveFont(Font.PLAIN, 12f));
        inputPanel.add(passwordField, gbc);
        
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton loginButton = new JButton("登录");
        loginButton.setFont(getDefaultFont().deriveFont(Font.PLAIN, 12f));
        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                    "请输入用户名和密码",
                    "输入错误", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                if (userManager.login(username, password)) {
                    User currentUser = userManager.getCurrentUser();
                    Set<String> permissions = currentUser.getPermissions();
                    String permissionsStr = permissions.contains("ALL") ? "ALL (所有权限)" :
                        permissions.isEmpty() ? "无权限" : String.join(", ", permissions);
                    
                    JOptionPane.showMessageDialog(dialog,
                        "登录成功！\n当前用户: " + username + "\n权限: " + permissionsStr,
                        "登录成功", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    
                    // 更新窗口标题显示当前用户
                    updateWindowTitle();
                } else {
                    JOptionPane.showMessageDialog(dialog,
                        "用户名或密码错误",
                        "登录失败", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                    "登录失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton cancelButton = new JButton("取消");
        cancelButton.setFont(getDefaultFont().deriveFont(Font.PLAIN, 12f));
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    /**
     * 用户登出
     */
    private void logoutUser() {
        if (userManager.getCurrentUser() == null) {
            JOptionPane.showMessageDialog(this,
                "当前没有登录的用户",
                "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "确定要登出吗？",
            "确认登出", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            userManager.logout();
            updateWindowTitle();
            JOptionPane.showMessageDialog(this,
                "已成功登出",
                "登出成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * 更新窗口标题（显示当前登录用户）
     */
    private void updateWindowTitle() {
        User currentUser = userManager.getCurrentUser();
        if (currentUser != null) {
            setTitle("DBMS - 数据库管理系统 [当前用户: " + currentUser.getUsername() + "]");
        } else {
            setTitle("DBMS - 数据库管理系统");
        }
    }
    
    private void loadDatabase() {
        try {
            File dbFile = new File(dbFilePath);
            if (dbFile.exists()) {
                database = DBFFileManager.readDatabaseFile(dbFilePath);
                database.setDbFilePath(dbFilePath);
                database.setDatFilePath(datFilePath);
                
                ddlExecutor = new DDLExecutor(database, dbFilePath, datFilePath);
                dmlExecutor = new DMLExecutor(ddlExecutor, datFilePath);
                queryExecutor = new QueryExecutor(ddlExecutor, datFilePath);
                sqlExecutor = new SQLExecutor(ddlExecutor, dmlExecutor, queryExecutor);
                userManager = sqlExecutor.getUserManager();
                updateWindowTitle(); // 更新窗口标题
                
                refreshTableList();
                resultArea.setText("数据库加载成功！当前数据库文件: " + dbFilePath + "\n" +
                        "表数量: " + database.getTableCount());
            } else {
                // 创建新数据库文件
                DBFFileManager.createDatabaseFile(dbFilePath, database);
                File datFile = new File(datFilePath);
                if (!datFile.exists()) {
                    datFile.createNewFile();
                }
                resultArea.setText("已创建新数据库文件: " + dbFilePath + "\n" +
                        "请使用CREATE TABLE语句创建表。");
            }
        } catch (Exception e) {
            String errorMsg = "Error loading database: " + e.getMessage();
            resultArea.setText(errorMsg);
            JOptionPane.showMessageDialog(this, errorMsg,
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void refreshTableList() {
        tableListModel.clear();
        List<String> tableNames = database.getTableNames();
        for (String name : tableNames) {
            tableListModel.addElement(name);
        }
    }
    
    private void showTableStructure(String tableName) {
        Table table = database.getTable(tableName);
        if (table == null) return;
        
        // 显示字段信息
        structureTableModel.setRowCount(0);
        for (com.dbms.model.Field field : table.getFields()) {
            // 查找该字段上的索引
            com.dbms.model.Index index = table.getIndexByColumn(field.getName());
            String indexInfo = "";
            if (index != null) {
                indexInfo = index.getIndexName() + (index.isUnique() ? " (唯一)" : "");
            }
            
            structureTableModel.addRow(new Object[]{
                field.getName(),
                field.getType().getSqlName(),
                field.getLength(),
                field.isKey() ? "是" : "否",
                field.isNullable() ? "是" : "否",
                indexInfo.isEmpty() ? "无" : indexInfo
            });
        }
        
        // 显示索引列表
        indexTableModel.setRowCount(0);
        if (table.getIndexes() != null && !table.getIndexes().isEmpty()) {
            for (com.dbms.model.Index index : table.getIndexes().values()) {
                indexTableModel.addRow(new Object[]{
                    index.getIndexName(),
                    index.getColumnName(),
                    index.isUnique() ? "是" : "否"
                });
            }
        }
    }
    
    private void showTableData(String tableName) {
        try {
            Table table = database.getTable(tableName);
            if (table == null) {
                System.err.println("表不存在: " + tableName);
                dataTableModel.setRowCount(0);
                dataTableModel.setColumnCount(0);
                return;
            }
            
            // 执行SELECT查询
            QueryExecutor.QueryResult result = queryExecutor.select(tableName, null, null, null, null, null, null);
            
            System.out.println("查询表 [" + tableName + "] 返回 " + result.getRowCount() + " 行数据");
            
            // 更新表格模型
            String[] columnNames = result.getColumnNames().toArray(new String[0]);
            dataTableModel.setColumnIdentifiers(columnNames);
            dataTableModel.setRowCount(0);
            
            if (result.getRowCount() > 0) {
                for (List<Object> row : result.getData()) {
                    Object[] rowData = new Object[row.size()];
                    for (int i = 0; i < row.size(); i++) {
                        Object value = row.get(i);
                        rowData[i] = value == null ? "NULL" : value.toString();
                    }
                    dataTableModel.addRow(rowData);
                }
                System.out.println("成功加载 " + result.getRowCount() + " 行数据到表格");
            } else {
                // 如果没有数据，确保表格列已设置
                dataTableModel.setRowCount(0);
                System.out.println("表 [" + tableName + "] 没有数据");
            }
        } catch (Exception e) {
            // 如果读取失败，清空表格
            dataTableModel.setRowCount(0);
            dataTableModel.setColumnCount(0);
            // 输出详细错误信息到控制台
            System.err.println("加载表数据失败 [" + tableName + "]: " + e.getMessage());
            e.printStackTrace();
            // 也在结果区域显示错误（如果用户需要）
            // 注意：这里不显示对话框，因为可能会在自动刷新时频繁弹出
        }
    }
    
    private void executeSQL() {
        String sql = sqlEditor.getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入SQL语句", 
                "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 移除整行注释（以--开头的行），但保留行内注释后的内容
        String[] lines = sql.split("\n");
        StringBuilder cleanSql = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            // 只过滤掉整行注释（以--开头且后面没有SQL代码的行）
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue; // 跳过空行和整行注释
            }
            // 处理行内注释：保留--之前的内容
            int commentIndex = line.indexOf("--");
            if (commentIndex >= 0) {
                // 检查--是否在字符串中
                String beforeComment = line.substring(0, commentIndex);
                // 简单检查：如果--前有奇数个单引号，说明在字符串中，保留整行
                long quoteCount = beforeComment.chars().filter(ch -> ch == '\'').count();
                if (quoteCount % 2 == 0) {
                    // 不在字符串中，是注释
                    line = beforeComment.trim();
                    if (line.isEmpty()) continue;
                }
            }
            cleanSql.append(line).append("\n");
        }
        sql = cleanSql.toString().trim();
        
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "SQL语句不能为空（请取消注释或输入有效的SQL语句）", 
                "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 按分号分割多条SQL语句
        String[] statements = splitSQLStatements(sql);
        StringBuilder resultText = new StringBuilder();
        boolean hasError = false;
        QueryExecutor.QueryResult lastQueryResult = null;
        
        for (int i = 0; i < statements.length; i++) {
            String statement = statements[i].trim();
            if (statement.isEmpty()) continue;
            
            try {
                Object result = sqlExecutor.execute(statement);
                
                if (result instanceof QueryExecutor.QueryResult) {
                    // SELECT查询结果
                    lastQueryResult = (QueryExecutor.QueryResult) result;
                    resultText.append("语句 ").append(i + 1).append(" 执行成功:\n");
                    resultText.append("查询到 ").append(lastQueryResult.getRowCount()).append(" 行数据\n\n");
                } else {
                    // 其他操作的结果消息
                    resultText.append("语句 ").append(i + 1).append(" 执行成功: ").append(result.toString()).append("\n\n");
                    refreshTableList();
                }
            } catch (SQLException e) {
                hasError = true;
                resultText.append("语句 ").append(i + 1).append(" 执行失败: ").append(e.getMessage()).append("\n\n");
            } catch (Exception e) {
                hasError = true;
                resultText.append("语句 ").append(i + 1).append(" 执行失败: ").append(e.getMessage()).append("\n\n");
                e.printStackTrace();
            }
        }
        
        // 显示结果
        if (lastQueryResult != null) {
            // 如果有查询结果，显示详细结果
            displayQueryResult(lastQueryResult);
            // 在数据表格中显示结果
            dataTableModel.setColumnIdentifiers(lastQueryResult.getColumnNames().toArray(new String[0]));
            dataTableModel.setRowCount(0);
            for (List<Object> row : lastQueryResult.getData()) {
                Object[] rowData = new Object[row.size()];
                for (int i = 0; i < row.size(); i++) {
                    Object value = row.get(i);
                    rowData[i] = value == null ? "NULL" : value.toString();
                }
                dataTableModel.addRow(rowData);
            }
        } else {
            resultArea.setText(resultText.toString());
        }
        
        // 刷新表格显示 - 如果执行了INSERT/UPDATE/DELETE，自动刷新当前表的数据
        String selectedTable = tableList.getSelectedValue();
        System.out.println("准备刷新表数据，选中表: " + selectedTable + ", lastQueryResult: " + lastQueryResult);
        
        // 如果最后执行的不是SELECT，需要刷新表数据
        if (lastQueryResult == null) {
            String tableToRefresh = null;
            if (selectedTable != null) {
                tableToRefresh = selectedTable;
            } else if (!tableListModel.isEmpty()) {
                // 如果没有选中表，但表列表不为空，选中第一个表
                tableList.setSelectedIndex(0);
                tableToRefresh = tableList.getSelectedValue();
            }
            
            if (tableToRefresh != null) {
                System.out.println("刷新表数据: " + tableToRefresh);
                showTableStructure(tableToRefresh);
                // 同步刷新表数据（确保在数据写入完成后立即刷新）
                try {
                    showTableData(tableToRefresh);
                } catch (Exception e) {
                    // 刷新表数据失败不影响SQL执行结果，只记录错误
                    System.err.println("刷新表数据失败 [" + tableToRefresh + "]: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("没有可刷新的表");
            }
        } else {
            System.out.println("跳过刷新，因为 lastQueryResult 不为 null");
        }
        
        // 只有在SQL语句执行失败时才显示警告
        if (hasError) {
            JOptionPane.showMessageDialog(this, "部分SQL语句执行失败，请查看结果区域", 
                "执行警告", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    /**
     * 按分号分割SQL语句（考虑字符串中的分号）
     */
    private String[] splitSQLStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        char stringChar = 0;
        
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            
            if (!inString && (c == '\'' || c == '"')) {
                inString = true;
                stringChar = c;
                current.append(c);
            } else if (inString && c == stringChar) {
                // 检查是否是转义字符
                if (i > 0 && sql.charAt(i - 1) != '\\') {
                    inString = false;
                    stringChar = 0;
                }
                current.append(c);
            } else if (!inString && c == ';') {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        
        // 添加最后一条语句（如果没有分号结尾）
        String lastStmt = current.toString().trim();
        if (!lastStmt.isEmpty()) {
            statements.add(lastStmt);
        }
        
        return statements.toArray(new String[0]);
    }
    
    private void displayQueryResult(QueryExecutor.QueryResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ 查询执行成功！\n");
        sb.append("📊 查询结果: ").append(result.getRowCount()).append(" 行数据\n\n");
        
        if (result.getRowCount() > 0) {
            // 显示列名
            sb.append("列名: ");
            for (int i = 0; i < result.getColumnNames().size(); i++) {
                if (i > 0) sb.append(" | ");
                sb.append(result.getColumnNames().get(i));
            }
            sb.append("\n");
            sb.append("─".repeat(Math.min(80, result.getColumnNames().size() * 15))).append("\n");
            
            // 显示前10行数据（避免显示过多）
            int maxRows = Math.min(10, result.getRowCount());
            for (int i = 0; i < maxRows; i++) {
                List<Object> row = result.getData().get(i);
                sb.append("第").append(i + 1).append("行: ");
                for (int j = 0; j < row.size(); j++) {
                    if (j > 0) sb.append(" | ");
                    Object value = row.get(j);
                    String str = value == null ? "NULL" : value.toString();
                    if (str.length() > 15) {
                        str = str.substring(0, 12) + "...";
                    }
                    sb.append(str);
                }
                sb.append("\n");
            }
            
            if (result.getRowCount() > 10) {
                sb.append("\n... (仅显示前10行，共").append(result.getRowCount()).append("行)\n");
                sb.append("完整数据请查看右侧'表数据'面板\n");
            }
        } else {
            sb.append("⚠️ 查询结果为空，没有匹配的数据\n");
        }
        
        resultArea.setText(sb.toString());
    }
    
    private void openDatabase() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".dbf");
            }
            
            @Override
            public String getDescription() {
                return "Database Files (*.dbf)";
            }
        });
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            dbFilePath = fileChooser.getSelectedFile().getAbsolutePath();
            datFilePath = dbFilePath.replace(".dbf", ".dat");
            loadDatabase();
        }
    }
    
    private void newDatabase() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".dbf");
            }
            
            @Override
            public String getDescription() {
                return "Database Files (*.dbf)";
            }
        });
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            dbFilePath = fileChooser.getSelectedFile().getAbsolutePath();
            if (!dbFilePath.endsWith(".dbf")) {
                dbFilePath += ".dbf";
            }
            datFilePath = dbFilePath.replace(".dbf", ".dat");
            
            database = new Database("NewDatabase");
            database.setDbFilePath(dbFilePath);
            database.setDatFilePath(datFilePath);
            
            ddlExecutor = new DDLExecutor(database, dbFilePath, datFilePath);
            dmlExecutor = new DMLExecutor(ddlExecutor, datFilePath);
            queryExecutor = new QueryExecutor(ddlExecutor, datFilePath);
            sqlExecutor = new SQLExecutor(ddlExecutor, dmlExecutor, queryExecutor);
            userManager = sqlExecutor.getUserManager();
            updateWindowTitle(); // 更新窗口标题
            
            try {
                DBFFileManager.createDatabaseFile(dbFilePath, database);
                File datFile = new File(datFilePath);
                if (!datFile.exists()) {
                    datFile.createNewFile();
                }
                refreshTableList();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error creating database: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void backupDatabase() {
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.setDialogTitle("选择备份目录");
        
        if (dirChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String backupDir = dirChooser.getSelectedFile().getAbsolutePath();
                String backupPath = BackupManager.backupDatabase(dbFilePath, backupDir);
                JOptionPane.showMessageDialog(this, 
                    "数据库备份成功！\n备份文件: " + backupPath,
                    "备份成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "备份失败: " + e.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void restoreDatabase() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || (f.getName().endsWith(".dbf") && f.getName().contains("_backup_"));
            }
            
            @Override
            public String getDescription() {
                return "Backup Files (*_backup_*.dbf)";
            }
        });
        fileChooser.setDialogTitle("选择备份文件");
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "恢复数据库将覆盖当前数据库文件，是否继续？",
                "确认恢复", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    String backupFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                    BackupManager.restoreDatabase(backupFilePath, dbFilePath);
                    
                    // 重新加载数据库
                    loadDatabase();
                    
                    JOptionPane.showMessageDialog(this, 
                        "数据库恢复成功！",
                        "恢复成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, 
                        "恢复失败: " + e.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}

