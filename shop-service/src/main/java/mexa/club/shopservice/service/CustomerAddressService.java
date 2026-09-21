package mexa.club.shopservice.service;

import mexa.club.shopservice.dto.AddressResponse;
import mexa.club.shopservice.dto.CreateAddressRequest;
import mexa.club.shopservice.dto.UpdateAddressRequest;
import mexa.club.shopservice.entity.CustomerAddress;
import mexa.club.shopservice.exception.ShopNotFoundException;
import mexa.club.shopservice.repository.CustomerAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerAddressService {

    private final CustomerAddressRepository customerAddressRepository;

    public CustomerAddressService(CustomerAddressRepository customerAddressRepository) {
        this.customerAddressRepository = customerAddressRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID userId) {
        return customerAddressRepository.findAllByUserIdOrderByDefaultAddressDescCreatedAtAsc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AddressResponse create(UUID userId, CreateAddressRequest req) {
        if (req.defaultAddress()) {
            clearDefaults(userId);
        }
        CustomerAddress a = new CustomerAddress();
        a.setUserId(userId);
        applyCreate(a, req);
        return toDto(customerAddressRepository.save(a));
    }

    @Transactional
    public AddressResponse update(UUID userId, UUID id, UpdateAddressRequest req) {
        CustomerAddress a = customerAddressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ShopNotFoundException(id, "Address"));
        if (Boolean.TRUE.equals(req.defaultAddress())) {
            clearDefaults(userId);
            a.setDefaultAddress(true);
        } else if (req.defaultAddress() != null && Boolean.FALSE.equals(req.defaultAddress())) {
            a.setDefaultAddress(false);
        }
        if (req.label() != null) {
            a.setLabel(trim(req.label()));
        }
        if (req.name() != null) {
            a.setName(trim(req.name()));
        }
        if (req.latitude() != null) {
            a.setLatitude(req.latitude());
        }
        if (req.longitude() != null) {
            a.setLongitude(req.longitude());
        }
        if (req.line2() != null) {
            a.setLine2(trim(req.line2()));
        }
        if (req.phone() != null) {
            a.setPhone(trim(req.phone()));
        }
        return toDto(customerAddressRepository.save(a));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        CustomerAddress a = customerAddressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ShopNotFoundException(id, "Address"));
        customerAddressRepository.delete(a);
    }

    @Transactional
    public AddressResponse setDefault(UUID userId, UUID id) {
        CustomerAddress a = customerAddressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ShopNotFoundException(id, "Address"));
        clearDefaults(userId);
        a.setDefaultAddress(true);
        return toDto(customerAddressRepository.save(a));
    }

    private void clearDefaults(UUID userId) {
        List<CustomerAddress> rows = customerAddressRepository.findAllByUserIdAndDefaultAddressTrue(userId);
        if (rows.isEmpty()) {
            return;
        }
        List<CustomerAddress> updates = new ArrayList<>(rows.size());
        for (CustomerAddress row : rows) {
            row.setDefaultAddress(false);
            updates.add(row);
        }
        customerAddressRepository.saveAll(updates);
    }

    private void applyCreate(CustomerAddress a, CreateAddressRequest req) {
        a.setLabel(trim(req.label()));
        a.setName(trim(req.name()));
        a.setLatitude(req.latitude());
        a.setLongitude(req.longitude());
        a.setLine2(trim(req.line2()));
        a.setPhone(trim(req.phone()));
        a.setDefaultAddress(req.defaultAddress());
    }

    private static String trim(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private AddressResponse toDto(CustomerAddress a) {
        return new AddressResponse(
                a.getId(),
                a.getLabel(),
                a.getName(),
                a.getLatitude(),
                a.getLongitude(),
                a.getLine2(),
                a.getPhone(),
                a.isDefaultAddress(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
