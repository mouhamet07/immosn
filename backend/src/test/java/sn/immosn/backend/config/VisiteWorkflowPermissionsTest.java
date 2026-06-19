package sn.immosn.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Sprint 2 — garde-fous de sécurité du processus commercial de visite.
 *
 * Vérifie la garantie critique : un ADMIN (non SUPER_ADMIN) ne peut PAS
 *  - accepter / refuser une demande de visite,
 *  - affecter un responsable,
 *  - piloter une replanification.
 * Ces actions sont réservées au SUPER_ADMIN → réponse 403.
 *
 * Le chemin autorisé (SUPER_ADMIN affecte, ADMIN rédige un rapport) est couvert au niveau
 * service par DemandeVisiteWorkflowServiceTest et RapportVisiteServiceTest.
 */
@SpringBootTest(properties = "app.super-admin.password=Test-SuperAdmin@2024")
@AutoConfigureMockMvc
@DisplayName("Sprint 2 — permissions du workflow de visite")
class VisiteWorkflowPermissionsTest {

    @Autowired MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN ne peut PAS accepter une visite (status=ACCEPTEE) → 403")
    void admin_cannotAccept() throws Exception {
        mockMvc.perform(put("/api/v1/visites/1/status").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statut\":\"ACCEPTEE\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN ne peut PAS refuser une visite (status=REFUSEE) → 403")
    void admin_cannotRefuse() throws Exception {
        mockMvc.perform(put("/api/v1/visites/1/status").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statut\":\"REFUSEE\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN ne peut PAS affecter une visite → 403")
    void admin_cannotAffect() throws Exception {
        mockMvc.perform(put("/api/v1/visites/1/affecter").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"adminId\":9}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN ne peut PAS demander une replanification → 403")
    void admin_cannotReplanifier() throws Exception {
        mockMvc.perform(put("/api/v1/visites/1/replanification").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nouvelleDate\":\"2999-01-01T10:00:00\"}"))
            .andExpect(status().isForbidden());
    }
}
