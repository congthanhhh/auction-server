package com.thanh.auction_server.service.admin;

import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.entity.AuditLog;
import com.thanh.auction_server.repository.AuditLogRepository;
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class AuditLogService {
    AuditLogRepository auditLogRepository;

    public void saveLog(String action, String targetId, String content) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        AuditLog log = AuditLog.builder()
                .adminUsername(currentUsername)
                .action(action)
                .targetId(targetId)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AuditLog> getLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<AuditLog> pageData = auditLogRepository.findAll(pageable);

        return PageResponse.<AuditLog>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(pageData.getTotalPages())
                .totalElements(pageData.getTotalElements())
                .data(pageData.getContent())
                .build();
    }
}
