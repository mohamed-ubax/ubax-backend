package com.africa.ubaxplatform.auth.service.interfaces;

import com.africa.ubaxplatform.auth.dto.HotelResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HotelService {

  Page<HotelResponse> listForClient(String city, Pageable pageable);
}
