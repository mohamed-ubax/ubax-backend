package com.africa.ubaxplatform.common.codelist.repository;

import com.africa.ubaxplatform.common.codelist.entity.LaCodeList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

public interface LaCodeListRepository extends CrudRepository<LaCodeList, UUID> {

  List<LaCodeList> findAllByType(String type);

  Optional<LaCodeList> findByTypeAndValue(String type, String value);

  Page<LaCodeList> findAll(Pageable pageable);
}
