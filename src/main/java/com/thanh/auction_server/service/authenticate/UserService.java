package com.thanh.auction_server.service.authenticate;

import com.thanh.auction_server.constants.LogAction;
import com.thanh.auction_server.constants.RoleEnum;
import com.thanh.auction_server.dto.request.*;
import com.thanh.auction_server.dto.response.*;
import com.thanh.auction_server.entity.Role;
import com.thanh.auction_server.entity.User;
import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.exception.*;
import com.thanh.auction_server.mapper.ProductMapper;
import com.thanh.auction_server.mapper.UserMapper;
import com.thanh.auction_server.repository.*;
import com.thanh.auction_server.service.admin.AuditLogService;
import com.thanh.auction_server.service.utils.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    RoleRepository roleRepository;
    RefreshTokenRepository refreshTokenRepository;
    OtpService otpService;
    EmailService emailService;
    FeedbackRepository feedbackRepository;
    ProductMapper productMapper;
    ProductRepository productRepository;
    AuditLogService auditLogService;

    public UserResponse createUser(UserCreationRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new UserAlreadyExistsException(ErrorMessage.USER_ALREADY_EXIST);
        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DataConflictException(ErrorMessage.PHONE_ALREADY_EXIST);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        HashSet<Role> roles = new HashSet<>();
        roleRepository.findById(RoleEnum.USER.name()).ifPresent(roles::add);

        user.setRoles(roles);
        user.setIsActive(true);

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public MessageResponse createUserOtp(UserCreationRequest request) {
        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DataConflictException(ErrorMessage.PHONE_ALREADY_EXIST);
        }
        var existingUserOpt = userRepository.findByEmail(request.getEmail());
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.getIsActive()) {
                throw new UserAlreadyExistsException(ErrorMessage.EMAIL_ALREADY_EXIST);
            } else {
                String newOtp = otpService.generateAndSaveOtp(existingUser);
                emailService.sendOtpEmail(existingUser.getEmail(), newOtp);
                return MessageResponse.builder()
                        .message("Email đã tồn tại nhưng chưa được kích hoạt. Một OTP mới đã được gửi đến email của bạn.")
                        .build();
            }
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException(ErrorMessage.USER_ALREADY_EXIST);
        }
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        HashSet<Role> roles = new HashSet<>();
        roleRepository.findById(RoleEnum.USER.name()).ifPresent(roles::add);
        user.setRoles(roles);

        user.setIsActive(false);

        User savedUser = userRepository.save(user);

        String otp = otpService.generateAndSaveOtp(savedUser);
        emailService.sendOtpEmail(savedUser.getEmail(), otp);

        return MessageResponse.builder()
                .message("User được tạo thành công. Vui lòng kiểm tra email để lấy OTP kích hoạt tài khoản.")
                .build();
    }

    @Transactional
    public UserResponse updateMyInfo(UserUpdateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElseThrow(
                () -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
        if (request.getEmail() != null
                && !request.getEmail().equals(currentUser.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DataConflictException(ErrorMessage.EMAIL_ALREADY_EXIST);
        }

        if (request.getPhoneNumber() != null
                && !request.getPhoneNumber().equals(currentUser.getPhoneNumber())
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DataConflictException(ErrorMessage.PHONE_ALREADY_EXIST);
        }
        userMapper.updateUser(currentUser, request);
        currentUser.setUpdatedAt(LocalDateTime.now());
        return userMapper.toUserResponse(userRepository.save(currentUser));
    }

    @PreAuthorize("hasRole('ADMIN')")
//    @PreAuthorize("hasAuthority('FULL_EDIT')") //Permission
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream().map(user -> {
            var userResponse = userMapper.toUserResponse(user);
            userResponse.setNoPassword(!StringUtils.hasText(user.getPassword()));
            return userResponse;
        }).toList();
    }

    public UserResponse getUser(String id) {
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(
                        () -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND))
        );
    }

    public PageResponse<UserResponse> getUsersPagination(int page, int size) {
//        Sort sort = Sort.by("createdAt").ascending();
        Pageable pageable = PageRequest.of(page - 1, size /*sort*/);
        Page<User> pageData = userRepository.findAll(pageable);
        return PageResponse.<UserResponse>builder()
                .currentPage(page)
                .totalPages(pageData.getTotalPages())
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .data(pageData.getContent().stream().map(user -> {
                    var userResponse = userMapper.toUserResponse(user);
                    userResponse.setNoPassword(!StringUtils.hasText(user.getPassword()));
                    return userResponse;
                }).toList())
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String id) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
        userRepository.delete(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public String updateUserStatus(String id, Boolean isActive) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
        user.setIsActive(isActive);
        userRepository.save(user);
        String action = isActive ? LogAction.UNLOCK_USER : LogAction.LOCK_USER;
        auditLogService.saveLog(action, id, "Admin đã change status thành: " + isActive);
        return isActive ? "User activated successfully" : "User deactivated successfully";
    }

    public void createPassword(PasswordCreationRequest request) {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        User user = userRepository.findByUsername(name).orElseThrow(
                () -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
        if (StringUtils.hasText(user.getPassword()))
            throw new UserAlreadyExistsException(ErrorMessage.PASSWORD_ALREADY_EXIST);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

    }

    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        User user = userRepository.findByUsername(name).orElseThrow(
                () -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
        var userResponse = userMapper.toUserResponse(user);
        userResponse.setNoPassword(!StringUtils.hasText(user.getPassword()));

        return userResponse;
    }

    @Transactional
    public MessageResponse changePassword(ChangePassRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException(ErrorMessage.CURRENT_PASSWORD_INCORRECT);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.deleteByUser(user);
        return MessageResponse.builder().message("Password changed successfully").build();
    }

    public MessageResponse forgotPassword(ForgotPassRequest request) {
        var userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isPresent() && userOptional.get().getIsActive()) {
            User user = userOptional.get();
            String otp = otpService.generateAndSavePasswordResetOtp(user);
            emailService.sendOtpEmail(user.getEmail(), otp);
        }
        return MessageResponse.builder()
                .message("Nếu email của bạn tồn tại trong hệ thống, chúng tôi đã gửi một mã OTP để đặt lại mật khẩu.")
                .build();
    }

    public MessageResponse resetPassword(ResetPassRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
        if (!otpService.verifyPasswordResetOtp(user, request.getOtp())) {
            throw new UnauthorizedException(ErrorMessage.INVALID_OTP);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return MessageResponse.builder()
                .message("Đặt lại mật khẩu thành công.")
                .build();
    }

    @Transactional
    public void incrementStrikeCount(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND + userId));

        int currentStrikes = user.getStrikeCount() != null ? user.getStrikeCount() : 0;
        user.setStrikeCount(currentStrikes + 1);
        userRepository.save(user);

        log.warn("User {} đã bị tăng điểm phạt lên {}.", user.getUsername(), user.getStrikeCount());

        if (user.getStrikeCount() >= 3) {
            user.setIsActive(false);
            userRepository.save(user);
            log.warn("User {} đã bị khóa tài khoản do quá nhiều điểm phạt.", user.getUsername());
        }

        // notificationService.createNotification(user, "Bạn đã nhận 1 điểm phạt do không thanh toán.", "/my-strikes");
    }

    public PublicUserProfileResponse getPublicProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND + userId));
        return userMapper.toPublicUserProfileResponse(user);
    }

    public UserProfileResponse getMyProfile() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        User user = userRepository.findByUsername(name).orElseThrow(
                () -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
        var userResponse = userMapper.toUserProfileResponse(user);
        userResponse.setNoPassword(!StringUtils.hasText(user.getPassword()));
        return userResponse;
    }

    // ================Admin===========================
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> getUsers(Boolean isActive, String roleName, int page, int size, String sortDir) {
        Sort.Direction direction = Sort.Direction.DESC;
        if ("asc".equalsIgnoreCase(sortDir) || "oldest".equalsIgnoreCase(sortDir)) {
            direction = Sort.Direction.ASC;
        }
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, "createdAt"));
        Page<User> userPage = userRepository.searchUsers(isActive, roleName, pageable);

        return PageResponse.<UserResponse>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements())
                .data(userPage.getContent().stream()
                        .map(userMapper::toUserResponse)
                        .toList())
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createUserByAdmin(AdminCreationRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new UserAlreadyExistsException(ErrorMessage.USER_ALREADY_EXIST);
        if (userRepository.existsByEmail(request.getEmail()))
            throw new UserAlreadyExistsException(ErrorMessage.EMAIL_ALREADY_EXIST);

        User user = userMapper.toUserFromAdminCreate(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        var roles = new HashSet<Role>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            roles.addAll(roleRepository.findAllById(request.getRoles()));
        } else {
            roleRepository.findById(RoleEnum.USER.name()).ifPresent(roles::add);
        }
        user.setRoles(roles);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setStrikeCount(0);
        user.setReputationScore(0);
        user.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUserByAdmin(String userId, AdminUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
        userMapper.updateUserFromAdminRequest(user, request);
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRoles() != null) {
            var roles = new HashSet<Role>(roleRepository.findAllById(request.getRoles()));
            user.setRoles(roles);
        }
        if (request.getStrikeCount() != null) user.setStrikeCount(request.getStrikeCount());
        if (request.getReputationScore() != null) user.setReputationScore(request.getReputationScore());
        user.setUpdatedAt(LocalDateTime.now());

        return userMapper.toUserResponse(userRepository.save(user));
    }

}
