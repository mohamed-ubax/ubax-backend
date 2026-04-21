package com.africa.ubaxplatform.payment.codeList;

/**
 * Centre de coût d'une dépense enregistrée par une agence UBAX.
 *
 * <p>Détermine à quel niveau la dépense doit être imputée : à l'agence dans son ensemble (charges
 * de structure) ou à un bien immobilier précis (dépenses directement liées à la gestion d'un
 * actif). Ce découpage permet :
 *
 * <ul>
 *   <li>au <b>Directeur d'agence</b> de visualiser la rentabilité nette par bien ;
 *   <li>au <b>Comptable</b> d'éditer des rapports de charges ventilés par propriété ;
 *   <li>à l'agence de refacturer au propriétaire les dépenses imputées à son bien.
 * </ul>
 *
 * <p><b>Règle de validation :</b> si {@code costCenter = PROPERTY_SPECIFIC}, le champ {@code
 * propertyId} est <b>obligatoire</b> lors de la création d'une dépense. Le service lève une {@code
 * BadRequestException} si {@code propertyId} est absent dans ce cas.
 *
 * @see ExpenseCategory
 * @see com.africa.ubaxplatform.payment.entity.Expense
 */
public enum CostCenter {

  /**
   * Charges générales de fonctionnement de l'agence.
   *
   * <p>Dépenses non rattachables à un bien immobilier particulier : loyer des bureaux, salaires,
   * abonnements logiciels, marketing institutionnel, frais bancaires, taxes professionnelles, etc.
   * Ces coûts sont absorbés par l'agence et n'apparaissent pas dans le compte d'exploitation d'un
   * bien précis.
   */
  AGENCY_GENERAL,

  /**
   * Dépense liée à un bien immobilier spécifique.
   *
   * <p>Travaux de maintenance, réparations, assurance du bien, frais de remise en état entre deux
   * locataires, honoraires d'un diagnostiqueur immobilier, etc. Le bien concerné est identifié par
   * {@code propertyId} (obligatoire lorsque cette valeur est sélectionnée). Cette dépense est
   * déduite des revenus du bien pour calculer sa rentabilité nette.
   */
  PROPERTY_SPECIFIC
}
