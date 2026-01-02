package com.thanh.auction_server.Controller;

import com.thanh.auction_server.dto.request.AddressRequest;
import com.thanh.auction_server.dto.response.AddressResponse;
import com.thanh.auction_server.dto.response.MessageResponse;
import com.thanh.auction_server.service.invoice.AddressService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/address")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressController {
    AddressService addressService;

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(@RequestBody @Valid AddressRequest request) {
        return ResponseEntity.ok(addressService.createAddress(request));
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getMyAddresses() {
        return ResponseEntity.ok(addressService.getMyAddresses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getAddress(@PathVariable Long id) {
        return ResponseEntity.ok(addressService.getAddress(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long id, @RequestBody @Valid AddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(id, request));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<MessageResponse> setDefaultAddress(@PathVariable Long id) {
        addressService.setDefaultAddress(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Đặt địa chỉ mặc định thành công.")
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(id);
        return ResponseEntity.ok("Address deleted successfully");
    }
}
