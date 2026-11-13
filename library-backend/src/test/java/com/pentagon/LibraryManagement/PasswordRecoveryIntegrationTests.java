package com.pentagon.LibraryManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.pentagon.LibraryManagement.Repository.PasswordResetRequestRepository;
import com.pentagon.LibraryManagement.Repository.UserRepository;
import com.pentagon.LibraryManagement.entity.PasswordResetRequest;
import com.pentagon.LibraryManagement.entity.PasswordResetRequestStatus;
import com.pentagon.LibraryManagement.entity.Profile;
import com.pentagon.LibraryManagement.entity.Role;
import com.pentagon.LibraryManagement.entity.Users;
import com.pentagon.LibraryManagement.security.JwtUtil;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:librarypassword;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.jakarta.persistence.lock.timeout=5000"
})
@AutoConfigureMockMvc
@Transactional
class PasswordRecoveryIntegrationTests {

	private static final String ORIGINAL_PASSWORD = "original-password";
	private static final String MEMBER_NEW_PASSWORD = "MemberChosenPass!42";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordResetRequestRepository requestRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtUtil jwtUtil;

	private Users admin;
	private Users librarian;
	private Users member;

	@BeforeEach
	void createAccounts() {
		String suffix = UUID.randomUUID().toString();
		admin = saveAccount("admin-" + suffix + "@example.test", Role.ADMIN);
		librarian = saveAccount("librarian-" + suffix + "@example.test", Role.LIBRARIAN);
		member = saveAccount("member-" + suffix + "@example.test", Role.MEMBER);
	}

	@Test
	void adminCanApproveMemberResetRequestAndOnlyTheBcryptHashIsStored() throws Exception {
		PasswordResetRequest request = submitRequestForMember();

		MvcResult result = mockMvc.perform(post("/password-reset-requests/{id}/approve", request.getId())
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.statuscode").value(200))
			.andExpect(jsonPath("$.data.requestId").value(request.getId()))
			.andExpect(jsonPath("$.data.userId").value(member.getId()))
			.andExpect(jsonPath("$.data.email").value(member.getProfile().getEmail()))
			.andExpect(jsonPath("$.data.temporaryPassword").isNotEmpty())
			.andExpect(jsonPath("$.data.passwordChangeRequired").value(true))
			.andExpect(jsonPath("$.data.password").doesNotExist())
			.andReturn();

		String temporaryPassword = JsonPath.read(result.getResponse().getContentAsString(),
				"$.data.temporaryPassword");
		Users persisted = userRepository.findById(member.getId()).orElseThrow();

		assertTrue(persisted.isPasswordChangeRequired());
		assertNotEquals(temporaryPassword, persisted.getPassword());
		assertTrue(persisted.getPassword().startsWith("$2"), "Temporary password must be BCrypt hashed");
		assertTrue(passwordEncoder.matches(temporaryPassword, persisted.getPassword()));
		assertFalse(result.getResponse().getContentAsString().contains(persisted.getPassword()));

		PasswordResetRequest reviewed = requestRepository.findById(request.getId()).orElseThrow();
		assertEquals(PasswordResetRequestStatus.APPROVED, reviewed.getStatus());
		assertEquals(admin.getId(), reviewed.getReviewedBy().getId());
		assertNotNull(reviewed.getReviewedAt());
	}

	@Test
	void librarianCanApproveMemberResetRequest() throws Exception {
		PasswordResetRequest request = submitRequestForMember();

		mockMvc.perform(post("/password-reset-requests/{id}/approve", request.getId())
				.header("Authorization", bearer(librarian)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.temporaryPassword").isNotEmpty())
			.andExpect(jsonPath("$.data.passwordChangeRequired").value(true));

		assertTrue(userRepository.findById(member.getId()).orElseThrow().isPasswordChangeRequired());
	}

	@Test
	void memberCannotViewOrProcessPasswordResetRequests() throws Exception {
		PasswordResetRequest request = submitRequestForMember();
		String originalHash = userRepository.findById(member.getId()).orElseThrow().getPassword();

		mockMvc.perform(get("/password-reset-requests/pending")
				.header("Authorization", bearer(member)))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/password-reset-requests")
				.header("Authorization", bearer(member)))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/password-reset-requests/{id}/approve", request.getId())
				.header("Authorization", bearer(member)))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/password-reset-requests/{id}/reject", request.getId())
				.header("Authorization", bearer(member)))
			.andExpect(status().isForbidden());

		PasswordResetRequest unchanged = requestRepository.findById(request.getId()).orElseThrow();
		assertEquals(PasswordResetRequestStatus.PENDING, unchanged.getStatus());
		assertEquals(originalHash, userRepository.findById(member.getId()).orElseThrow().getPassword());
		assertFalse(userRepository.findById(member.getId()).orElseThrow().isPasswordChangeRequired());
	}

