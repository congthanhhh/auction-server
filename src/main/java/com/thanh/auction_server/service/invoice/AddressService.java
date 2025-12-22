package com.thanh.auction_server.service.invoice;

import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.dto.request.AddressRequest;
import com.thanh.auction_server.dto.response.AddressResponse;
import com.thanh.auction_server.entity.Address;
import com.thanh.auction_server.entity.User;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.exception.UnauthorizedException;
import com.thanh.auction_server.exception.UserNotFoundException;
import com.thanh.auction_server.mapper.AddressMapper;
import com.thanh.auction_server.repository.AddressRepository;
import com.thanh.auction_server.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class AddressService {
    AddressRepository addressRepository;
    UserRepository userRepository;
    AddressMapper addressMapper;


    @Transactional
    public AddressResponse createAddress(AddressRequest request) {
        User currentUser = getCurrentUser();
        List<Address> existingAddresses = addressRepository.findByUser_Id(currentUser.getId());
        boolean isDefault = request.getIsDefault() != null && request.getIsDefault();
        if (existingAddresses.isEmpty()) {
            isDefault = true;
        }
        if (isDefault) {
            unsetOldDefault(currentUser.getId());
        }
        //Ở đây đơn giản hóa: User chỉ set True
        Address address = addressMapper.toAddress(request);
        address.setUser(currentUser);
        address.setIsDefault(isDefault);
        return addressMapper.toAddressResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(Long addressId, AddressRequest request) {
        User currentUser = getCurrentUser();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED_ACCESS);
        }
        unsetOldDefault(currentUser.getId());
        addressMapper.updateAddress(address, request);
        return addressMapper.toAddressResponse(addressRepository.save(address));
    }

    @Transactional
    public void setDefaultAddress(Long addressId) {
        User currentUser = getCurrentUser();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED_ACCESS);
        }
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            return;
        }
        unsetOldDefault(currentUser.getId());
        address.setIsDefault(true);
        addressRepository.save(address);
    }

    @Transactional
    public void deleteAddress(Long addressId) {
        User currentUser = getCurrentUser();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED_ACCESS);
        }
        addressRepository.delete(address);
    }

    public List<AddressResponse> getMyAddresses() {
        User currentUser = getCurrentUser();
        return addressRepository.findByUser_Id(currentUser.getId()).stream()
                .map(addressMapper::toAddressResponse)
                .collect(Collectors.toList());
    }

    public AddressResponse getAddress(Long id) {
        User currentUser = getCurrentUser();
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Access denied");
        }
        return addressMapper.toAddressResponse(address);
    }

    private void unsetOldDefault(String userId) {
        Address oldDefault = addressRepository.findByUser_IdAndIsDefaultTrue(userId);
        if (oldDefault != null) {
            oldDefault.setIsDefault(false);
            addressRepository.save(oldDefault);
        }
    }

    private User getCurrentUser() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND + currentUsername));
    }
}
