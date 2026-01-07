package com.thanh.auction_server.service.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanh.auction_server.constants.LogAction;
import com.thanh.auction_server.constants.SystemConfigKey;
import com.thanh.auction_server.entity.SystemParameter;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.repository.SystemParameterRepository;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class SystemParameterService {
    SystemParameterRepository parameterRepository;
    ObjectMapper objectMapper;
    AuditLogService auditLogService;

    // Chạy hàm này khi khởi động App để đảm bảo DB luôn có dữ liệu
    @PostConstruct
    public void initDefaultParameters() {
        createIfNotExists(SystemConfigKey.LISTING_FEE_PERCENT, "0.05", "Phí đăng bài giá sàn theo phần trăm (0.05 = 5%)");
        createIfNotExists(SystemConfigKey.INVOICE_PAYMENT_DUE_DAYS, "4", "Số ngày tối đa để thanh toán hóa đơn");
        createIfNotExists(SystemConfigKey.INVOICE_AUTO_COMPLETED_DAYS, "15", "Số ngày tự động xác nhận hoàn thành đơn hàng");

    }
    private void createIfNotExists(String key, String defaultValue, String desc) {
        if (parameterRepository.findByParamKey(key).isEmpty()) {
            parameterRepository.save(SystemParameter.builder()
                    .paramKey(key)
                    .paramValue(defaultValue)
                    .description(desc)
                    .build());
            log.info("Initialized system parameter: {}", key);
        }
    }

    // Lấy config dạng String
    public String getConfig(String key) {
        return parameterRepository.findByParamKey(key)
                .map(SystemParameter::getParamValue)
                .orElseThrow(() -> new ResourceNotFoundException("Config not found: " + key));
    }

    // Lấy config dạng BigDecimal (Dùng cho tiền)
    public BigDecimal getBigDecimalConfig(String key) {
        try {
            return new BigDecimal(getConfig(key));
        } catch (NumberFormatException e) {
            log.error("Config key {} is not a valid number", key);
            return BigDecimal.ZERO;
        }
    }

    // Lấy config dạng Integer (Dùng cho ngày tháng)
    public int getIntConfig(String key) {
        try {
            return Integer.parseInt(getConfig(key));
        } catch (NumberFormatException e) {
            log.error("Config key {} is not a valid integer", key);
            return 0;
        }
    }

    // Cho Admin update config
    public SystemParameter updateConfig(String key, String newValue) {
        SystemParameter param = parameterRepository.findByParamKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Config not found: " + key));
        String oldValue = param.getParamValue();
        param.setParamValue(newValue);
        SystemParameter saved = parameterRepository.save(param);
        String details = "Thay đổi cấu hình '" + key + "': " + oldValue + " -> " + newValue;
        auditLogService.saveLog(LogAction.UPDATE_SYSTEM_CONFIG, key, details);
        return saved;
    }

    public List<SystemParameter> getAllConfigs() {
        return parameterRepository.findAll();
    }
}
