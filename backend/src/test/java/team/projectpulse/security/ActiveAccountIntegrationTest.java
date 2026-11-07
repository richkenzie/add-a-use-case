package team.projectpulse.security;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.system.StatusCode;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-SEC-active-account end to end: a token does not outlive its account's deactivation, and a deactivated
 * account cannot log in.
 *
 * <p>The account is deactivated here by setting {@code enabled} in the repository, so these tests hold for
 * students and instructors alike and do not depend on any deactivate endpoint. The test transaction makes the
 * change visible to the next request, which runs on the same thread.
 */
@DisplayName("Integration tests for FR-SEC-active-account")
class ActiveAccountIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Value("${api.endpoint.base-url}")
    String baseUrl;


    private JSONObject login(String email) throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic(email, "123456"))).andReturn();
        return new JSONObject(mvcResult.getResponse().getContentAsString()).getJSONObject("data");
    }

    private void deactivate(int userId) {
        PeerEvaluationUser user = this.userRepository.findById(userId).orElseThrow();
        user.setEnabled(false);
        this.userRepository.saveAndFlush(user);
    }

    @Test
    @DisplayName("A student's token issued before deactivation is refused on its next request")
    void studentTokenIssuedBeforeDeactivationIsRefused() throws Exception {
        // Given: John is logged in and can read his own profile
        JSONObject loginData = login("j.smith@abc.edu");
        String token = "Bearer " + loginData.getString("token");
        int johnId = loginData.getJSONObject("userInfo").getInt("id");
        this.mockMvc.perform(get(this.baseUrl + "/students/" + johnId).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        // When: he is deactivated while the token is still within its lifetime
        deactivate(johnId);

        // Then
        this.mockMvc.perform(get(this.baseUrl + "/students/" + johnId).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value("User account is abnormal."));
    }

    @Test
    @DisplayName("A deactivated student cannot log in")
    void deactivatedStudentCannotLogIn() throws Exception {
        // Given
        int johnId = login("j.smith@abc.edu").getJSONObject("userInfo").getInt("id");
        deactivate(johnId);

        // When, then
        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value("User account is abnormal."));
    }

    @Test
    @DisplayName("An instructor's token issued before deactivation is refused on its next request")
    void instructorTokenIssuedBeforeDeactivationIsRefused() throws Exception {
        // Given: Bill Gates is logged in and can read his own profile
        JSONObject loginData = login("b.gates@abc.edu");
        String token = "Bearer " + loginData.getString("token");
        int instructorId = loginData.getJSONObject("userInfo").getInt("id");
        this.mockMvc.perform(get(this.baseUrl + "/instructors/" + instructorId).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        // When: there is no instructor deactivate endpoint yet, so the flag is set directly
        deactivate(instructorId);

        // Then
        this.mockMvc.perform(get(this.baseUrl + "/instructors/" + instructorId).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value("User account is abnormal."));
    }

}
