package com.africa.ubaxplatform.favorite.service.impl;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.favorite.dto.FavoriteResponse;
import com.africa.ubaxplatform.favorite.entity.PropertyFavorite;
import com.africa.ubaxplatform.favorite.repository.PropertyFavoriteRepository;
import com.africa.ubaxplatform.favorite.service.interfaces.FavoriteService;
import com.africa.ubaxplatform.property.dto.PropertyDetailResponse;
import com.africa.ubaxplatform.property.dto.PropertyMediaResponse;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.mapper.PropertyMapper;
import com.africa.ubaxplatform.property.repository.PropertyMediaRepository;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteServiceImpl implements FavoriteService {

  private final PropertyFavoriteRepository favoriteRepo;
  private final PropertyRepository propertyRepo;
  private final UserRepository userRepo;
  private final PropertyMediaRepository mediaRepo;

  @Override
  @Transactional
  public FavoriteResponse add(String keycloakId, UUID propertyId) throws CustomException {
    Optional<PropertyFavorite> existing =
        favoriteRepo.findByUserKeycloakIdAndPropertyId(keycloakId, propertyId);
    if (existing.isPresent()) {
      PropertyFavorite fav = existing.get();
      return new FavoriteResponse(fav.getId(), propertyId, fav.getCreatedAt());
    }

    User user =
        userRepo
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND));

    Property property =
        propertyRepo
            .findById(propertyId)
            .orElseThrow(
                () ->
                    new NotFoundException(ResponseMessageConstants.PROPERTY_GET_FAILURE_NOT_FOUND));

    PropertyFavorite saved =
        favoriteRepo.save(PropertyFavorite.builder().user(user).property(property).build());

    log.info("Favori ajouté : userId={}, propertyId={}", user.getId(), propertyId);
    return new FavoriteResponse(saved.getId(), propertyId, saved.getCreatedAt());
  }

  @Override
  @Transactional
  public void remove(String keycloakId, UUID propertyId) throws CustomException {
    if (!favoriteRepo.existsByUserKeycloakIdAndPropertyId(keycloakId, propertyId)) {
      throw new NotFoundException(ResponseMessageConstants.FAVORITE_NOT_FOUND);
    }
    favoriteRepo.deleteByUserKeycloakIdAndPropertyId(keycloakId, propertyId);
    log.info("Favori supprimé : keycloakId={}, propertyId={}", keycloakId, propertyId);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PropertyDetailResponse> listMine(String keycloakId, Pageable pageable) {
    return favoriteRepo
        .findByUserKeycloakId(keycloakId, pageable)
        .map(
            fav -> {
              Property p = fav.getProperty();
              List<PropertyMediaResponse> media =
                  mediaRepo.findByPropertyIdOrderBySortOrderAsc(p.getId()).stream()
                      .map(PropertyMapper::toMediaResponse)
                      .toList();
              String coverPhotoUrl =
                  media.stream()
                      .filter(PropertyMediaResponse::cover)
                      .map(PropertyMediaResponse::fileUrl)
                      .findFirst()
                      .orElse(null);
              return new PropertyDetailResponse(
                  PropertyMapper.toResponse(p, coverPhotoUrl), media, null);
            });
  }
}
