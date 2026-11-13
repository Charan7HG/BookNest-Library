package com.pentagon.LibraryManagement;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.pentagon.LibraryManagement.Repository.PasswordResetRequestRepository;
import com.pentagon.LibraryManagement.Repository.UserRepository;
import com.pentagon.LibraryManagement.entity.PasswordResetRequest;
import com.pentagon.LibraryManagement.entity.PasswordResetRequestStatus;
import com.pentagon.LibraryManagement.entity.Profile;
import com.pentagon.LibraryManagement.entity.Role;
import com.pentagon.LibraryManagement.entity.Users;
import com.pentagon.LibraryManagement.security.JwtUtil;

/**
 * Focused tests for the public request endpoint, staff listing, and the
 * concurrency behaviour of approval.
 *
 * This class intentionally does not use a test-wide transaction so that the
 * parallel approval test exercises real competing database transactions.
 */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:librarypassword;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.jakarta.persistence.lock.timeout=5000"
})
@AutoConfigureMockMvc
class PasswordResetRequestIntegrationTests {

	private static final String ORIGINAL_PASSWORD = "original-password";

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
	void unknownEmailAndMemberEmailReturnIndistinguishableResponses() throws Exception {
		String unknownEmail = "unknown-" + UUID.randomUUID() + "@example.test";

		MvcResult unknownResult = mockMvc.perform(post("/password-reset-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content(emailJson(unknownEmail)))
			.andExpect(status().isOk())
			.andReturn();

		MvcResult memberResult = mockMvc.perform(post("/password-reset-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content(emailJson(member.getProfile().getEmail())))
			.andExpect(status().isOk())
			.andReturn();

		assertEquals(unknownResult.getResponse().getStatus(), memberResult.getResponse().getStatus());
		assertEquals(unknownResult.getResponse().getContentAsString(),
				memberResult.getResponse().getContentAsString());
		assertEquals(1, pendingRequestsFor(member.getId()),
				"Exactly one pending request should exist for the member");
		assertEquals(0, countRequestsForEmail(unknownEmail));
	}

	@Test
	void duplicateSubmissionsCreateOnlyOnePendingRequest() throws Exception {
		String body = emailJson(member.getProfile().getEmail());

		MvcResult first = mockMvc.perform(post("/password-reset-requests")
				.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isOk())
			.andReturn();
		MvcResult second = mockMvc.perform(post("/password-reset-requests")
				.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isOk())
			.andReturn();

		assertEquals(first.getResponse().getContentAsString(), second.getResponse().getContentAsString());
		assertEquals(1, pendingRequestsFor(member.getId()));
	}

	@Test
	void adminEmailsNeverCreateRequestsButLibrarianEmailsDo() throws Exception {
		mockMvc.perform(post("/password-reset-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content(emailJson(admin.getProfile().getEmail())))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers
					.containsString("If an eligible account exists")));
		mockMvc.perform(post("/password-reset-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content(emailJson(librarian.getProfile().getEmail())))
			.andExpect(status().isOk());

		// ADMIN must use the authenticated Change Password page instead.
		assertEquals(0, pendingRequestsFor(admin.getId()));
		assertEquals(1, pendingRequestsFor(librarian.getId()));
	}

	@Test
	void blankOrMissingEmailStillReturnsTheGenericResponse() throws Exception {
		mockMvc.perform(post("/password-reset-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"   \"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message")
					.value(org.hamcrest.Matchers.containsString("If an eligible account exists")));
		mockMvc.perform(post("/password-reset-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isOk());
	}

	@Test
	void adminAndLibrarianCanListPendingAndHistoryRequests() throws Exception {
		PasswordResetRequest request = submitRequestForMember();

		mockMvc.perform(get("/password-reset-requests/pending")
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[*].memberEmail")
					.value(hasItem(member.getProfile().getEmail())))
			.andExpect(jsonPath("$.data[*].status").value(hasItem("PENDING")))
			.andExpect(jsonPath("$.data[*].temporaryPassword").doesNotExist());

		mockMvc.perform(get("/password-reset-requests/pending")
				.header("Authorization", bearer(librarian)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[*].memberEmail")
					.value(hasItem(member.getProfile().getEmail())));

		mockMvc.perform(post("/password-reset-requests/{id}/approve", request.getId())
				.header("Authorization", bearer(librarian)))
			.andExpect(status().isOk());

		// Completed requests disappear from the pending queue but remain in history.
		mockMvc.perform(get("/password-reset-requests/pending")
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[*].memberEmail")
					.value(org.hamcrest.Matchers.not(hasItem(member.getProfile().getEmail()))));

		mockMvc.perform(get("/password-reset-requests")
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[*].memberEmail")
					.value(hasItem(member.getProfile().getEmail())))
			.andExpect(jsonPath("$.data[*].status")
					.value(hasItem("APPROVED")))
			.andExpect(jsonPath("$.data[*].temporaryPassword").doesNotExist());
	}

	@Test
	void approvalReturnsTheTemporaryPasswordOnlyOnceAndNeverInHistory() throws Exception {
		PasswordResetRequest request = submitRequestForMember();

		MvcResult approval = mockMvc.perform(post("/password-reset-requests/{id}/approve", request.getId())
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.temporaryPassword").isNotEmpty())
			.andReturn();

		String temporaryPassword = JsonPath.read(approval.getResponse().getContentAsString(),
				"$.data.temporaryPassword");
		Users persisted = userRepository.findById(member.getId()).orElseThrow();

		assertTrue(persisted.isPasswordChangeRequired());
		assertNotEquals(temporaryPassword, persisted.getPassword());
		assertTrue(persisted.getPassword().startsWith("$2"));
		assertTrue(passwordEncoder.matches(temporaryPassword, persisted.getPassword()));
		assertFalse(approval.getResponse().getContentAsString().contains(persisted.getPassword()));

		MvcResult history = mockMvc.perform(get("/password-reset-requests")
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk())
			.andReturn();
		String historyBody = history.getResponse().getContentAsString();
		assertFalse(historyBody.contains(temporaryPassword));
		assertFalse(historyBody.contains(persisted.getPassword()));

		// A completed request can never be approved again, so no second password exists.
		mockMvc.perform(post("/password-reset-requests/{id}/approve", request.getId())
				.header("Authorization", bearer(admin)))
			.andExpect(status().isConflict());
	}

	@Test
	void concurrentApprovalsGenerateOnlyOneTemporaryPassword() throws Exception {
		PasswordResetRequest request = submitRequestForMember();
		String adminToken = bearer(admin);
		String url = "/password-reset-requests/" + request.getId() + "/approve";

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		try {
			Future<MvcResult> first = executor.submit(() -> {
				start.await();
				return mockMvc.perform(post(url).header("Authorization", adminToken)).andReturn();
			});
			Future<MvcResult> second = executor.submit(() -> {
				start.await();
				return mockMvc.perform(post(url).header("Authorization", adminToken)).andReturn();
			});

			start.countDown();
			MvcResult firstResult;
			MvcResult secondResult;
			try {
				firstResult = first.get(30, TimeUnit.SECONDS);
				secondResult = second.get(30, TimeUnit.SECONDS);
			} catch (java.util.concurrent.ExecutionException exception) {
				throw new AssertionError("An approval attempt failed unexpectedly", exception.getCause());
			}

			List<Integer> statuses = List.of(
					firstResult.getResponse().getStatus(),
					secondResult.getResponse().getStatus()).stream().sorted().toList();
			assertEquals(List.of(200, 409), statuses,
					"Exactly one approval must succeed and the other must see a completed request");

			MvcResult winner = firstResult.getResponse().getStatus() == 200 ? firstResult : secondResult;
			String temporaryPassword = JsonPath.read(winner.getResponse().getContentAsString(),
					"$.data.temporaryPassword");

			Users persisted = userRepository.findById(member.getId()).orElseThrow();
			assertTrue(persisted.isPasswordChangeRequired());
			assertTrue(passwordEncoder.matches(temporaryPassword, persisted.getPassword()));
			assertEquals(PasswordResetRequestStatus.APPROVED,
					requestRepository.findById(request.getId()).orElseThrow().getStatus());
			assertNotNull(requestRepository.findById(request.getId()).orElseThrow().getReviewedBy());
		} finally {
			executor.shutdownNow();
		}
	}

	private PasswordResetRequest submitRequestForMember() throws Exception {
		mockMvc.perform(post("/password-reset-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content(emailJson(member.getProfile().getEmail())))
			.andExpect(status().isOk());
		return requestRepository.findByStatusOrderByRequestedAtAsc(PasswordResetRequestStatus.PENDING)
				.stream()
				.filter(request -> request.getUser().getId() == member.getId())
				.findFirst()
				.orElseThrow();
	}

	private int pendingRequestsFor(int memberId) {
		return (int) requestRepository.findAll().stream()
				.filter(request -> request.getUser().getId() == memberId)
				.filter(request -> request.getStatus() == PasswordResetRequestStatus.PENDING)
				.count();
	}

	private long countRequestsForEmail(String email) {
		return requestRepository.findAll().stream()
				.filter(request -> request.getUser().getProfile() != null)
				.filter(request -> email.equals(request.getUser().getProfile().getEmail()))
				.count();
	}

	private String emailJson(String email) {
		return "{\"email\":\"" + email + "\"}";
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
