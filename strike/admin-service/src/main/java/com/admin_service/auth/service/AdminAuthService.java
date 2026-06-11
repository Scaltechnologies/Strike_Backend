package com.admin_service.auth.service;

import com.admin_service.auth.dto.*;
import com.admin_service.auth.entity.Admin;

import java.util.List;

public interface AdminAuthService {
    AdminLoginResponse login(AdminLoginRequest request);
    Admin register(AdminRegisterRequest request);
    Admin setup(AdminRegisterRequest request);
    void changePassword(Long adminId, ChangePasswordRequest request);
    Admin getById(Long adminId);
    List<Admin> getAllAdmins();
    void setActive(Long adminId, boolean active);
}