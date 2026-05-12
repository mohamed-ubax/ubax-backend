package com.africa.ubaxplatform.auth.service.interfaces;

import com.africa.ubaxplatform.auth.dto.AgencyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AgencyService {

  Page<AgencyResponse> listForClient(String city, Pageable pageable);
}
