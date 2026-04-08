package com.africa.ubaxplatform.common.codelist.service.impl;

import com.africa.ubaxplatform.common.codelist.entity.LaCodeList;
import com.africa.ubaxplatform.common.codelist.repository.LaCodeListRepository;
import com.africa.ubaxplatform.common.codelist.service.interfaces.LaCodeListService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import jakarta.persistence.EntityExistsException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LaCodeListServiceImpl implements LaCodeListService {

  private final LaCodeListRepository codeListRepository;

  @Override
  public List<LaCodeList> findAllByType(String type) {
    return codeListRepository.findAllByType(type);
  }

  @Override
  public Page<LaCodeList> findAll(Pageable pageable) {
    return codeListRepository.findAll(pageable);
  }

  @Override
  public LaCodeList findById(UUID id) throws CustomException {
    if (id == null) {
      throw new CustomException(
          new IllegalArgumentException("L'identifiant du code list est obligatoire"),
          ResponseMessageConstants.CODELIST_GET_FAILURE_BAD_REQUEST);
    }
    return codeListRepository
        .findById(id)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("CodeList introuvable avec l'id : " + id),
                    ResponseMessageConstants.CODELIST_GET_FAILURE_NOT_FOUND));
  }

  @Override
  public LaCodeList createCodeList(LaCodeList codeList) throws CustomException {
    try {
      return codeListRepository.save(codeList);
    } catch (DataIntegrityViolationException ex) {
      log.error("Duplicate code list type/value : {}", ex.getMessage());
      throw new CustomException(
          new EntityExistsException("Un code list avec ce type et cette valeur existe déjà"),
          ResponseMessageConstants.CODELIST_POST_DUPLICATE);
    } catch (Exception ex) {
      log.error("Erreur lors de la création du code list : {}", ex.getMessage());
      throw new CustomException(ex, ResponseMessageConstants.CODELIST_POST_FAILURE);
    }
  }

  @Override
  public LaCodeList updateCodeList(LaCodeList codeList, UUID id) throws CustomException {
    if (codeList == null || !id.equals(codeList.getId())) {
      throw new CustomException(
          new IllegalArgumentException("L'id dans le body ne correspond pas à l'id du path"),
          ResponseMessageConstants.CODELIST_PUT_FAILURE_BAD_REQUEST);
    }
    findById(id);
    try {
      return codeListRepository.save(codeList);
    } catch (DataIntegrityViolationException ex) {
      log.error("Duplicate code list type/value : {}", ex.getMessage());
      throw new CustomException(
          new EntityExistsException("Un code list avec ce type et cette valeur existe déjà"),
          ResponseMessageConstants.CODELIST_POST_DUPLICATE);
    } catch (Exception ex) {
      log.error("Erreur lors de la mise à jour du code list : {}", ex.getMessage());
      throw new CustomException(ex, ResponseMessageConstants.CODELIST_PUT_FAILURE);
    }
  }
}
