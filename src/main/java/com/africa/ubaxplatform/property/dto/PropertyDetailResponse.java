package com.africa.ubaxplatform.property.dto;

import java.util.List;

/**
 * Vue complète d'un bien : données du bien + médias + documents. Retourné par {@code GET
 * /v1/properties/{id}}.
 */
public record PropertyDetailResponse(
    PropertyResponse property,
    List<PropertyMediaResponse> media,
    List<PropertyDocumentResponse> documents) {}
