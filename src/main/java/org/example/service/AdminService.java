package org.example.service;

import java.util.List;
import org.example.dao.OtpConfigDao;
import org.example.dao.UserDao;
import org.example.dto.OtpConfigResponse;
import org.example.dto.UpdateOtpConfigRequest;
import org.example.dto.UserResponse;
import org.example.exception.ApiException;
import org.example.model.OtpConfig;
import org.example.model.User;

public class AdminService {
    private final UserDao userDao;
    private final OtpConfigDao otpConfigDao;

    public AdminService(UserDao userDao, OtpConfigDao otpConfigDao) {
        this.userDao = userDao;
        this.otpConfigDao = otpConfigDao;
    }

    public OtpConfigResponse getConfig() {
        OtpConfig config = otpConfigDao.get();
        return toResponse(config);
    }

    public OtpConfigResponse updateConfig(UpdateOtpConfigRequest request) {
        if (request.codeLength() == null || request.ttlSeconds() == null) {
            throw new ApiException(400, "codeLength and ttlSeconds are required");
        }
        if (request.codeLength() < 4 || request.codeLength() > 12) {
            throw new ApiException(400, "codeLength must be between 4 and 12");
        }
        if (request.ttlSeconds() < 30 || request.ttlSeconds() > 3600) {
            throw new ApiException(400, "ttlSeconds must be between 30 and 3600");
        }

        OtpConfig updated = otpConfigDao.update(request.codeLength(), request.ttlSeconds());
        return toResponse(updated);
    }

    public List<UserResponse> getUsers() {
        return userDao.findAllNonAdminUsers()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteUser(long userId) {
        if (!userDao.deleteById(userId)) {
            throw new ApiException(404, "User not found");
        }
    }

    private OtpConfigResponse toResponse(OtpConfig config) {
        return new OtpConfigResponse(config.codeLength(), config.ttlSeconds(), config.updatedAt());
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.id(), user.username(), user.role().name(), user.createdAt());
    }
}
