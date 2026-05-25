package com.africa.ubaxplatform.unit.favorite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.favorite.dto.FavoriteResponse;
import com.africa.ubaxplatform.favorite.entity.PropertyFavorite;
import com.africa.ubaxplatform.favorite.repository.PropertyFavoriteRepository;
import com.africa.ubaxplatform.favorite.service.impl.FavoriteServiceImpl;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.repository.PropertyMediaRepository;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriteServiceImpl")
class FavoriteServiceImplTest {

  @Mock PropertyFavoriteRepository favoriteRepo;
  @Mock PropertyRepository propertyRepo;
  @Mock UserRepository userRepo;
  @Mock PropertyMediaRepository mediaRepo;

  @InjectMocks FavoriteServiceImpl service;

  private static final String KEYCLOAK_ID = SharedTestFixtures.KEYCLOAK_ID;
  private static final UUID USER_ID = SharedTestFixtures.USER_ID;
  private static final UUID PROPERTY_ID = SharedTestFixtures.PROPERTY_ID;
  private static final UUID FAVORITE_ID = UUID.randomUUID();

  private User clientUser;
  private Property property;

  @BeforeEach
  void setUp() {
    clientUser = SharedTestFixtures.buildUserWithoutAgency(KEYCLOAK_ID, USER_ID);

    property = Property.builder().title("Appartement T3 Cocody").build();
    SharedTestFixtures.injectId(property, PROPERTY_ID);
  }

  // ── add ──────────────────────────────────────────────────────────

  @Nested
  @DisplayName("add()")
  class Add {

    @Test
    @DisplayName(
        "Favori déjà existant – retourne le favori existant sans re-sauvegarder (idempotent)")
    void add_alreadyFavorited_returnsExistingWithoutSaving() throws Exception {
      PropertyFavorite existing =
          PropertyFavorite.builder().user(clientUser).property(property).build();
      SharedTestFixtures.injectId(existing, FAVORITE_ID);

      when(favoriteRepo.findByUserKeycloakIdAndPropertyId(KEYCLOAK_ID, PROPERTY_ID))
          .thenReturn(Optional.of(existing));

      FavoriteResponse result = service.add(KEYCLOAK_ID, PROPERTY_ID);

      assertThat(result.favoriteId()).isEqualTo(FAVORITE_ID);
      assertThat(result.propertyId()).isEqualTo(PROPERTY_ID);
      verify(favoriteRepo, never()).save(any());
      verify(userRepo, never()).findByKeycloakId(any());
    }

    @Test
    @DisplayName("Utilisateur non trouvé – lève NotFoundException")
    void add_userNotFound_throwsNotFoundException() {
      when(favoriteRepo.findByUserKeycloakIdAndPropertyId(KEYCLOAK_ID, PROPERTY_ID))
          .thenReturn(Optional.empty());
      when(userRepo.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.add(KEYCLOAK_ID, PROPERTY_ID))
          .isInstanceOf(NotFoundException.class);

      verify(favoriteRepo, never()).save(any());
    }

    @Test
    @DisplayName("Bien non trouvé – lève NotFoundException")
    void add_propertyNotFound_throwsNotFoundException() {
      when(favoriteRepo.findByUserKeycloakIdAndPropertyId(KEYCLOAK_ID, PROPERTY_ID))
          .thenReturn(Optional.empty());
      when(userRepo.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(clientUser));
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.add(KEYCLOAK_ID, PROPERTY_ID))
          .isInstanceOf(NotFoundException.class);

      verify(favoriteRepo, never()).save(any());
    }

