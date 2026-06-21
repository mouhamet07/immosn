package sn.immosn.backend.client.web.dashboard.dto;

import java.util.List;

public record DashboardStatsDto(
    // Compteurs globaux
    long totalAnnonces,
    long annoncesActives,
    long totalClients,
    long totalAdmins,
    long totalContrats,
    long contratsActifs,
    long totalVisites,
    long visitesEnAttente,
    long visitesAujourdhui,
    long totalSignalements,
    long signalementsOuverts,
    long totalLeads,
    long leadsEnCours,
    long leadsConvertis,
    long leadsAbandonnes,
    double tauxConversionLeads,
    long totalDiscussions,

    // Sprint 4 — répartition par type de transaction
    long annoncesVente,
    long annoncesLocation,
    long contratsVente,
    long contratsLocation,
    long visitesVente,
    long visitesLocation,

    // Module propriétaires
    long totalProprietaires,
    long proprietairesActifs,

    // Activités récentes
    List<RecentActivityDto> activitesRecentes
) {}
