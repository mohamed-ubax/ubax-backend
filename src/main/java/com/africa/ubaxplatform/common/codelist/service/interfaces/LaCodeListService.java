package com.africa.ubaxplatform.common.codelist.service.interfaces;

import com.africa.ubaxplatform.common.codelist.entity.LaCodeList;
import com.africa.ubaxplatform.common.exception.CustomException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LaCodeListService {

  /**
   * Retourne toutes les valeurs pour un type donné.
   *
   * <p>Utilisé par le frontend pour peupler les selects (dropdown). L'appel est public et ne
   * nécessite pas d'authentification.
   *
   * <p>Exemple de types disponibles : {@code PROPERTY_TYPE}, {@code EMPLOYMENT_STATUS}, {@code
   * TICKET_CATEGORY}…
   *
   * @param type identifiant du type de code list (ex : {@code "PROPERTY_TYPE"})
   * @return la liste des entrées correspondant au type, triée dans l'ordre d'insertion
   */
  List<LaCodeList> findAllByType(String type);

  /**
   * Retourne une page de code lists tous types confondus.
   *
   * <p>Réservé à l'interface d'administration.
   *
   * @param pageable paramètres de pagination et de tri
   * @return une page de code lists
   */
  Page<LaCodeList> findAll(Pageable pageable);

  /**
   * Retourne un code list par son identifiant.
   *
   * @param id UUID du code list
   * @return le code list trouvé
   * @throws CustomException 404 si aucun code list ne correspond à l'id
   */
  LaCodeList findById(UUID id) throws CustomException;

  /**
   * Crée un nouveau code list.
   *
   * <p>Le couple {@code type + value} doit être unique : une violation de contrainte lève une
   * {@link CustomException} 409.
   *
   * @param codeList entité à persister (sans id)
   * @return le code list créé avec son id assigné
   * @throws CustomException 409 si le couple type/value existe déjà, 500 en cas d'erreur serveur
   */
  LaCodeList createCodeList(LaCodeList codeList) throws CustomException;

  /**
   * Met à jour un code list existant.
   *
   * <p>L'id de l'entité passée en paramètre doit correspondre à {@code id}, sinon une {@link
   * CustomException} 400 est levée.
   *
   * @param codeList entité avec les nouvelles valeurs
   * @param id UUID du code list à mettre à jour
   * @return le code list mis à jour
   * @throws CustomException 400 si les ids sont incohérents, 404 si introuvable, 409 si doublon
   */
  LaCodeList updateCodeList(LaCodeList codeList, UUID id) throws CustomException;
}
