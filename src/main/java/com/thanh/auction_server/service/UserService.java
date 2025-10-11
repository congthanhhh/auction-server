package com.thanh.auction_server.service;

import com.thanh.auction_server.constants.RoleEnum;
import com.thanh.auction_server.dto.request.PasswordCreationRequest;
import com.thanh.auction_server.dto.request.UserCreationRequest;
import com.thanh.auction_server.dto.request.UserUpdateRequest;
import com.thanh.auction_server.dto.response.MessageResponse;
import com.thanh.auction_server.dto.response.UserResponse;
import com.thanh.auction_server.entity.Role;
import com.thanh.auction_server.entity.User;
import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.exception.UserAlreadyExistsException;
import com.thanh.auction_server.exception.UserNotFoundException;
import com.thanh.auction_server.mapper.UserMapper;
import com.thanh.auction_server.repository.RoleRepository;
import com.thanh.auction_server.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    RoleRepository roleRepository;
    OtpService otpService;
    EmailService emailService;

    public UserResponse createUser(UserCreationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) throw new UserAlreadyExistsException(ErrorMessage.USER_ALREADY_EXIST);

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

    public UserResponse updateUser(String id, UserUpdateRequest request) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
        userMapper.updateUser(user, request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        var roles = roleRepository.findAllById(request.getRoles());
        user.setRoles(new HashSet<>(roles));
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
//    @PreAuthorize("hasAuthority('FULL_EDIT')") //Permission
    public List<UserResponse> getUsers() {
        log.info("In method get Users");
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

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String id) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
        userRepository.delete(user);
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

}
