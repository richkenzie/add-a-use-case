package team.projectpulse.student;

import tools.jackson.databind.json.JsonMapper;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.activity.ActivityRepository;
import team.projectpulse.evaluation.PeerEvaluation;
import team.projectpulse.evaluation.PeerEvaluationRepository;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.section.SectionRepository;
import team.projectpulse.system.StatusCode;
import team.projectpulse.user.UserRepository;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Integration tests for Student API endpoints")
class StudentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    ActivityRepository activityRepository;

    @Autowired
    PeerEvaluationRepository peerEvaluationRepository;

    String adminToken;

    String instructorBillToken;

    String studentJohnToken;

    String studentEricToken;

    String adminTimToken;

    @Value("${api.endpoint.base-url}")
    String baseUrl;


    @BeforeEach
    void setUp() throws Exception {
        ResultActions resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("b.wei@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        MvcResult mvcResult = resultActions.andDo(print()).andReturn();
        String contentAsString = mvcResult.getResponse().getContentAsString();
        JSONObject json = new JSONObject(contentAsString);
        this.adminToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("b.gates@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.instructorBillToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.studentJohnToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("e.hudson@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.studentEricToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("t.cook@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.adminTimToken = "Bearer " + json.getJSONObject("data").getString("token");
    }

    @Test
    void testFindStudentsByCriteria() throws Exception {
        // Given
        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("teamName", "Team1");
        String json = this.jsonMapper.writeValueAsString(searchCriteria);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("page", "0");
        requestParams.add("size", "30");
        requestParams.add("sort", "lastName,asc");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students/search").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find students successfully"))
                .andExpect(jsonPath("$.data.content", Matchers.hasSize(3)));
    }

    @Test
    void testInstructorBillFindStudentInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"));
    }

    @Test
    void testAdminTimFindStudentInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminTimToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    /**
     * Makes Tim Cook, course admin of the EE capstone course, no longer an instructor of its only course section,
     * and returns the id of Jana Norton, a student of that course section. The seed data assigns every course
     * admin to all of her course's course sections, so this is the only way to reach the case BR-student-lifecycle
     * names: the course admin of the student's course who is not assigned to her course section.
     */
    private int unassignAdminTimFromJanasSection() {
        Student jana = (Student) this.userRepository.findByEmail("j.norton@abc.edu").orElseThrow();
        Instructor tim = (Instructor) this.userRepository.findByEmail("t.cook@abc.edu").orElseThrow();
        Section section = jana.getSection();
        section.removeInstructor(tim);
        this.sectionRepository.saveAndFlush(section);
        return jana.getId();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student step 2: the course admin of the student's course opens her even when not assigned to her course section")
    void testCourseAdminNotAssignedToSectionFindsStudentInfoById() throws Exception {
        // Given
        int janaId = unassignAdminTimFromJanasSection();

        // When and then
        this.mockMvc.perform(get(this.baseUrl + "/students/" + janaId).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminTimToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.data.id").value(janaId));
    }

    @Test
    void testStudentJohnFindOwnInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"));
    }

    @Test
    void testStudentJohnFindAnotherStudentsInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/5").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    @Test
    void testAddStudent() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "lucas");
        studentDto.put("firstName", "Lucas");
        studentDto.put("lastName", "Santos");
        studentDto.put("email", "l.santos@abc.edu");
        studentDto.put("password", "Abc123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "registrationToken");
        requestParams.add("role", "student");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Add student successfully"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.username").value("lucas"))
                .andExpect(jsonPath("$.data.firstName").value("Lucas"))
                .andExpect(jsonPath("$.data.lastName").value("Santos"))
                .andExpect(jsonPath("$.data.email").value("l.santos@abc.edu"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void testAddStudentWithInvalidUserInvitation() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "marcos");
        studentDto.put("firstName", "Marcos");
        studentDto.put("lastName", "Grant");
        studentDto.put("email", "m.grant@abc.edu");
        studentDto.put("password", "123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "invalidRegistrationToken");
        requestParams.add("role", "student");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("m.grant@abc.edu is not invited to register. Please contact the course admin."));
    }

    @Test
    void testAddStudentWithNoWrongUserInvitation() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "lucas");
        studentDto.put("firstName", "Lucas");
        studentDto.put("lastName", "Santos");
        studentDto.put("email", "l.santos@abc.edu");
        studentDto.put("password", "123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "registrationToken");
        requestParams.add("role", "instructor"); // This is wrong, Lucas should be a student.

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("You are not allowed to register as instructor for email: l.santos@abc.edu"));
    }

    @Test
    void testStudentJohnUpdatesOwnInfo() throws Exception {
        Map<String, Object> studentDto = new HashMap<>();
        studentDto.put("username", "john");
        studentDto.put("firstName", "John (updated)");
        studentDto.put("lastName", "Smith");
        studentDto.put("email", "j.smith@abc.edu");
        studentDto.put("enabled", false); // Student cannot change their enabled status.
        studentDto.put("roles", "instructor"); // Student cannot change their role.

        String json = this.jsonMapper.writeValueAsString(studentDto);

        // When and then
        this.mockMvc.perform(put(this.baseUrl + "/students/4").contentType(MediaType.APPLICATION_JSON).content(json).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Update student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("john"))
                .andExpect(jsonPath("$.data.firstName").value("John (updated)"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.enabled").value(true)) // The enabled status should not be changed.
                .andExpect(jsonPath("$.data.roles").value("student")); // The role should not be changed.
    }

    @Test
    void testStudentJohnUpdatesAnotherStudentsInfo() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "eric");
        studentDto.put("firstName", "Eric (updated)");
        studentDto.put("lastName", "Hudson");
        studentDto.put("email", "e.hudson@abc.edu");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        // When and then
        this.mockMvc.perform(put(this.baseUrl + "/students/5").contentType(MediaType.APPLICATION_JSON).content(json).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

//    @Test
//    void testAdminDeletesStudent() throws Exception {
//        this.mockMvc.perform(delete(this.baseUrl + "/students/3").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
//                .andExpect(jsonPath("$.flag").value(true))
//                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
//                .andExpect(jsonPath("$.message").value("Delete instructor successfully"));
//    }
//
//    @Test
//    void testStudentDeletesAnotherStudent() throws Exception {
//        this.mockMvc.perform(delete(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentToken))
//                .andExpect(jsonPath("$.flag").value(false))
//                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
//                .andExpect(jsonPath("$.message").value("No permission."))
//                .andExpect(jsonPath("$.data").value("Access Denied"));
//    }

    @Test
    void testStudentJohnFindStudentsInOwnTeam() throws Exception {
        // When and then
        this.mockMvc.perform(get(this.baseUrl + "/students/teams/1").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find students by team id successfully"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(3)));
    }

    @Test
    void testStudentJohnFindStudentsInAnotherTeam() throws Exception {
        // When and then
        this.mockMvc.perform(get(this.baseUrl + "/students/teams/2").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    // UC-STU-deactivate-student. John (id 4) is in course section 2023-2024, taught by Bingyang Wei (also the
    // course admin) and Bill Gates, and is on Team1 with two teammates.

    private static final int JOHN_ID = 4;

    private ResultActions deactivate(int studentId, String token) throws Exception {
        return this.mockMvc.perform(patch(this.baseUrl + "/students/" + studentId + "/deactivate").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token));
    }

    @Test
    @DisplayName("UC-STU-deactivate-student main: the instructor of the student's course section deactivates her")
    void testInstructorBillDeactivatesStudent() throws Exception {
        // When
        deactivate(JOHN_ID, this.instructorBillToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Deactivate student successfully"))
                .andExpect(jsonPath("$.data.id").value(JOHN_ID))
                .andExpect(jsonPath("$.data.enabled").value(false));

        // Then: the student, read back, is deactivated
        this.mockMvc.perform(get(this.baseUrl + "/students/" + JOHN_ID).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @DisplayName("UC-STU-deactivate-student main: the course admin deactivates a student of a course section she is not assigned to")
    void testCourseAdminNotAssignedToSectionDeactivatesStudent() throws Exception {
        // Given
        int janaId = unassignAdminTimFromJanasSection();

        // When and then
        deactivate(janaId, this.adminTimToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @DisplayName("UC-STU-deactivate-student main: the student stays on her team")
    void testDeactivatedStudentStaysOnHerTeam() throws Exception {
        // When
        deactivate(JOHN_ID, this.instructorBillToken).andExpect(status().isOk());

        // Then: Team1 still lists her, still reported as deactivated
        this.mockMvc.perform(get(this.baseUrl + "/students/teams/1").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.data", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.data[?(@.id == " + JOHN_ID + ")].enabled").value(Matchers.contains(false)));
    }

    @Test
    @DisplayName("UC-STU-deactivate-student main: her weekly activity reports and peer evaluations are retained")
    void testDeactivatedStudentsWorkIsRetained() throws Exception {
        // Given: what John has on record before deactivation
        long activities = countActivitiesOf(JOHN_ID);
        long evaluationsWritten = countEvaluations(evaluation -> evaluation.getEvaluator().getId() == JOHN_ID);
        long evaluationsReceived = countEvaluations(evaluation -> evaluation.getEvaluatee().getId() == JOHN_ID);
        assertThat(activities).isPositive();
        assertThat(evaluationsWritten).isPositive();
        assertThat(evaluationsReceived).isPositive();

        // When
        deactivate(JOHN_ID, this.instructorBillToken).andExpect(status().isOk());

        // Then
        assertThat(countActivitiesOf(JOHN_ID)).isEqualTo(activities);
        assertThat(countEvaluations(evaluation -> evaluation.getEvaluator().getId() == JOHN_ID)).isEqualTo(evaluationsWritten);
        assertThat(countEvaluations(evaluation -> evaluation.getEvaluatee().getId() == JOHN_ID)).isEqualTo(evaluationsReceived);
    }

    private long countActivitiesOf(int studentId) {
        return this.activityRepository.findAll().stream().filter(activity -> activity.getStudent().getId() == studentId).count();
    }

    private long countEvaluations(Predicate<PeerEvaluation> predicate) {
        return this.peerEvaluationRepository.findAll().stream().filter(predicate).count();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student 6a: a student who is already deactivated")
    void testDeactivateStudentAlreadyDeactivated() throws Exception {
        // Given
        deactivate(JOHN_ID, this.instructorBillToken).andExpect(status().isOk());

        // When and then
        deactivate(JOHN_ID, this.instructorBillToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.CONFLICT))
                .andExpect(jsonPath("$.message").value("This student is already deactivated."));
    }

    @Test
    @DisplayName("UC-STU-deactivate-student 6b: an id that names no student")
    void testDeactivateStudentThatDoesNotExist() throws Exception {
        deactivate(999999, this.adminToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."));
    }

    @Test
    @DisplayName("UC-STU-deactivate-student 6c: the student herself may not deactivate her account")
    void testStudentDeactivatesHerself() throws Exception {
        deactivate(JOHN_ID, this.studentJohnToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."));
    }

    @Test
    @DisplayName("UC-STU-deactivate-student 6c: an instructor of another course's course section may not deactivate her")
    void testInstructorOfAnotherSectionDeactivatesStudent() throws Exception {
        // Tim Cook teaches and administers only the EE capstone course
        deactivate(JOHN_ID, this.adminTimToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."));

        // Nothing changed
        this.mockMvc.perform(get(this.baseUrl + "/students/" + JOHN_ID).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    @DisplayName("FR-SEC-active-account: the deactivated student's token stops working, and she cannot log in again")
    void testDeactivatedStudentIsLockedOut() throws Exception {
        // When
        deactivate(JOHN_ID, this.instructorBillToken).andExpect(status().isOk());

        // Then: the token issued before deactivation
        this.mockMvc.perform(get(this.baseUrl + "/students/" + JOHN_ID).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));

        // And a new login
        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

}
