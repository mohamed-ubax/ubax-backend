package com.africa.ubaxplatform.auth.service.impl;

import com.africa.ubaxplatform.auth.dto.HotelResponse;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.repository.HotelRepository;
import com.africa.ubaxplatform.auth.service.interfaces.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

  private final HotelRepository hotelRepository;

  @Override
  @Transactional(readOnly = true)
  public Page<HotelResponse> listForClient(String city, Pageable pageable) {
    Page<Hotel> page =
        (city != null && !city.isBlank())
            ? hotelRepository.findByActiveAndDeletedAtIsNullAndCityIgnoreCase(true, city, pageable)
            : hotelRepository.findByActiveAndDeletedAtIsNull(true, pageable);
    return page.map(HotelServiceImpl::toResponse);
  }

  private static HotelResponse toResponse(Hotel h) {
    return new HotelResponse(
        h.getId(),
        h.getName(),
        h.getDescription(),
        h.getLogoUrl(),
        h.getAddress(),
        h.getCity(),
        h.getCountry(),
        h.getPhone(),
        h.getEmail(),
        h.getWebsite(),
        h.getStars(),
        h.getTotalRooms(),
        h.isVerified());
  }
}
