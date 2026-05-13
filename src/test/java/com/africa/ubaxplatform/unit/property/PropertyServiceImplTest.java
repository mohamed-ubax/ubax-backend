package com.africa.ubaxplatform.unit.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import com.africa.ubaxplatform.property.dto.PropertyBoostRequest;
import com.africa.ubaxplatform.property.dto.PropertyCreateRequest;
import com.africa.ubaxplatform.property.dto.PropertyDocumentAddRequest;
import com.africa.ubaxplatform.property.dto.PropertyExpirationRequest;
import com.africa.ubaxplatform.property.dto.PropertyMediaAddRequest;
import com.africa.ubaxplatform.property.dto.PropertyStatusUpdateRequest;
import com.africa.ubaxplatform.property.dto.PropertyUpdateRequest;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.entity.PropertyDocument;
import com.africa.ubaxplatform.property.entity.PropertyMedia;
import com.africa.ubaxplatform.property.repository.PropertyAmenityRepository;
import com.africa.ubaxplatform.property.repository.PropertyDocumentRepository;
import com.africa.ubaxplatform.property.repository.PropertyMediaRepository;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.property.service.impl.PropertyServiceImpl;
import com.africa.ubaxplatform.storage.service.interfaces.MinioService;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("PropertyServiceImpl")
class PropertyServiceImplTest {

  @Mock PropertyRepository propertyRepo;
  @Mock PropertyAmenityRepository amenityRepo;
  @Mock PropertyMediaRepository mediaRepo;
  @Mock PropertyDocumentRepository docRepo;
  @Mock UserRepository userRepo;
  @Mock MinioService minioService;

  @InjectMocks PropertyServiceImpl service;

  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID MEDIA_ID = UUID.randomUUID();
  private static final UUID DOC_ID = UUID.randomUUID();

  private User caller;
  private Agency agency;
  private Property property;

  @BeforeEach
  void setUp() {
    agency = SharedTestFixtures.buildAgency();
    caller = SharedTestFixtures.buildPartnerUser();

    property =
        Property.builder()
            .owner(caller)
            .agency(agency)
            .title("Villa Test Dakar")
            .description("Belle villa 4ch")
            .propertyType("VILLA")
            .transactionType("RENT")
            .price(BigDecimal.valueOf(500_000))
            .city("Dakar")
            .status(PropertyStatus.DRAFT)
            .build();
    SharedTestFixtures.injectId(property, PROPERTY_ID);
  }

  // create

  @Nested
  @DisplayName("create()")
  class Create {

    @Test
    @DisplayName("Succès – bien créé en DRAFT sans amenities")
    void create_success_noAmenities() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.save(any(Property.class))).thenReturn(property);

      var req = minimalCreateRequest(null);
      var result = service.create(SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(result).isNotNull();
      verify(propertyRepo).save(any(Property.class));
    }

