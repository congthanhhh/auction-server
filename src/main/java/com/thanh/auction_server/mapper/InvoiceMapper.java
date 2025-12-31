package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.response.InvoiceResponse;
import com.thanh.auction_server.entity.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Mapper(componentModel = "spring", uses = {UserMapper.class, ProductMapper.class})
public interface InvoiceMapper {

    @Mapping(source = "user", target = "user")
    @Mapping(source = "product", target = "product")
    @Mapping(source = "auctionSession.id", target = "auctionSessionId")
    @Mapping(target = "paymentTime", source = "paymentTime", qualifiedByName = "parsePaymentTime")
    InvoiceResponse toInvoiceResponse(Invoice invoice);

    @Named("parsePaymentTime")
    default LocalDateTime parsePaymentTime(String paymentTime) {
        if (paymentTime == null || paymentTime.isEmpty()) {
            return null;
        }
        try {
            // Định dạng của VNPay
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(paymentTime, formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
