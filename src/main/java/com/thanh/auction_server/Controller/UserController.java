package com.thanh.auction_server.Controller;

import com.thanh.auction_server.dto.request.*;
import com.thanh.auction_server.dto.response.*;
import com.thanh.auction_server.service.authenticate.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @PostMapping
    ResponseEntity<UserResponse> createUser(@RequestBody @Validated UserCreationRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @PostMapping("/otp")
    ResponseEntity<MessageResponse> createUserOtp(@RequestBody @Validated UserCreationRequest request) {
        return ResponseEntity.ok(userService.createUserOtp(request));
    }

    @PostMapping("/create-password")
    ResponseEntity<Void> createPassword(@RequestBody @Validated PasswordCreationRequest request) {
        userService.createPassword(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    ResponseEntity<UserResponse> updateUser(@PathVariable String id, @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @GetMapping("/{id}")
    ResponseEntity<UserResponse> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @GetMapping("/my-info")
    ResponseEntity<UserResponse> getMyInfo() {
        return ResponseEntity.ok(userService.getMyInfo());
    }

    @GetMapping
    ResponseEntity<PageResponse<UserResponse>> getUsers(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        return ResponseEntity.<PageResponse<UserResponse>>ok(userService.getUsersPagination(page, size));
    }

    @DeleteMapping("/delete/{id}")
    ResponseEntity<String> deleteUser(@PathVariable String id) {
        UserResponse user = userService.getUser(id);
        userService.deleteUser(id);
        return ResponseEntity.ok().body("User '" + user.getUsername() + "' deleted successfully");
    }

    @PostMapping("/change-password")
    ResponseEntity<MessageResponse> changePassword(@RequestBody ChangePassRequest request) {
        return ResponseEntity.ok(userService.changePassword(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody @Validated ForgotPassRequest request) {
        return ResponseEntity.ok(userService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody @Validated ResetPassRequest request) {
        return ResponseEntity.ok(userService.resetPassword(request));
    }

    @GetMapping("/my-profile")
    public ResponseEntity<UserProfileResponse> getPublicProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @GetMapping("/{userId}/public-profile")
    public ResponseEntity<PublicUserProfileResponse> getPublicProfile(@PathVariable String userId) {
        var user = userService.getPublicProfile(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/admin/search")
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        return ResponseEntity.ok(userService.getUsers(isActive, role, page, size, sort));
    }

    @PatchMapping("/admin/{id}/active-status")
    ResponseEntity<String> updateUserActiveStatus(@PathVariable String id, @RequestBody Boolean isActive) {
        return ResponseEntity.ok(userService.updateUserStatus(id, isActive));
    }

    @PostMapping("/admin-create")
    public ResponseEntity<UserResponse> createUserByAdmin(@RequestBody @Valid AdminCreationRequest request) {
        return ResponseEntity.ok(userService.createUserByAdmin(request));
    }

    @PutMapping("/{userId}/admin-update")
    public ResponseEntity<UserResponse> updateUserByAdmin(
            @PathVariable String userId,
            @RequestBody AdminUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUserByAdmin(userId, request));
    }

}
