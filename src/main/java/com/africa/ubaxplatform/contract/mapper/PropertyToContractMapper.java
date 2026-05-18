package com.africa.ubaxplatform.contract.mapper;

import com.africa.ubaxplatform.common.constants.Constants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Mapping dynamique entre transactionType d'une propriété et contractType approprié.
 *
 * <p>Utilisé pour : - Auto-générer le type de contrat en fonction du bien à mettre en
 * location/vente - Valider la cohérence métier (ex: un bien en SALE ne peut pas être contracté en
 * LEASE)
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PropertyToContractMapper {

  /**
   * Retourne le contractType préconisé pour un transactionType donné.
   *
   * <p>Mapping :
   *
   * <ul>
   *   <li>{@code SALE} → {@code SALE}
   *   <li>{@code RENT} → {@code LEASE}
   *   <li>{@code RENT_FURNISHED} → {@code LEASE}
   *   <li>{@code SHORT_STAY} → {@code RESERVATION}
   * </ul>
   *
   * @param transactionType la nature de la transaction (depuis Property)
   * @return le contractType préconisé, ou {@code null} si aucune correspondance
   */
  public static String mapToContractType(String transactionType) {
    if (transactionType == null) return null;
    return switch (transactionType) {
      case Constants.CodeList.TransactionType.SALE -> Constants.CodeList.ContractType.SALE;
      case Constants.CodeList.TransactionType.RENT,
              Constants.CodeList.TransactionType.RENT_FURNISHED ->
          Constants.CodeList.ContractType.LEASE;
      case Constants.CodeList.TransactionType.SHORT_STAY ->
          Constants.CodeList.ContractType.RESERVATION;
      default -> null;
    };
  }

  /**
   * Retourne true si le contractType est cohérent avec le transactionType.
   *
   * <p>Permet une validation métier stricte : un bien en SALE ne peut pas être contracté en LEASE
   * sans justification explicite.
   *
   * <p>Règles de cohérence :
   *
   * <ul>
   *   <li>SALE contract → transactionType doit être SALE
   *   <li>LEASE contract → transactionType doit être RENT ou RENT_FURNISHED
   *   <li>RESERVATION contract → transactionType doit être SHORT_STAY
   *   <li>RENT_TO_OWN contract → transactionType doit être RENT ou RENT_FURNISHED
   *   <li>MANDATE contract → compatible avec tous (flexible)
   *   <li>null values → retourne true (défensif : permet si données manquantes)
   * </ul>
   *
   * @param transactionType type de transaction (depuis Property)
   * @param contractType type de contrat proposé (depuis CreateContractRequest)
   * @return true si cohérent, false sinon
   */
  public static boolean isCoherent(String transactionType, String contractType) {
    if (transactionType == null || contractType == null) return true;

    return switch (contractType) {
      case Constants.CodeList.ContractType.SALE ->
          transactionType.equals(Constants.CodeList.TransactionType.SALE);
      case Constants.CodeList.ContractType.LEASE ->
          transactionType.equals(Constants.CodeList.TransactionType.RENT)
              || transactionType.equals(Constants.CodeList.TransactionType.RENT_FURNISHED);
      case Constants.CodeList.ContractType.RESERVATION ->
          transactionType.equals(Constants.CodeList.TransactionType.SHORT_STAY);
      case Constants.CodeList.ContractType.RENT_TO_OWN ->
          transactionType.equals(Constants.CodeList.TransactionType.RENT)
              || transactionType.equals(Constants.CodeList.TransactionType.RENT_FURNISHED);
      case Constants.CodeList.ContractType.MANDATE -> true; // Mandat compatible avec tout
      default -> false;
    };
  }
}