    @Test
    @DisplayName("Nouvel ajout – sauvegarde et retourne le FavoriteResponse")
    void add_newFavorite_savesAndReturnsFavoriteResponse() throws Exception {
      PropertyFavorite saved =
          PropertyFavorite.builder().user(clientUser).property(property).build();
      SharedTestFixtures.injectId(saved, FAVORITE_ID);

      when(favoriteRepo.findByUserKeycloakIdAndPropertyId(KEYCLOAK_ID, PROPERTY_ID))
          .thenReturn(Optional.empty());
      when(userRepo.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(clientUser));
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(favoriteRepo.save(any(PropertyFavorite.class))).thenReturn(saved);

      FavoriteResponse result = service.add(KEYCLOAK_ID, PROPERTY_ID);

      assertThat(result.favoriteId()).isEqualTo(FAVORITE_ID);
      assertThat(result.propertyId()).isEqualTo(PROPERTY_ID);
      verify(favoriteRepo).save(any(PropertyFavorite.class));
    }
  }

  // ── remove ───────────────────────────────────────────────────────

  @Nested
  @DisplayName("remove()")
  class Remove {

    @Test
    @DisplayName("Favori non trouvé – lève NotFoundException")
    void remove_favoriteNotFound_throwsNotFoundException() {
      when(favoriteRepo.existsByUserKeycloakIdAndPropertyId(KEYCLOAK_ID, PROPERTY_ID))
          .thenReturn(false);

      assertThatThrownBy(() -> service.remove(KEYCLOAK_ID, PROPERTY_ID))
          .isInstanceOf(NotFoundException.class);

      verify(favoriteRepo, never()).deleteByUserKeycloakIdAndPropertyId(any(), any());
    }

    @Test
    @DisplayName("Suppression réussie – appelle deleteByUserKeycloakIdAndPropertyId")
    void remove_favoriteExists_deletesSuccessfully() throws Exception {
      when(favoriteRepo.existsByUserKeycloakIdAndPropertyId(KEYCLOAK_ID, PROPERTY_ID))
          .thenReturn(true);

      service.remove(KEYCLOAK_ID, PROPERTY_ID);

      verify(favoriteRepo).deleteByUserKeycloakIdAndPropertyId(KEYCLOAK_ID, PROPERTY_ID);
    }
  }

  // ── listMine ─────────────────────────────────────────────────────

  @Nested
  @DisplayName("listMine()")
  class ListMine {

    @Test
    @DisplayName("Aucun favori – retourne page vide sans exception")
    void listMine_noFavorites_returnsEmptyPage() {
      when(favoriteRepo.findByUserKeycloakId(KEYCLOAK_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of()));

      var result = service.listMine(KEYCLOAK_ID, Pageable.unpaged());

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName(
        "Un favori avec médias vides – retourne PropertyDetailResponse avec coverPhotoUrl null")
    void listMine_oneFavoriteNoMedia_returnsDetailWithNullCover() {
      PropertyFavorite fav = PropertyFavorite.builder().user(clientUser).property(property).build();
      SharedTestFixtures.injectId(fav, FAVORITE_ID);

      when(favoriteRepo.findByUserKeycloakId(KEYCLOAK_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(fav)));
      when(mediaRepo.findByPropertyIdOrderBySortOrderAsc(PROPERTY_ID)).thenReturn(List.of());

      var result = service.listMine(KEYCLOAK_ID, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      var detail = result.getContent().getFirst();
      assertThat(detail.property().id()).isEqualTo(PROPERTY_ID);
      assertThat(detail.property().coverPhotoUrl()).isNull();
      assertThat(detail.media()).isEmpty();
    }

    @Test
    @DisplayName("Plusieurs favoris – tous retournés dans la page")
    void listMine_multipleFavorites_allReturned() {
      UUID propId2 = UUID.randomUUID();
      Property property2 = Property.builder().title("Villa Cocody").build();
      SharedTestFixtures.injectId(property2, propId2);

      PropertyFavorite fav1 =
          PropertyFavorite.builder().user(clientUser).property(property).build();
      SharedTestFixtures.injectId(fav1, UUID.randomUUID());

      PropertyFavorite fav2 =
          PropertyFavorite.builder().user(clientUser).property(property2).build();
      SharedTestFixtures.injectId(fav2, UUID.randomUUID());

      when(favoriteRepo.findByUserKeycloakId(KEYCLOAK_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(fav1, fav2)));
      when(mediaRepo.findByPropertyIdOrderBySortOrderAsc(PROPERTY_ID)).thenReturn(List.of());
      when(mediaRepo.findByPropertyIdOrderBySortOrderAsc(propId2)).thenReturn(List.of());

      var result = service.listMine(KEYCLOAK_ID, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(2);
    }
  }
}