	@Test
	void unauthenticatedUserCannotAccessStaffEndpoints() throws Exception {
		PasswordResetRequest request = submitRequestForMember();

		mockMvc.perform(get("/password-reset-requests/pending"))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/password-reset-requests"))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/password-reset-requests/{id}/approve", request.getId()))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/password-reset-requests/{id}/reject", request.getId()))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void adminAccountsCannotBeTargetedButLibrariansCanSubmitRequests() throws Exception {
		mockMvc.perform(post("/password-reset-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content(emailJson(admin.getProfile().getEmail())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message")
					.value(org.hamcrest.Matchers.containsString("If an eligible account exists")));
		mockMvc.perform(post("/password-reset-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content(emailJson(librarian.getProfile().getEmail())))
			.andExpect(status().isOk());

		assertEquals(0, pendingRequestsFor(admin.getId()));
		assertEquals(1, pendingRequestsFor(librarian.getId()));
	}

	@Test
	void onlyAdminCanApproveLibrarianRequestsAndLibrarianCannotSelfApprove() throws Exception {
		PasswordResetRequest request = submitRequestFor(librarian);

		// A librarian cannot process even their own request, and the librarian
		// queue view deliberately hides librarian requests.
		mockMvc.perform(get("/password-reset-requests/pending")
				.header("Authorization", bearer(librarian)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[*].accountRole")
					.value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("LIBRARIAN"))));
		mockMvc.perform(post("/password-reset-requests/{id}/approve", request.getId())
				.header("Authorization", bearer(librarian)))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/password-reset-requests/{id}/reject", request.getId())
				.header("Authorization", bearer(librarian)))
			.andExpect(status().isForbidden());

		assertEquals(PasswordResetRequestStatus.PENDING,
				requestRepository.findById(request.getId()).orElseThrow().getStatus());

		// ADMIN sees and approves it.
		mockMvc.perform(get("/password-reset-requests/pending")
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[*].accountRole")
					.value(org.hamcrest.Matchers.hasItem("LIBRARIAN")));

		mockMvc.perform(post("/password-reset-requests/{id}/approve", request.getId())
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.temporaryPassword").isNotEmpty())
			.andExpect(jsonPath("$.data.passwordChangeRequired").value(true));
	}

	@Test
	void librarianForcedPasswordChangeWorksAndTheOldPasswordStopsWorking() throws Exception {
		PasswordResetRequest request = submitRequestFor(librarian);

		MvcResult approval = mockMvc.perform(post("/password-reset-requests/{id}/approve", request.getId())
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk())
			.andReturn();
		String temporaryPassword = JsonPath.read(approval.getResponse().getContentAsString(),
				"$.data.temporaryPassword");

		MvcResult loginResult = mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(librarian.getProfile().getEmail(), temporaryPassword)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.passwordChangeRequired").value(true))
			.andExpect(jsonPath("$.data.user.passwordChangeRequired").value(true))
			.andReturn();
		String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.token");

		mockMvc.perform(get("/books").header("Authorization", "Bearer " + token))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("Password change required before using other endpoints"));

		mockMvc.perform(post("/change-password")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(changePasswordJson(temporaryPassword, "LibrarianNewPass!42")))
			.andExpect(status().isOk());

		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(librarian.getProfile().getEmail(), temporaryPassword)))
			.andExpect(status().isUnauthorized());
		assertNotNull(login(librarian.getProfile().getEmail(), "LibrarianNewPass!42"));
	}

	@Test
	void adminCanChangeOwnPasswordWhileAuthenticated() throws Exception {
		String token = login(admin.getProfile().getEmail(), ORIGINAL_PASSWORD);

		mockMvc.perform(post("/change-password")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(changePasswordJson(ORIGINAL_PASSWORD, "AdminNewPass!42")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Password changed successfully"));

		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(admin.getProfile().getEmail(), ORIGINAL_PASSWORD)))
			.andExpect(status().isUnauthorized());
		assertNotNull(login(admin.getProfile().getEmail(), "AdminNewPass!42"));
	}

	@Test
	void directStaffResetEndpointNoLongerExists() throws Exception {
		String originalHash = userRepository.findById(member.getId()).orElseThrow().getPassword();

		// ADMIN may reach /user/** but the reset endpoint is gone, so nothing resets.
		mockMvc.perform(post("/user/{id}/reset-password", member.getId())
				.header("Authorization", bearer(admin)))
			.andExpect(status().isNotFound());
		// LIBRARIAN cannot reach /user/** at all.
		mockMvc.perform(post("/user/{id}/reset-password", member.getId())
				.header("Authorization", bearer(librarian)))
			.andExpect(status().isForbidden());

		assertEquals(originalHash, userRepository.findById(member.getId()).orElseThrow().getPassword());
		assertFalse(userRepository.findById(member.getId()).orElseThrow().isPasswordChangeRequired());
	}

	@Test
	void onlyPendingRequestsCanBeProcessed() throws Exception {
		PasswordResetRequest request = submitRequestForMember();

		mockMvc.perform(post("/password-reset-requests/{id}/approve", request.getId())
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk());
		mockMvc.perform(post("/password-reset-requests/{id}/approve", request.getId())
				.header("Authorization", bearer(admin)))
			.andExpect(status().isConflict());
		mockMvc.perform(post("/password-reset-requests/{id}/reject", request.getId())
				.header("Authorization", bearer(admin)))
			.andExpect(status().isConflict());

		assertEquals(PasswordResetRequestStatus.APPROVED,
				requestRepository.findById(request.getId()).orElseThrow().getStatus());
	}

	@Test
	void rejectionDoesNotChangeTheMemberPassword() throws Exception {
		PasswordResetRequest request = submitRequestForMember();
		String originalHash = userRepository.findById(member.getId()).orElseThrow().getPassword();

		mockMvc.perform(post("/password-reset-requests/{id}/reject", request.getId())
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("REJECTED"))
			.andExpect(jsonPath("$.data.temporaryPassword").doesNotExist());

		Users persisted = userRepository.findById(member.getId()).orElseThrow();
		assertEquals(originalHash, persisted.getPassword());
		assertFalse(persisted.isPasswordChangeRequired());

		PasswordResetRequest reviewed = requestRepository.findById(request.getId()).orElseThrow();
		assertEquals(PasswordResetRequestStatus.REJECTED, reviewed.getStatus());
		assertEquals(admin.getId(), reviewed.getReviewedBy().getId());
		assertNotNull(reviewed.getReviewedAt());
	}

	@Test
	void missingRequestReturnsNotFoundWithoutRevealingDetails() throws Exception {
		mockMvc.perform(post("/password-reset-requests/{id}/approve", Integer.MAX_VALUE)
				.header("Authorization", bearer(admin)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Password reset request not found"))
			.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	void memberMustChangeTemporaryPasswordBeforeUsingNormalEndpoints() throws Exception {
		String temporaryPassword = submitAndApproveAs(admin);
		String token = login(member.getProfile().getEmail(), temporaryPassword);

		mockMvc.perform(get("/books").header("Authorization", "Bearer " + token))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("Password change required before using other endpoints"));
		mockMvc.perform(get("/dashboard").header("Authorization", "Bearer " + token))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/borrow-requests")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"bookId\":1}"))
			.andExpect(status().isForbidden());

		mockMvc.perform(post("/change-password")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(changePasswordJson(temporaryPassword, MEMBER_NEW_PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Password changed successfully"));

		Users persisted = userRepository.findById(member.getId()).orElseThrow();
		assertFalse(persisted.isPasswordChangeRequired());
		assertTrue(passwordEncoder.matches(MEMBER_NEW_PASSWORD, persisted.getPassword()));

		mockMvc.perform(get("/books").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk());

		assertNotNull(login(member.getProfile().getEmail(), MEMBER_NEW_PASSWORD));
		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(member.getProfile().getEmail(), temporaryPassword)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void existingUsersAreNotForcedToChangePasswords() throws Exception {
		Users persisted = userRepository.findById(member.getId()).orElseThrow();
		assertFalse(persisted.isPasswordChangeRequired());
		assertTrue(passwordEncoder.matches(ORIGINAL_PASSWORD, persisted.getPassword()));

		String token = login(member.getProfile().getEmail(), ORIGINAL_PASSWORD);
		mockMvc.perform(get("/books").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk());

		String email = "new-" + UUID.randomUUID() + "@example.test";
		MvcResult created = mockMvc.perform(post("/user")
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New Reader\",\"profile\":{\"email\":\"" + email
						+ "\",\"phone\":123456789},\"password\":\"starter-password\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.passwordChangeRequired").value(false))
			.andReturn();

		int newId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");
		assertFalse(userRepository.findById(newId).orElseThrow().isPasswordChangeRequired());
		assertNotNull(login(email, "starter-password"));
	}

	@Test
	void changePasswordRejectsWrongCurrentPasswordAndBlankNewPassword() throws Exception {
		String token = login(member.getProfile().getEmail(), ORIGINAL_PASSWORD);
		String originalHash = userRepository.findById(member.getId()).orElseThrow().getPassword();

		mockMvc.perform(post("/change-password")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(changePasswordJson("wrong-current-password", MEMBER_NEW_PASSWORD)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Current password is incorrect"));
		assertEquals(originalHash, userRepository.findById(member.getId()).orElseThrow().getPassword());

		mockMvc.perform(post("/change-password")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(changePasswordJson(ORIGINAL_PASSWORD, "")))
			.andExpect(status().isBadRequest());
		assertEquals(originalHash, userRepository.findById(member.getId()).orElseThrow().getPassword());
	}

	@Test
	void legacyForgotPasswordEndpointIsDisabledAndCannotOverwritePasswords() throws Exception {
		String originalHash = userRepository.findById(member.getId()).orElseThrow().getPassword();
		String attackBody = "{\"profile\":{\"email\":\"" + member.getProfile().getEmail()
				+ "\"},\"password\":\"attacker-chosen-password\"}";

		mockMvc.perform(post("/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(attackBody))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/forgot-password")
				.header("Authorization", bearer(member))
				.contentType(MediaType.APPLICATION_JSON)
				.content(attackBody))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/forgot-password")
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(attackBody))
			.andExpect(status().isForbidden());

		String currentHash = userRepository.findById(member.getId()).orElseThrow().getPassword();
		assertEquals(originalHash, currentHash);
		assertFalse(passwordEncoder.matches("attacker-chosen-password", currentHash));
	}

	private PasswordResetRequest submitRequestForMember() throws Exception {
		return submitRequestFor(member);
	}

	private PasswordResetRequest submitRequestFor(Users account) throws Exception {
		mockMvc.perform(post("/password-reset-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content(emailJson(account.getProfile().getEmail())))
			.andExpect(status().isOk());
		return pendingRequestFor(account.getId());
	}

	private PasswordResetRequest pendingRequestFor(int memberId) {
		return requestRepository.findByStatusOrderByRequestedAtAsc(PasswordResetRequestStatus.PENDING)
				.stream()
				.filter(request -> request.getUser().getId() == memberId)
				.findFirst()
				.orElseThrow();
	}

	private int pendingRequestsFor(int memberId) {
		return (int) requestRepository.findAll().stream()
				.filter(request -> request.getUser().getId() == memberId)
				.filter(request -> request.getStatus() == PasswordResetRequestStatus.PENDING)
				.count();
	}

	private String submitAndApproveAs(Users staff) throws Exception {
		PasswordResetRequest request = submitRequestForMember();
		MvcResult result = mockMvc.perform(post("/password-reset-requests/{id}/approve", request.getId())
				.header("Authorization", bearer(staff)))
			.andExpect(status().isOk())
			.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.data.temporaryPassword");
	}

	private String login(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(email, password)))
			.andExpect(status().isOk())
			.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
	}

	private String emailJson(String email) {
		return "{\"email\":\"" + email + "\"}";
	}

	private String loginJson(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	private String changePasswordJson(String currentPassword, String newPassword) {
		return "{\"currentPassword\":\"" + currentPassword + "\",\"newPassword\":\"" + newPassword + "\"}";
	}

	private Users saveAccount(String email, Role role) {
		Profile profile = new Profile();
		profile.setEmail(email);
		profile.setPhone(123456789L);
		Users user = new Users();
		user.setName(role.name() + " Test Account");
		user.setProfile(profile);
		user.setRole(role);
		user.setPassword(passwordEncoder.encode(ORIGINAL_PASSWORD));
		return userRepository.saveAndFlush(user);
	}

	private String bearer(Users user) {
		return "Bearer " + jwtUtil.generateToken(user.getProfile().getEmail(), user.getRole().name());
	}
}
