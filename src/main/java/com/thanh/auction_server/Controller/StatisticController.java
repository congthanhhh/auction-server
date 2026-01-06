package com.thanh.auction_server.Controller;

import com.thanh.auction_server.dto.response.StatisticResponse;
import com.thanh.auction_server.service.admin.StatisticService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/statistics")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatisticController {
    private final StatisticService statisticService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StatisticResponse> getStatistics(@RequestParam(required = false) Integer month,
                                                           @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(statisticService.getDashboardStatistics(month, year));
    }
}
