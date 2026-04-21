package com.africa.ubaxplatform.property.codeList;

/**
 * Cycle de vie d'une annonce immobilière sur la plateforme UBAX.
 *
 * <p>Toute annonce suit ce workflow depuis sa création par le partenaire jusqu'à son archivage :
 *
 * <pre>
 *  DRAFT
 *    │  (soumission par le partenaire)
 *    ▼
 *  PENDING ──────────────────────────────► REJECTED  (motif obligatoire)
 *    │  (validation modérateur)
 *    ▼
 *  PUBLISHED ────────────────────────────► ARCHIVED  (expiration ou retrait)
 *    │  (réservation / offre acceptée)
 *    ▼
 *  RESERVED
 *    │
 *    ├──► SOLD      (transaction de vente finalisée)
 *    └──► PUBLISHED (annulation de la réservation)
 * </pre>
 *
 * <p><b>Visibilité publique :</b> seuls les statuts {@code PUBLISHED} et {@code RESERVED} sont
 * accessibles sans authentification sur le portail grand public.
 *
 * <p><b>Boost :</b> une annonce peut être boostée indépendamment de son statut via le flag {@code
 * boosted} sur l'entité {@link com.africa.ubaxplatform.property.entity.Property}.
 */
public enum PropertyStatus {

  /**
   * Brouillon – annonce en cours de rédaction par le partenaire.
   *
   * <p>L'annonce n'est visible que par son auteur dans son espace partenaire. Les informations et
   * médias peuvent être complétés librement. Aucune publication ni modération n'est déclenchée tant
   * que le partenaire n'a pas soumis l'annonce.
   */
  DRAFT,

  /**
   * En attente de modération par l'équipe UBAX.
   *
   * <p>Le partenaire a soumis l'annonce. Elle est invisible pour le grand public et en file
   * d'attente chez les modérateurs ({@link
   * com.africa.ubaxplatform.auth.codeList.UbaxAdminRole#CONTENT_MODERATOR}). Le modérateur peut
   * approuver (→ {@code PUBLISHED}) ou rejeter (→ {@code REJECTED}).
   */
  PENDING,

  /**
   * Annonce publiée et visible publiquement.
   *
   * <p>L'annonce est accessible sur le portail grand public, dans les résultats de recherche et sur
   * les cartes. La date de publication est enregistrée dans {@code publishedAt}. L'annonce expire
   * automatiquement à la date {@code expiresAt} via un job planifié (→ {@code ARCHIVED}).
   */
  PUBLISHED,

  /**
   * Bien réservé suite à une offre acceptée ou une demande de location en cours.
   *
   * <p>L'annonce reste visible sur le portail mais est marquée « Réservé ». Aucune nouvelle demande
   * ne peut être soumise. Si la réservation tombe (offre annulée, contrat non signé), le statut
   * repasse à {@code PUBLISHED}.
   */
  RESERVED,

  /**
   * Bien vendu – transaction de vente finalisée.
   *
   * <p>État terminal pour les biens en {@code transaction_type = SALE}. Le contrat de vente est
   * signé et la propriété a changé de mains. L'annonce est retirée du portail public et conservée
   * dans les archives pour la traçabilité comptable.
   */
  SOLD,

  /**
   * Annonce archivée – retirée du portail public.
   *
   * <p>L'annonce a expiré (date {@code expiresAt} dépassée), a été manuellement retirée par le
   * partenaire, ou a été archivée par un modérateur. Les données sont conservées en base pour
   * l'historique et les rapports. Le partenaire peut remettre l'annonce en {@code DRAFT} pour la
   * retravailler et la republier.
   */
  ARCHIVED,

  /**
   * Annonce rejetée par un modérateur.
   *
   * <p>L'annonce ne respecte pas la charte de publication UBAX (informations incomplètes, photos de
   * mauvaise qualité, contenu trompeur, doublon détecté, etc.). Le motif de refus est
   * obligatoirement renseigné dans {@code rejectionReason} et notifié au partenaire. Le partenaire
   * peut corriger et resoumettre (→ retour à {@code DRAFT} puis {@code PENDING}).
   */
  REJECTED
}
