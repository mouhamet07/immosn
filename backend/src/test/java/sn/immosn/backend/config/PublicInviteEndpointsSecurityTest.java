package sn.immosn.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Vérifie les règles RBAC du parcours visiteur (Sprint 1) avec la configuration réelle :
 *  - les endpoints invité (/discussions/invite, /discussions/token/**, /visites/invite) sont PUBLICS
 *    (la requête atteint le contrôleur : pas de 401/403) ;
 *  - les endpoints existants protégés restent refusés sans authentification.
 *
 * Test d'intégration léger : contexte complet (H2 en mémoire), aucune authentification fournie.
 */
@SpringBootTest(properties = {
    // Le bootstrap SUPER_ADMIN exige ce secret (sans défaut en application.properties).
    // Fourni ici pour rendre le test autonome, sans dépendre d'une variable d'environnement.
    "app.super-admin.password=Test-SuperAdmin@2024"
})
@AutoConfigureMockMvc
@DisplayName("Sécurité — endpoints invité publics vs endpoints protégés")
class PublicInviteEndpointsSecurityTest {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("POST /discussions/invite — PUBLIC : la requête atteint le contrôleur (pas 401/403)")
    void discussionInvite_isPublic() throws Exception {
        // annonceId inexistante → 404 côté service, mais surtout PAS 401/403 (donc bien public)
        String body = """
            {"annonceId":999999,"nom":"Sow","telephone":"+221770000002",
             "email":"ibrahima@email.com","premierMessage":"Bonjour"}""";

        mockMvc.perform(post("/api/v1/discussions/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().is(404));
    }

    @Test
    @DisplayName("GET /discussions/token/{token} — PUBLIC : la requête atteint le contrôleur (pas 401/403)")
    void discussionToken_isPublic() throws Exception {
        // token inconnu → 404, jamais 401/403
        mockMvc.perform(get("/api/v1/discussions/token/token-inexistant"))
            .andExpect(status().is(404));
    }

    @Test
    @DisplayName("POST /visites/invite — PUBLIC : la requête atteint le contrôleur (pas 401/403)")
    void visiteInvite_isPublic() throws Exception {
        // Données incomplètes → 400 de validation, mais PAS 401/403 (donc bien public)
        String body = """
            {"annonceId":999999}""";

        mockMvc.perform(post("/api/v1/visites/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().is(400));
    }

    @Test
    @DisplayName("GET /discussions/admin — PROTÉGÉ : refusé sans authentification (401/403)")
    void adminEndpoint_isDenied() throws Exception {
        mockMvc.perform(get("/api/v1/discussions/admin"))
            .andExpect(status().is4xxClientError())
            .andExpect(result -> {
                int s = result.getResponse().getStatus();
                if (s != 401 && s != 403) {
                    throw new AssertionError("Attendu 401 ou 403, obtenu " + s);
                }
            });
    }

    @Test
    @DisplayName("GET /visites/admin — PROTÉGÉ : refusé sans authentification (401/403)")
    void visiteAdminEndpoint_isDenied() throws Exception {
        mockMvc.perform(get("/api/v1/visites/admin"))
            .andExpect(status().is4xxClientError())
            .andExpect(result -> {
                int s = result.getResponse().getStatus();
                if (s != 401 && s != 403) {
                    throw new AssertionError("Attendu 401 ou 403, obtenu " + s);
                }
            });
    }

    @Test
    @DisplayName("POST /discussions — PROTÉGÉ (client connecté) : refusé sans authentification (401/403)")
    void protectedCreate_isDenied() throws Exception {
        String body = """
            {"annonceId":2,"premierMessage":"Bonjour"}""";

        mockMvc.perform(post("/api/v1/discussions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().is4xxClientError())
            .andExpect(result -> {
                int s = result.getResponse().getStatus();
                if (s != 401 && s != 403) {
                    throw new AssertionError("Attendu 401 ou 403, obtenu " + s);
                }
            });
    }
}
