package com.thanh.auction_server.Controller;

import com.thanh.auction_server.entity.SystemParameter;
import com.thanh.auction_server.service.admin.SystemParameterService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/settings")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemParameterController {

    SystemParameterService systemParameterService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SystemParameter>> getAllSettings() {
        return ResponseEntity.ok(systemParameterService.getAllConfigs());
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemParameter> updateSetting(@PathVariable String key, @RequestBody Map<String, String> request) {
        String newValue = request.get("value");
        return ResponseEntity.ok(systemParameterService.updateConfig(key, newValue));
    }
}
