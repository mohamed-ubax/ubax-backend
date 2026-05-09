package com.africa.ubaxplatform.dashboard.dto;

public record AdminDashboardResponse(
    long totalActiveAgencies,
    long totalActiveHotels,
    long totalClients,
    long totalOwners,
    long pendingReservations,
    long confirmedReservations,
    long propertiesPendingReview,
    long publishedProperties,
    long openTickets) {}
