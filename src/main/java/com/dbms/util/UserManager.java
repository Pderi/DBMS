package com.dbms.util;

import com.dbms.model.User;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户管理器
 */
public class UserManager {
    private static final String USER_FILE = "users.dat";
    private Map<String, User> users;
    private User currentUser;
    
    public UserManager() {
        this.users = new HashMap<>();
        loadUsers();
    }
    
    /**
     * 创建用户
     */
    public void createUser(String username, String password) {
        if (users.containsKey(username.toLowerCase())) {
            throw new DBMSException("User " + username + " already exists");
        }
        User user = new User(username, password);
        users.put(username.toLowerCase(), user);
        saveUsers();
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(String username) {
        if (!users.containsKey(username.toLowerCase())) {
            throw new DBMSException("User " + username + " does not exist");
        }
        users.remove(username.toLowerCase());
        saveUsers();
    }
    
    /**
     * 用户登录
     */
    public boolean login(String username, String password) {
        User user = users.get(username.toLowerCase());
        if (user != null && user.getPassword().equals(password)) {
            currentUser = user;
            return true;
        }
        return false;
    }
    
    /**
     * 用户登出
     */
    public void logout() {
        currentUser = null;
    }
    
    /**
     * 获取当前用户
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * 获取所有用户
     */
    public Map<String, User> getAllUsers() {
        return new HashMap<>(users); // 返回副本，防止外部修改
    }
    
    /**
     * 检查当前用户是否有权限
     */
    public boolean hasPermission(String permission) {
        if (currentUser == null) {
            return false;  // 未登录用户无权限
        }
        return currentUser.hasPermission(permission);
    }
    
    /**
     * 分配权限
     */
    public void grantPermission(String username, String permission) {
        User user = users.get(username.toLowerCase());
        if (user == null) {
            throw new DBMSException("User " + username + " does not exist");
        }
        user.addPermission(permission);
        saveUsers();
    }
    
    /**
     * 回收权限
     */
    public void revokePermission(String username, String permission) {
        User user = users.get(username.toLowerCase());
        if (user == null) {
            throw new DBMSException("User " + username + " does not exist");
        }
        user.removePermission(permission);
        saveUsers();
    }
    
    /**
     * 从文件加载用户
     */
    @SuppressWarnings("unchecked")
    private void loadUsers() {
        File file = new File(USER_FILE);
        if (!file.exists()) {
            // 创建默认管理员用户
            User admin = new User("admin", "admin");
            admin.addPermission("ALL");
            users.put("admin", admin);
            saveUsers();
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            users = (Map<String, User>) ois.readObject();
        } catch (Exception e) {
            // 如果加载失败，创建默认管理员
            User admin = new User("admin", "admin");
            admin.addPermission("ALL");
            users.put("admin", admin);
            saveUsers();
        }
    }
    
    /**
     * 保存用户到文件
     */
    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            throw new DBMSException("Failed to save users: " + e.getMessage(), e);
        }
    }
}

