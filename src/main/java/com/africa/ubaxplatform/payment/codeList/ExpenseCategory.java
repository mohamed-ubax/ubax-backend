package com.africa.ubaxplatform.payment.codeList;

/**
 * Catégorie comptable d'une dépense enregistrée par une agence UBAX.
 *
 * <p>Les catégories permettent de ventiler les sorties financières de l'agence par nature et
 * d'alimenter les graphiques de répartition des dépenses sur le tableau de bord comptable
 * ({@code GET /v1/expenses} avec agrégats par catégorie).
 *
 * <p>Chaque dépense est liée à un centre de coût ({@link CostCenter}) qui précise si la dépense
 * concerne l'agence dans son ensemble ou un bien immobilier spécifique.
 *
 * @see Expense
 * @see CostCenter
 */
public enum ExpenseCategory {

  /**
   * Travaux et frais de maintenance immobilière.
   *
   * <p>Couvre toutes les interventions techniques sur les biens gérés : réparations, entretien
   * courant, plomberie, électricité, peinture, nettoyage, jardinage, etc. Directement liée à un
   * bien si {@code costCenter = PROPERTY_SPECIFIC}. C'est la catégorie la plus fréquente pour
   * les agences gérant un parc locatif.
   */
  MAINTENANCE,

  /**
   * Dépenses de marketing et communication.
   *
   * <p>Campagnes publicitaires (réseaux sociaux, portails immobiliers, presse locale), création
   * de supports visuels, photographe professionnel pour les annonces, boost d'annonces sur UBAX,
   * développement du site web de l'agence, etc.
   */
  MARKETING,

  /**
   * Charges salariales et honoraires.
   *
   * <p>Salaires bruts des employés de l'agence (agents, comptable, SAV), cotisations sociales,
   * honoraires des prestataires indépendants et freelances, commissions versées aux commerciaux
   * externes. Toujours imputé au centre de coût {@code AGENCY_GENERAL}.
   */
  SALARY,

  /**
   * Charges d'exploitation et de fonctionnement.
   *
   * <p>Eau, électricité, internet, téléphonie, loyer des locaux de l'agence, assurances
   * professionnelles, fournitures de bureau, logiciels SaaS (CRM, comptabilité), frais bancaires.
   * Comprend également les charges des parties communes des immeubles gérés lorsqu'elles sont
   * refacturées en bloc à l'agence.
   */
  UTILITIES,

  /**
   * Taxes, impôts et obligations fiscales.
   *
   * <p>Taxes foncières, patentes, TVA décaissée, contributions professionnelles, droits
   * d'enregistrement, frais notariés, amendes administratives et toutes obligations fiscales
   * légales liées à l'activité de l'agence ou aux biens gérés.
   */
  TAX,

  /**
   * Toute autre dépense non couverte par les catégories ci-dessus.
   *
   * <p>Utilisé exceptionnellement pour les dépenses atypiques ou ponctuelles difficiles à
   * rattacher à une catégorie existante. Il est recommandé de préciser la nature dans le champ
   * {@code label} de la dépense pour faciliter l'analyse comptable ultérieure.
   */
  OTHER
}