    @Test
    @DisplayName("Echec – utilisateur introuvable → USER_NOT_FOUND")
    void create_userNotFound_throws() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.create(SharedTestFixtures.KEYCLOAK_ID, minimalCreateRequest(null)))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – ownerId spécifié introuvable → USER_NOT_FOUND")
    void create_ownerNotFound_throws() throws CustomException {
      UUID otherId = UUID.randomUUID();
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(userRepo.findById(otherId)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.create(SharedTestFixtures.KEYCLOAK_ID, minimalCreateRequest(otherId)))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }
  }

  // getById

  @Nested
  @DisplayName("getById()")
  class GetById {

    @Test
    @DisplayName("Succès – retourne le détail du bien avec médias et documents")
    void getById_success() throws CustomException {
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(mediaRepo.findByPropertyIdOrderBySortOrderAsc(PROPERTY_ID)).thenReturn(List.of());
      when(docRepo.findByPropertyIdOrderByCreatedAtDesc(PROPERTY_ID)).thenReturn(List.of());

      var result = service.getById(PROPERTY_ID);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Echec – bien introuvable → PROPERTY_GET_FAILURE_NOT_FOUND")
    void getById_notFound_throws() {
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getById(PROPERTY_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PROPERTY_GET_FAILURE_NOT_FOUND);
    }
  }

  // list

  @Nested
  @DisplayName("list()")
  class ListProperties {

    @Test
    @DisplayName("Retourne une page de biens filtrée par ville")
    void list_returnsPage() {
      when(propertyRepo.findWithFilters(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(new PageImpl<>(List.of(property)));

      var result =
          service.list(
              PropertyStatus.PUBLISHED,
              "Dakar",
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              PageRequest.of(0, 20));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Filtre par agencyId – retourne les biens de l'agence")
    void list_filterByAgencyId_returnsAgencyProperties() {
      when(propertyRepo.findWithFilters(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(new PageImpl<>(List.of(property)));

      var result =
          service.list(
              PropertyStatus.PUBLISHED,
              null,
              null,
              null,
              null,
              null,
              SharedTestFixtures.AGENCY_ID,
              null,
              null,
              PageRequest.of(0, 20));

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).agencyId()).isEqualTo(SharedTestFixtures.AGENCY_ID);
    }

    @Test
    @DisplayName("Filtre par hotelId – retourne les chambres de l'hôtel")
    void list_filterByHotelId_returnsHotelRooms() {
      UUID hotelId = UUID.randomUUID();
      Hotel hotel = buildHotel(hotelId);
      Property hotelRoom =
          Property.builder()
              .owner(caller)
              .hotel(hotel)
              .title("Suite Présidentielle")
              .propertyType("HOTEL_SUITE")
              .transactionType("SHORT_STAY")
              .price(BigDecimal.valueOf(150_000))
              .city("Abidjan")
              .status(PropertyStatus.PUBLISHED)
              .build();
      SharedTestFixtures.injectId(hotelRoom, UUID.randomUUID());

      when(propertyRepo.findWithFilters(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(new PageImpl<>(List.of(hotelRoom)));

      var result =
          service.list(
              PropertyStatus.PUBLISHED,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              hotelId,
              PageRequest.of(0, 20));

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).hotelId()).isEqualTo(hotelId);
      assertThat(result.getContent().get(0).hotelName()).isEqualTo("Hôtel Test CI");
    }

    @Test
    @DisplayName("Filtre par hotelId – page vide si hôtel sans biens")
    void list_filterByHotelId_emptyWhenNoRooms() {
      when(propertyRepo.findWithFilters(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(new PageImpl<>(List.of()));

      var result =
          service.list(
              PropertyStatus.PUBLISHED,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              UUID.randomUUID(),
              PageRequest.of(0, 20));

      assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Bien sans hôtel – hotelId et hotelName sont null dans la réponse")
    void list_propertyWithoutHotel_hotelFieldsAreNull() {
      when(propertyRepo.findWithFilters(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(new PageImpl<>(List.of(property)));

      var result =
          service.list(
              PropertyStatus.PUBLISHED,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              PageRequest.of(0, 20));

      assertThat(result.getContent().get(0).hotelId()).isNull();
      assertThat(result.getContent().get(0).hotelName()).isNull();
    }

    @Test
    @DisplayName("Bien avec hôtel – hotelId et hotelName présents dans la réponse")
    void list_propertyWithHotel_responseContainsHotelInfo() {
      UUID hotelId = UUID.randomUUID();
      Hotel hotel = buildHotel(hotelId);
      Property hotelRoom =
          Property.builder()
              .owner(caller)
              .hotel(hotel)
              .title("Chambre Deluxe")
              .propertyType("HOTEL_SUITE")
              .transactionType("SHORT_STAY")
              .price(BigDecimal.valueOf(75_000))
              .city("Abidjan")
              .status(PropertyStatus.PUBLISHED)
              .build();
      SharedTestFixtures.injectId(hotelRoom, UUID.randomUUID());

      when(propertyRepo.findWithFilters(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(new PageImpl<>(List.of(hotelRoom)));

      var result =
          service.list(
              PropertyStatus.PUBLISHED,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              PageRequest.of(0, 20));

      var response = result.getContent().get(0);
      assertThat(response.hotelId()).isEqualTo(hotelId);
      assertThat(response.hotelName()).isEqualTo("Hôtel Test CI");
      assertThat(response.agencyId()).isNull();
    }
  }

  // listMine

  @Nested
  @DisplayName("listMine()")
  class ListMine {

    @Test
    @DisplayName("PARTNER avec agence – filtre par agencyId")
    void listMine_withAgency() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findWithFilters(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(new PageImpl<>(List.of(property)));

      var result = service.listMine(SharedTestFixtures.KEYCLOAK_ID, null, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("OWNER sans agence – filtre par ownerId")
    void listMine_withoutAgency() throws CustomException {
      User ownerOnly = SharedTestFixtures.buildUserWithoutAgency("kc-owner", UUID.randomUUID());
      when(userRepo.findByKeycloakId("kc-owner")).thenReturn(Optional.of(ownerOnly));
      when(propertyRepo.findWithFilters(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(new PageImpl<>(List.of()));

      var result = service.listMine("kc-owner", null, Pageable.unpaged());

      assertThat(result.getContent()).isEmpty();
    }
  }

  // update

  @Nested
  @DisplayName("update()")
  class Update {

    @Test
    @DisplayName("Succès – bien DRAFT mis à jour (tous champs null → aucun changement)")
    void update_draft_success() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(propertyRepo.save(property)).thenReturn(property);

      var req = emptyUpdateRequest();
      var result = service.update(PROPERTY_ID, SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Echec – bien PUBLISHED non modifiable → PROPERTY_UPDATE_FAILURE")
    void update_publishedStatus_throws() {
      property.setStatus(PropertyStatus.PUBLISHED);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

      assertThatThrownBy(
              () ->
                  service.update(PROPERTY_ID, SharedTestFixtures.KEYCLOAK_ID, emptyUpdateRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PROPERTY_UPDATE_FAILURE);
    }

    @Test
    @DisplayName("Echec – appelant non propriétaire → PROPERTY_UPDATE_FAILURE")
    void update_notOwner_throws() {
      User other = SharedTestFixtures.buildUserWithoutAgency("kc-other", UUID.randomUUID());
      when(userRepo.findByKeycloakId("kc-other")).thenReturn(Optional.of(other));
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

      assertThatThrownBy(() -> service.update(PROPERTY_ID, "kc-other", emptyUpdateRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PROPERTY_UPDATE_FAILURE);
    }
  }

  // submit

  @Nested
  @DisplayName("submit()")
  class Submit {

    @Test
    @DisplayName("Succès – DRAFT → PENDING")
    void submit_draft_success() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(propertyRepo.save(property)).thenReturn(property);

      service.submit(PROPERTY_ID, SharedTestFixtures.KEYCLOAK_ID);

      assertThat(property.getStatus()).isEqualTo(PropertyStatus.PENDING);
    }

    @Test
    @DisplayName("Echec – bien déjà PENDING → PROPERTY_UPDATE_FAILURE")
    void submit_notDraft_throws() {
      property.setStatus(PropertyStatus.PENDING);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

      assertThatThrownBy(() -> service.submit(PROPERTY_ID, SharedTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PROPERTY_UPDATE_FAILURE);
    }
  }

  // archive

  @Nested
  @DisplayName("archive()")
  class Archive {

    @Test
    @DisplayName("Succès – statut passe à ARCHIVED")
    void archive_success() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(propertyRepo.save(property)).thenReturn(property);

      service.archive(PROPERTY_ID, SharedTestFixtures.KEYCLOAK_ID);

      assertThat(property.getStatus()).isEqualTo(PropertyStatus.ARCHIVED);
    }
  }

  // updateStatus

  @Nested
  @DisplayName("updateStatus()")
  class UpdateStatus {

    @Test
    @DisplayName("Succès – PUBLISHED avec publishedAt initialisé")
    void updateStatus_published_success() throws CustomException {
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(propertyRepo.save(property)).thenReturn(property);

      service.updateStatus(
          PROPERTY_ID, new PropertyStatusUpdateRequest(PropertyStatus.PUBLISHED, null));

      assertThat(property.getStatus()).isEqualTo(PropertyStatus.PUBLISHED);
      assertThat(property.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Succès – REJECTED avec motif")
    void updateStatus_rejected_withReason_success() throws CustomException {
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(propertyRepo.save(property)).thenReturn(property);

      service.updateStatus(
          PROPERTY_ID,
          new PropertyStatusUpdateRequest(PropertyStatus.REJECTED, "Photos manquantes"));

      assertThat(property.getStatus()).isEqualTo(PropertyStatus.REJECTED);
    }

    @Test
    @DisplayName("Echec – REJECTED sans motif → PROPERTY_UPDATE_FAILURE")
    void updateStatus_rejected_noReason_throws() {
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

      assertThatThrownBy(
              () ->
                  service.updateStatus(
                      PROPERTY_ID, new PropertyStatusUpdateRequest(PropertyStatus.REJECTED, "")))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PROPERTY_UPDATE_FAILURE);
    }
  }

  // boost / expiration

  @Nested
  @DisplayName("Boost & Expiration (admin)")
  class BoostExpiration {

    @BeforeEach
    void stubProperty() {
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(propertyRepo.save(property)).thenReturn(property);
    }

    @Test
    @DisplayName("boost – setBoosted=true et boostExpiresAt calculé")
    void boost_success() throws CustomException {
      service.boost(PROPERTY_ID, new PropertyBoostRequest(30));

      assertThat(property.isBoosted()).isTrue();
      assertThat(property.getBoostExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("removeBoost – setBoosted=false et boostExpiresAt=null")
    void removeBoost_success() throws CustomException {
      property.setBoosted(true);
      service.removeBoost(PROPERTY_ID);

      assertThat(property.isBoosted()).isFalse();
      assertThat(property.getBoostExpiresAt()).isNull();
    }

    @Test
    @DisplayName("setExpiration – expiresAt positionné")
    void setExpiration_success() throws CustomException {
      LocalDateTime future = LocalDateTime.now().plusMonths(6);
      service.setExpiration(PROPERTY_ID, new PropertyExpirationRequest(future));

      assertThat(property.getExpiresAt()).isEqualTo(future);
    }

    @Test
    @DisplayName("removeExpiration – expiresAt remis à null")
    void removeExpiration_success() throws CustomException {
      property.setExpiresAt(LocalDateTime.now().plusDays(30));
      service.removeExpiration(PROPERTY_ID);

      assertThat(property.getExpiresAt()).isNull();
    }
  }

  // ── Médias

  @Nested
  @DisplayName("Médias")
  class Media {

    private PropertyMedia media;

    @BeforeEach
    void stubProperty() {
      lenient()
          .when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      lenient().when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

      media =
          PropertyMedia.builder()
              .property(property)
              .mediaType("PHOTO")
              .fileUrl("https://minio/img.jpg")
              .build();
      SharedTestFixtures.injectId(media, MEDIA_ID);
    }

    @Test
    @DisplayName("addMedia – sans cover, média ajouté")
    void addMedia_noCover_success() throws CustomException {
      when(mediaRepo.save(any(PropertyMedia.class))).thenReturn(media);

      var req =
          new PropertyMediaAddRequest(
              "PHOTO", "https://minio/img.jpg", "img.jpg", 1024L, "image/jpeg", false, 0);
      var result = service.addMedia(PROPERTY_ID, SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(result).isNotNull();
      verify(mediaRepo, never()).clearCoversForProperty(any());
    }

    @Test
    @DisplayName("addMedia – avec cover, clearCovers appelé")
    void addMedia_withCover_clearsPreviousCovers() throws CustomException {
      when(mediaRepo.save(any(PropertyMedia.class))).thenReturn(media);

      var req =
          new PropertyMediaAddRequest(
              "PHOTO", "https://minio/img.jpg", "img.jpg", 1024L, "image/jpeg", true, 0);
      service.addMedia(PROPERTY_ID, SharedTestFixtures.KEYCLOAK_ID, req);

      verify(mediaRepo).clearCoversForProperty(PROPERTY_ID);
    }

    @Test
    @DisplayName("listMedia – retourne les médias du bien")
    void listMedia_success() throws CustomException {
      when(mediaRepo.findByPropertyIdOrderBySortOrderAsc(PROPERTY_ID)).thenReturn(List.of(media));

      var result = service.listMedia(PROPERTY_ID);

      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("setCover – média existant marqué cover=true")
    void setCover_success() throws CustomException {
      when(mediaRepo.findById(MEDIA_ID)).thenReturn(Optional.of(media));
      when(mediaRepo.save(media)).thenReturn(media);

      service.setCover(PROPERTY_ID, MEDIA_ID, SharedTestFixtures.KEYCLOAK_ID);

      assertThat(media.isCover()).isTrue();
      verify(mediaRepo).clearCoversForProperty(PROPERTY_ID);
    }

    @Test
    @DisplayName("setCover – média d'un autre bien → PROPERTY_MEDIA_NOT_FOUND")
    void setCover_mediaFromOtherProperty_throws() {
      Property otherProp = Property.builder().owner(caller).status(PropertyStatus.DRAFT).build();
      SharedTestFixtures.injectId(otherProp, UUID.randomUUID());
      PropertyMedia otherMedia =
          PropertyMedia.builder().property(otherProp).mediaType("PHOTO").fileUrl("x").build();
      SharedTestFixtures.injectId(otherMedia, MEDIA_ID);

      when(mediaRepo.findById(MEDIA_ID)).thenReturn(Optional.of(otherMedia));

      assertThatThrownBy(
              () -> service.setCover(PROPERTY_ID, MEDIA_ID, SharedTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PROPERTY_MEDIA_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteMedia – média supprimé")
    void deleteMedia_success() throws CustomException {
      when(mediaRepo.findById(MEDIA_ID)).thenReturn(Optional.of(media));

      service.deleteMedia(PROPERTY_ID, MEDIA_ID, SharedTestFixtures.KEYCLOAK_ID);

      verify(mediaRepo).delete(media);
    }

    @Test
    @DisplayName("uploadAndLinkMedia – fichier vide → PROPERTY_MEDIA_NOT_FOUND")
    void uploadAndLinkMedia_emptyFile_throws() {
      MultipartFile emptyFile = mock(MultipartFile.class);
      when(emptyFile.isEmpty()).thenReturn(true);

      assertThatThrownBy(
              () ->
                  service.uploadAndLinkMedia(
                      PROPERTY_ID, SharedTestFixtures.KEYCLOAK_ID, emptyFile, "PHOTO", false))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PROPERTY_MEDIA_NOT_FOUND);
    }

    @Test
    @DisplayName("uploadAndLinkMedia – MIME non autorisé → PROPERTY_MEDIA_NOT_FOUND")
    void uploadAndLinkMedia_badMime_throws() {
      MultipartFile file = mock(MultipartFile.class);
      when(file.isEmpty()).thenReturn(false);
      when(file.getContentType()).thenReturn("application/pdf");

      assertThatThrownBy(
              () ->
                  service.uploadAndLinkMedia(
                      PROPERTY_ID, SharedTestFixtures.KEYCLOAK_ID, file, "PHOTO", false))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PROPERTY_MEDIA_NOT_FOUND);
    }
  }

  // Documents

  @Nested
  @DisplayName("Documents")
  class Documents {

    private PropertyDocument doc;

    @BeforeEach
    void stubProperty() {
      lenient()
          .when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

      doc =
          PropertyDocument.builder()
              .property(property)
              .docType("TITLE_DEED")
              .fileUrl("https://minio/doc.pdf")
              .build();
      SharedTestFixtures.injectId(doc, DOC_ID);
    }

    @Test
    @DisplayName("addDocument – document ajouté au bien")
    void addDocument_success() throws CustomException {
      when(docRepo.save(any(PropertyDocument.class))).thenReturn(doc);

      var req =
          new PropertyDocumentAddRequest(
              "TITLE_DEED",
              "Titre foncier",
              "https://minio/doc.pdf",
              "doc.pdf",
              2048L,
              "application/pdf",
              false);
      var result = service.addDocument(PROPERTY_ID, SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("listDocuments – gestionnaire voit tous les docs (y compris privés)")
    void listDocuments_manager_seesAll() throws CustomException {
      when(docRepo.findByPropertyIdOrderByCreatedAtDesc(PROPERTY_ID)).thenReturn(List.of(doc));

      var result = service.listDocuments(PROPERTY_ID, SharedTestFixtures.KEYCLOAK_ID);

      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("listDocuments – non-gestionnaire ne voit que les docs publics")
    void listDocuments_nonManager_seesOnlyPublic() throws CustomException {
      User other = SharedTestFixtures.buildUserWithoutAgency("kc-other", UUID.randomUUID());
      when(userRepo.findByKeycloakId("kc-other")).thenReturn(Optional.of(other));
      when(docRepo.findByPropertyIdAndVisibleToPublicTrue(PROPERTY_ID)).thenReturn(List.of());

      var result = service.listDocuments(PROPERTY_ID, "kc-other");

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("verifyDocument – document marqué vérifié")
    void verifyDocument_success() throws CustomException {
      when(docRepo.findById(DOC_ID)).thenReturn(Optional.of(doc));
      when(docRepo.save(doc)).thenReturn(doc);

      service.verifyDocument(PROPERTY_ID, DOC_ID, SharedTestFixtures.KEYCLOAK_ID, "Conforme");

      assertThat(doc.isVerified()).isTrue();
      assertThat(doc.getVerifiedBy()).isEqualTo(caller);
    }

    @Test
    @DisplayName("deleteDocument – document supprimé")
    void deleteDocument_success() throws CustomException {
      when(docRepo.findById(DOC_ID)).thenReturn(Optional.of(doc));

      service.deleteDocument(PROPERTY_ID, DOC_ID, SharedTestFixtures.KEYCLOAK_ID);

      verify(docRepo).delete(doc);
    }

    @Test
    @DisplayName("deleteDocument – doc d'un autre bien → PROPERTY_DOCUMENT_NOT_FOUND")
    void deleteDocument_docFromOtherProperty_throws() {
      Property otherProp = Property.builder().owner(caller).status(PropertyStatus.DRAFT).build();
      SharedTestFixtures.injectId(otherProp, UUID.randomUUID());
      PropertyDocument otherDoc =
          PropertyDocument.builder().property(otherProp).docType("OTHER").fileUrl("x").build();
      SharedTestFixtures.injectId(otherDoc, DOC_ID);

      when(docRepo.findById(DOC_ID)).thenReturn(Optional.of(otherDoc));

      assertThatThrownBy(
              () -> service.deleteDocument(PROPERTY_ID, DOC_ID, SharedTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PROPERTY_DOCUMENT_NOT_FOUND);
    }
  }

  // Helpers

  private static PropertyCreateRequest minimalCreateRequest(UUID ownerId) {
    return new PropertyCreateRequest(
        "Villa Test",
        "Description",
        "VILLA",
        "RENT",
        BigDecimal.valueOf(500_000),
        "GOOD",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "12 Rue Test",
        "Dakar",
        null,
        null,
        null,
        null,
        null,
        ownerId,
        null,
        null,
        null,
        null);
  }

  private static PropertyUpdateRequest emptyUpdateRequest() {
    return new PropertyUpdateRequest(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null);
  }

  private static Hotel buildHotel(UUID hotelId) {
    Hotel hotel = Hotel.builder().name("Hôtel Test CI").build();
    SharedTestFixtures.injectId(hotel, hotelId);
    return hotel;
  }
}
