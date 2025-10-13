package com.thanh.auction_server.Controller;

import com.thanh.auction_server.dto.request.PermissionRequest;
import com.thanh.auction_server.dto.request.RoleRequest;
import com.thanh.auction_server.dto.response.PermissionResponse;
import com.thanh.auction_server.dto.response.RoleResponse;
import com.thanh.auction_server.service.authenticate.PermissionService;
import com.thanh.auction_server.service.authenticate.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/roles")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleController {
    RoleService roleService;
    PermissionService permissionService;

    @PostMapping
    ResponseEntity<RoleResponse> createRole(@RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.createRole(request));
    }

    @GetMapping
    ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAll());
    }

    @DeleteMapping("/{role}")
    ResponseEntity<Void> deleteRole(@PathVariable String role) {
        roleService.delete(role);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/permissions")
    ResponseEntity<PermissionResponse> createPermission(@RequestBody PermissionRequest request) {
        return ResponseEntity.ok(permissionService.create(request));
    }

    @GetMapping("/permissions")
    ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAll());
    }

    @DeleteMapping("/permissions/{permission}")
    ResponseEntity<Void> deletePermission(@PathVariable String permission) {
        permissionService.delete(permission);
        return ResponseEntity.noContent().build();
    }
}
