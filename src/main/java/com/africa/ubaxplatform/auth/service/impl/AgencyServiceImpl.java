package com.africa.ubaxplatform.auth.service.impl;

import com.africa.ubaxplatform.auth.dto.AgencyResponse;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.repository.AgencyRepository;
import com.africa.ubaxplatform.auth.service.interfaces.AgencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgencyServiceImpl implements AgencyService {

  private final AgencyRepository agencyRepository;

  @Override
  @Transactional(readOnly = true)
  public Page<AgencyResponse> listForClient(String city, Pageable pageable) {
    Page<Agency> page =
        (city != null && !city.isBlank())
            ? agencyRepository.findByActiveTrueAndDeletedAtIsNullAndCityIgnoreCase(city, pageable)
            : agencyRepository.findByActiveTrueAndDeletedAtIsNull(pageable);
    return page.map(AgencyServiceImpl::toResponse);
  }

  private static AgencyResponse toResponse(Agency a) {
    return new AgencyResponse(
        a.getId(),
        a.getName(),
        a.getDescription(),
        a.getLogoUrl(),
        a.getAddress(),
        a.getCity(),
        a.getCountry(),
        a.getPhone(),
        a.getEmail(),
        a.getWebsite(),
        a.isVerified());
  }
}
