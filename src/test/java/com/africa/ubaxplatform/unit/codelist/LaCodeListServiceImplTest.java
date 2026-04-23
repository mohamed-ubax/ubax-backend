package com.africa.ubaxplatform.unit.codelist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.common.codelist.entity.LaCodeList;
import com.africa.ubaxplatform.common.codelist.repository.LaCodeListRepository;
import com.africa.ubaxplatform.common.codelist.service.impl.LaCodeListServiceImpl;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.testHelper.TenantTestFixtures;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("LaCodeListServiceImpl – tests unitaires")
class LaCodeListServiceImplTest {

  @Mock private LaCodeListRepository codeListRepository;

  @InjectMocks private LaCodeListServiceImpl service;

  private LaCodeList buildCodeList(UUID id) {
    LaCodeList cl =
        LaCodeList.builder()
            .type("TICKET_CATEGORY")
            .value("LEAK")
            .description("Fuite d'eau")
            .isSystemAssign(true)
            .build();
    if (id != null) TenantTestFixtures.injectId(cl, id);
    return cl;
  }

  @Nested
  @DisplayName("findAllByType()")
  class FindAllByType {

    @Test
    @DisplayName("Succès – retourne la liste filtrée par type")
    void findAllByType_success_returnsFilteredList() {
      LaCodeList cl = buildCodeList(UUID.randomUUID());
      when(codeListRepository.findAllByType("TICKET_CATEGORY")).thenReturn(List.of(cl));

      List<LaCodeList> result = service.findAllByType("TICKET_CATEGORY");

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getValue()).isEqualTo("LEAK");
    }

    @Test
    @DisplayName("Succès – retourne liste vide si aucun résultat")
    void findAllByType_noMatch_returnsEmptyList() {
      when(codeListRepository.findAllByType("UNKNOWN_TYPE")).thenReturn(List.of());

      List<LaCodeList> result = service.findAllByType("UNKNOWN_TYPE");

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("findAll()")
  class FindAll {

    @Test
    @DisplayName("Succès – retourne page paginée")
    void findAll_success_returnsPage() {
      LaCodeList cl = buildCodeList(UUID.randomUUID());
      Page<LaCodeList> page = new PageImpl<>(List.of(cl));
      when(codeListRepository.findAll(any(PageRequest.class))).thenReturn(page);

      Page<LaCodeList> result = service.findAll(PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("findById()")
  class FindById {

    @Test
    @DisplayName("Succès – retourne le code list existant")
    void findById_success_returnsCodeList() throws CustomException {
      UUID id = UUID.randomUUID();
      LaCodeList cl = buildCodeList(id);
      when(codeListRepository.findById(id)).thenReturn(Optional.of(cl));

      LaCodeList result = service.findById(id);

      assertThat(result.getType()).isEqualTo("TICKET_CATEGORY");
      assertThat(result.getValue()).isEqualTo("LEAK");
    }

    @Test
    @DisplayName("Échec – id null → CODELIST_GET_FAILURE_BAD_REQUEST")
    void findById_nullId_throwsBadRequest() {
      assertThatThrownBy(() -> service.findById(null))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.CODELIST_GET_FAILURE_BAD_REQUEST);
      verify(codeListRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Échec – id introuvable → CODELIST_GET_FAILURE_NOT_FOUND")
    void findById_notFound_throwsNotFoundException() {
      UUID id = UUID.randomUUID();
      when(codeListRepository.findById(id)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.findById(id))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.CODELIST_GET_FAILURE_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("createCodeList()")
  class CreateCodeList {

    @Test
    @DisplayName("Succès – code list créé et retourné")
    void createCodeList_success_returnsSaved() throws CustomException {
      LaCodeList cl = buildCodeList(null);
      UUID generatedId = UUID.randomUUID();
      when(codeListRepository.save(cl))
          .thenAnswer(
              inv -> {
                LaCodeList saved = inv.getArgument(0);
                TenantTestFixtures.injectId(saved, generatedId);
                return saved;
              });

      LaCodeList result = service.createCodeList(cl);

      assertThat(result.getId()).isEqualTo(generatedId);
      verify(codeListRepository).save(cl);
    }

    @Test
    @DisplayName("Échec – doublon type/value → CODELIST_POST_DUPLICATE")
    void createCodeList_duplicate_throwsConflict() {
      LaCodeList cl = buildCodeList(null);
      when(codeListRepository.save(cl)).thenThrow(new DataIntegrityViolationException("duplicate"));

      assertThatThrownBy(() -> service.createCodeList(cl))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.CODELIST_POST_DUPLICATE);
    }
  }

  @Nested
  @DisplayName("updateCodeList()")
  class UpdateCodeList {

    @Test
    @DisplayName("Succès – code list mis à jour")
    void updateCodeList_success_returnsUpdated() throws CustomException {
      UUID id = UUID.randomUUID();
      LaCodeList cl = buildCodeList(id);
      when(codeListRepository.findById(id)).thenReturn(Optional.of(cl));
      when(codeListRepository.save(cl)).thenReturn(cl);

      LaCodeList result = service.updateCodeList(cl, id);

      assertThat(result.getValue()).isEqualTo("LEAK");
      verify(codeListRepository).save(cl);
    }

    @Test
    @DisplayName("Échec – id du body différent du path → CODELIST_PUT_FAILURE_BAD_REQUEST")
    void updateCodeList_idMismatch_throwsBadRequest() {
      UUID id = UUID.randomUUID();
      LaCodeList cl = buildCodeList(UUID.randomUUID()); // different id

      assertThatThrownBy(() -> service.updateCodeList(cl, id))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.CODELIST_PUT_FAILURE_BAD_REQUEST);
      verify(codeListRepository, never()).save(any());
    }

    @Test
    @DisplayName("Échec – body null → CODELIST_PUT_FAILURE_BAD_REQUEST")
    void updateCodeList_nullBody_throwsBadRequest() {
      UUID id = UUID.randomUUID();

      assertThatThrownBy(() -> service.updateCodeList(null, id))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.CODELIST_PUT_FAILURE_BAD_REQUEST);
    }

    @Test
    @DisplayName("Échec – id introuvable → CODELIST_GET_FAILURE_NOT_FOUND")
    void updateCodeList_notFound_throwsNotFoundException() {
      UUID id = UUID.randomUUID();
      LaCodeList cl = buildCodeList(id);
      when(codeListRepository.findById(id)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.updateCodeList(cl, id))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.CODELIST_GET_FAILURE_NOT_FOUND);
    }
  }
}
