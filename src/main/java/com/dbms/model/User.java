package com.dbms.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户类
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String username;           // 用户名
    private String password;           // 密码（实际应用中应该加密）
    private Set<String> permissions;   // 权限集合
    
    public User() {
        this.permissions = new HashSet<>();
    }
    
    public User(String username, String password) {
        this();
        this.username = username;
        this.password = password;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Set<String> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
    
    /**
     * 添加权限
     */
    public void addPermission(String permission) {
        permissions.add(permission);
    }
    
    /**
     * 删除权限
     */
    public void removePermission(String permission) {
        permissions.remove(permission);
    }
    
    /**
     * 检查是否有权限
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission) || permissions.contains("ALL");
    }
    
    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", permissions=" + permissions +
                '}';
    }
}

