package com.africa.ubaxplatform.contract.dto;

/** Compteurs de contrats par groupe de statut — utilisés par les KPIs de la page liste UI. */
public record ContractStatsResponse(long total, long active, long pending, long terminated) {}
