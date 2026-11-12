package com.pentagon.LibraryManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import com.pentagon.LibraryManagement.Repository.AuthorRepository;
import com.pentagon.LibraryManagement.Repository.BookRepository;
import com.pentagon.LibraryManagement.Repository.CategoryRepository;
import com.pentagon.LibraryManagement.Repository.UserRepository;
import com.pentagon.LibraryManagement.entity.Author;
import com.pentagon.LibraryManagement.entity.Book;
import com.pentagon.LibraryManagement.entity.Category;
import com.pentagon.LibraryManagement.entity.Profile;
import com.pentagon.LibraryManagement.entity.Role;
import com.pentagon.LibraryManagement.entity.Users;
import com.pentagon.LibraryManagement.security.JwtUtil;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:librarycrud;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class CrudIntegrationTests {

	private static final String ORIGINAL_PASSWORD = "original-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private AuthorRepository authorRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtUtil jwtUtil;

	private Users admin;
	private Users librarian;
	private Users member;

	@BeforeEach
	void createRoleAccounts() {
		String suffix = UUID.randomUUID().toString();
		admin = saveAccount("admin-" + suffix + "@example.test", Role.ADMIN);
		librarian = saveAccount("librarian-" + suffix + "@example.test", Role.LIBRARIAN);
		member = saveAccount("member-" + suffix + "@example.test", Role.MEMBER);
	}

	@Test
	void adminCanCreateAndReadUsersWithoutExposingPasswordHashes() throws Exception {
		String email = "new-" + UUID.randomUUID() + "@example.test";
		MvcResult created = mockMvc.perform(post("/user")
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(userJson("New Reader", email, 99887766L, "new-password", null)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.password").doesNotExist())
			.andReturn();

		int id = idFromResponse(created.getResponse().getContentAsString());
		Users persisted = userRepository.findById(id).orElseThrow();
		assertEquals("New Reader", persisted.getName());
		assertEquals(Role.MEMBER, persisted.getRole());
		assertEquals(email, persisted.getProfile().getEmail());
		assertTrue(passwordEncoder.matches("new-password", persisted.getPassword()));
		assertFalse(created.getResponse().getContentAsString().contains(persisted.getPassword()));

		mockMvc.perform(get("/users").header("Authorization", bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].profile.email")
						.value(org.hamcrest.Matchers.hasItem(email)))
				.andExpect(jsonPath("$.data[0].password").doesNotExist());
		mockMvc.perform(get("/user/{id}", id).header("Authorization", bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.profile.email").value(email))
				.andExpect(jsonPath("$.data.password").doesNotExist());
	}

	@Test
	void userCreationRejectsMissingEmailPasswordAndDuplicateEmail() throws Exception {
		long before = userRepository.count();
		mockMvc.perform(post("/user")
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"No email\",\"password\":\"pw\"}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(post("/user")
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(userJson("No password", "missing-password@example.test", 123L, null, null)))
			.andExpect(status().isBadRequest());
		mockMvc.perform(post("/user")
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(userJson("Blank password", "blank-password@example.test", 123L, "", null)))
			.andExpect(status().isBadRequest());

		String duplicateEmail = admin.getProfile().getEmail();
		mockMvc.perform(post("/user")
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(userJson("Duplicate", duplicateEmail, 111L, "pw", null)))
			.andExpect(status().isConflict());
		assertEquals(before, userRepository.count());
	}

	@Test
	void userUpdatesPreserveOrReplacePasswordOnlyWhenRequested() throws Exception {
		Users original = saveAccount("update-" + UUID.randomUUID() + "@example.test", Role.MEMBER);
		String oldHash = original.getPassword();
		String changedEmail = "changed-" + UUID.randomUUID() + "@example.test";

		mockMvc.perform(put("/user/{id}", original.getId())
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(userJson("Updated Name", changedEmail, 7654321L, null, null)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.statuscode").value(200))
			.andExpect(jsonPath("$.data.password").doesNotExist());

		Users updated = userRepository.findById(original.getId()).orElseThrow();
		assertEquals("Updated Name", updated.getName());
		assertEquals(changedEmail, updated.getProfile().getEmail());
		assertEquals(7654321L, updated.getProfile().getPhone());
		assertEquals(oldHash, updated.getPassword());

		mockMvc.perform(put("/user/{id}", updated.getId())
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(userJson("Updated Again", changedEmail, 7654322L, "", null)))
			.andExpect(status().isOk());
		assertEquals(oldHash, userRepository.findById(updated.getId()).orElseThrow().getPassword());

		mockMvc.perform(put("/user/{id}", updated.getId())
				.header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(userJson("Updated Password", changedEmail, 7654323L, "new-password", null)))
			.andExpect(status().isOk());
		String newHash = userRepository.findById(updated.getId()).orElseThrow().getPassword();
		assertNotNull(newHash);
		assertTrue(passwordEncoder.matches("new-password", newHash));
		assertFalse(newHash.equals(oldHash));
	}

	@Test
	void userDeleteAndMissingIdUseNotFoundResponses() throws Exception {
		Users removable = saveAccount("delete-" + UUID.randomUUID() + "@example.test", Role.MEMBER);
		mockMvc.perform(delete("/user/{id}", removable.getId()).header("Authorization", bearer(admin)))
				.andExpect(status().isOk());
		assertFalse(userRepository.existsById(removable.getId()));
		mockMvc.perform(get("/user/{id}", Integer.MAX_VALUE).header("Authorization", bearer(admin)))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/user/{id}", Integer.MAX_VALUE).header("Authorization", bearer(admin)))
				.andExpect(status().isNotFound());
	}

	@Test
	void deletingBorrowerAuthorOrCategoryKeepsBooksAndUnlinksTheRelationship() throws Exception {
		Users borrower = saveAccount("borrower-" + UUID.randomUUID() + "@example.test", Role.MEMBER);
		Book borrowedBook = new Book();
		borrowedBook.setName("Keep After Borrower Delete");
		borrowedBook.setPrice(12.5);
		borrowedBook.setUsers(borrower);
		int borrowedBookId = bookRepository.saveAndFlush(borrowedBook).getId();

		Author author = new Author();
		author.setName("Keep Book After Author Delete");
		author = authorRepository.saveAndFlush(author);
		Book authoredBook = new Book();
		authoredBook.setName("Keep After Author Delete");
		authoredBook.setPrice(13.5);
		authoredBook.setAuthor(author);
		int authoredBookId = bookRepository.saveAndFlush(authoredBook).getId();

		Category category = new Category();
		category.setName("Keep Book After Category Delete");
		category = categoryRepository.saveAndFlush(category);
		Book categorizedBook = new Book();
		categorizedBook.setName("Keep After Category Delete");
		categorizedBook.setPrice(14.5);
		categorizedBook.setCategory(new java.util.ArrayList<>(List.of(category)));
		int categorizedBookId = bookRepository.saveAndFlush(categorizedBook).getId();

		mockMvc.perform(delete("/user/{id}", borrower.getId()).header("Authorization", bearer(admin)))
				.andExpect(status().isOk());
		assertTrue(userRepository.existsById(admin.getId()));
		assertNotNull(userRepository.findById(admin.getId()).orElseThrow().getProfile());
		mockMvc.perform(get("/users").header("Authorization", bearer(admin)))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/author/{id}", author.getId()).header("Authorization", bearer(admin)))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/category/{id}", category.getId()).header("Authorization", bearer(admin)))
				.andExpect(status().isOk());

		assertTrue(bookRepository.existsById(borrowedBookId));
		assertNull(bookRepository.findById(borrowedBookId).orElseThrow().getUsers());
		assertTrue(bookRepository.existsById(authoredBookId));
		assertNull(bookRepository.findById(authoredBookId).orElseThrow().getAuthor());
		assertTrue(bookRepository.existsById(categorizedBookId));
		assertTrue(bookRepository.findById(categorizedBookId).orElseThrow().getCategory().isEmpty());
	}

	@Test
	void userSearchIsAdminOnlyCaseInsensitiveAndDoesNotModifyRecords() throws Exception {
		Users matching = saveAccount("search-user-" + UUID.randomUUID() + "@example.test", Role.MEMBER);
		matching.setName("Searchable Reader");
		matching = userRepository.save(matching);
		long before = userRepository.count();

		mockMvc.perform(get("/user/search").param("name", "SEARCHABLE")
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].name").value("Searchable Reader"));
		mockMvc.perform(get("/user/search").param("name", "does-not-match")
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(0));
		mockMvc.perform(get("/user/search").param("name", "")
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(4));
		for (Users roleUser : List.of(member, librarian)) {
			mockMvc.perform(get("/user/search").param("name", "Searchable")
					.header("Authorization", bearer(roleUser)))
				.andExpect(status().isForbidden());
		}
		assertEquals(before, userRepository.count());
	}

	@Test
	void nonAdminCannotUseUserManagementRoutes() throws Exception {
		for (Users roleUser : List.of(member, librarian)) {
			String token = bearer(roleUser);
			mockMvc.perform(get("/users").header("Authorization", token)).andExpect(status().isForbidden());
			mockMvc.perform(get("/user/{id}", admin.getId()).header("Authorization", token))
					.andExpect(status().isForbidden());
			mockMvc.perform(post("/user").header("Authorization", token)
					.contentType(MediaType.APPLICATION_JSON)
					.content(userJson("Blocked", "blocked@example.test", 123L, "pw", null)))
				.andExpect(status().isForbidden());
		}
	}

	@Test
	void bookCrudSearchAndCatalogPermissionsWorkThroughPersistence() throws Exception {
		int adminBook = createBook(admin, "Regression Testing Handbook");
		int librarianBook = createBook(librarian, "Java Library Guide");
		Book blockedBook = new Book();
		blockedBook.setName("Blocked Book");
		blockedBook.setPrice(19.95);
		int memberBook = bookRepository.saveAndFlush(blockedBook).getId();

		for (Users roleUser : List.of(admin, librarian, member)) {
			mockMvc.perform(get("/books").header("Authorization", bearer(roleUser)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.length()").value(3));
			mockMvc.perform(get("/book/{id}", adminBook).header("Authorization", bearer(roleUser)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.name").value("Regression Testing Handbook"));
		}
		mockMvc.perform(get("/book/{id}", Integer.MAX_VALUE).header("Authorization", bearer(member)))
				.andExpect(status().isNotFound());
		mockMvc.perform(put("/book/{id}", Integer.MAX_VALUE).header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON).content(bookJson("Missing")))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/book/{id}", Integer.MAX_VALUE).header("Authorization", bearer(librarian)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/book/search").param("name", "REGRESSION")
				.header("Authorization", bearer(member)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
		mockMvc.perform(get("/book/search").param("name", "missing")
				.header("Authorization", bearer(librarian)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
		long booksBeforeEmptySearch = bookRepository.count();
		mockMvc.perform(get("/book/search").param("name", "")
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(3));
		assertEquals(booksBeforeEmptySearch, bookRepository.count());

		mockMvc.perform(post("/book").header("Authorization", bearer(member))
				.contentType(MediaType.APPLICATION_JSON).content(bookJson("Denied")))
			.andExpect(status().isForbidden());
		mockMvc.perform(put("/book/{id}", adminBook).header("Authorization", bearer(member))
				.contentType(MediaType.APPLICATION_JSON).content(bookJson("Denied Update")))
			.andExpect(status().isForbidden());
		mockMvc.perform(delete("/book/{id}", memberBook).header("Authorization", bearer(member)))
			.andExpect(status().isForbidden());

		mockMvc.perform(put("/book/{id}", adminBook).header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON).content(bookJson("Updated by Admin")))
			.andExpect(status().isOk());
		mockMvc.perform(put("/book/{id}", librarianBook).header("Authorization", bearer(librarian))
				.contentType(MediaType.APPLICATION_JSON).content(bookJson("Updated by Librarian")))
			.andExpect(status().isOk());
		assertEquals("Updated by Admin", bookRepository.findById(adminBook).orElseThrow().getName());
		assertEquals("Updated by Librarian", bookRepository.findById(librarianBook).orElseThrow().getName());

		mockMvc.perform(delete("/book/{id}", adminBook).header("Authorization", bearer(admin)))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/book/{id}", librarianBook).header("Authorization", bearer(librarian)))
				.andExpect(status().isOk());
		assertFalse(bookRepository.existsById(adminBook));
		assertFalse(bookRepository.existsById(librarianBook));
	}

	@Test
	void booksWithDuplicateNamesCanBeSavedBecauseNamesAreNotUniqueInTheEntity() throws Exception {
		int first = createBook(admin, "Shared Title");
		int second = createBook(librarian, "Shared Title");

		assertNotEquals(first, second);
		assertEquals(2, bookRepository.count());
		mockMvc.perform(get("/book/{id}", first).header("Authorization", bearer(member)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Shared Title"));
		mockMvc.perform(get("/book/{id}", second).header("Authorization", bearer(member)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Shared Title"));
	}

	@Test
	void authorCrudSearchAndCatalogPermissionsWorkThroughPersistence() throws Exception {
		int adminAuthor = createAuthor(admin, "Ursula Test Writer");
		int librarianAuthor = createAuthor(librarian, "Jules Regression");

		for (Users roleUser : List.of(admin, librarian, member)) {
			mockMvc.perform(get("/authors").header("Authorization", bearer(roleUser)))
					.andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
			mockMvc.perform(get("/author/{id}", adminAuthor).header("Authorization", bearer(roleUser)))
					.andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Ursula Test Writer"));
		}
		mockMvc.perform(get("/author/{id}", Integer.MAX_VALUE).header("Authorization", bearer(member)))
				.andExpect(status().isNotFound());
		long authorCountBeforeSearch = authorRepository.count();
		mockMvc.perform(get("/author/search").param("name", "URSULA")
				.header("Authorization", bearer(member)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
		mockMvc.perform(get("/author/search").param("name", "absent")
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
		mockMvc.perform(get("/author/search").param("name", "")
				.header("Authorization", bearer(librarian)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
		assertEquals(authorCountBeforeSearch, authorRepository.count());

		mockMvc.perform(post("/author").header("Authorization", bearer(member))
				.contentType(MediaType.APPLICATION_JSON).content(authorJson("Denied")))
			.andExpect(status().isForbidden());
		mockMvc.perform(put("/author/{id}", adminAuthor).header("Authorization", bearer(member))
				.contentType(MediaType.APPLICATION_JSON).content(authorJson("Denied")))
			.andExpect(status().isForbidden());
		mockMvc.perform(delete("/author/{id}", librarianAuthor).header("Authorization", bearer(member)))
			.andExpect(status().isForbidden());

		mockMvc.perform(put("/author/{id}", adminAuthor).header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON).content(authorJson("Admin Updated")))
			.andExpect(status().isOk());
		mockMvc.perform(put("/author/{id}", librarianAuthor).header("Authorization", bearer(librarian))
				.contentType(MediaType.APPLICATION_JSON).content(authorJson("Librarian Updated")))
			.andExpect(status().isOk());
		assertEquals("Admin Updated", authorRepository.findById(adminAuthor).orElseThrow().getName());
		assertEquals("Librarian Updated", authorRepository.findById(librarianAuthor).orElseThrow().getName());
		mockMvc.perform(delete("/author/{id}", adminAuthor).header("Authorization", bearer(admin)))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/author/{id}", librarianAuthor).header("Authorization", bearer(librarian)))
				.andExpect(status().isOk());
		assertFalse(authorRepository.existsById(adminAuthor));
		assertFalse(authorRepository.existsById(librarianAuthor));
	}

	@Test
	void categoryCrudSearchAndCatalogPermissionsWorkThroughPersistence() throws Exception {
		int adminCategory = createCategory(admin, "Reference Works");
		int librarianCategory = createCategory(librarian, "Java Programming");

		for (Users roleUser : List.of(admin, librarian, member)) {
			mockMvc.perform(get("/categories").header("Authorization", bearer(roleUser)))
					.andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
			mockMvc.perform(get("/category/{id}", adminCategory).header("Authorization", bearer(roleUser)))
					.andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Reference Works"));
		}
		mockMvc.perform(get("/category/{id}", Integer.MAX_VALUE).header("Authorization", bearer(member)))
				.andExpect(status().isNotFound());
		long categoryCountBeforeSearch = categoryRepository.count();
		mockMvc.perform(get("/category/search").param("name", "REFERENCE")
				.header("Authorization", bearer(member)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
		mockMvc.perform(get("/category/search").param("name", "absent")
				.header("Authorization", bearer(admin)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
		mockMvc.perform(get("/category/search").param("name", "")
				.header("Authorization", bearer(librarian)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
		assertEquals(categoryCountBeforeSearch, categoryRepository.count());

		mockMvc.perform(post("/category").header("Authorization", bearer(member))
				.contentType(MediaType.APPLICATION_JSON).content(categoryJson("Denied")))
			.andExpect(status().isForbidden());
		mockMvc.perform(put("/category/{id}", adminCategory).header("Authorization", bearer(member))
				.contentType(MediaType.APPLICATION_JSON).content(categoryJson("Denied")))
			.andExpect(status().isForbidden());
		mockMvc.perform(delete("/category/{id}", librarianCategory).header("Authorization", bearer(member)))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/category/{id}", adminCategory).header("Authorization", bearer(admin))
				.contentType(MediaType.APPLICATION_JSON).content(categoryJson("Admin Updated")))
			.andExpect(status().isOk());
		mockMvc.perform(put("/category/{id}", librarianCategory).header("Authorization", bearer(librarian))
				.contentType(MediaType.APPLICATION_JSON).content(categoryJson("Librarian Updated")))
			.andExpect(status().isOk());
		assertEquals("Admin Updated", categoryRepository.findById(adminCategory).orElseThrow().getName());
		assertEquals("Librarian Updated", categoryRepository.findById(librarianCategory).orElseThrow().getName());
		mockMvc.perform(delete("/category/{id}", adminCategory).header("Authorization", bearer(admin)))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/category/{id}", librarianCategory).header("Authorization", bearer(librarian)))
				.andExpect(status().isOk());
		assertFalse(categoryRepository.existsById(adminCategory));
		assertFalse(categoryRepository.existsById(librarianCategory));
	}

	@Test
	void dashboardCountsRecentBooksAndEmptyCatalogAreCorrectForEachRole() throws Exception {
		mockMvc.perform(get("/dashboard")).andExpect(status().isUnauthorized());
		for (Users roleUser : List.of(admin, librarian, member)) {
			mockMvc.perform(get("/dashboard").header("Authorization", bearer(roleUser)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.users").value(3))
					.andExpect(jsonPath("$.books").value(0))
					.andExpect(jsonPath("$.authors").value(0))
					.andExpect(jsonPath("$.categories").value(0))
					.andExpect(jsonPath("$.recentBooks").isArray())
					.andExpect(jsonPath("$.recentBooks.length()").value(0));
		}

		int older = createBook(admin, "Older Dashboard Book");
		int newer = createBook(librarian, "Newer Dashboard Book");
		assertTrue(newer > older);
		mockMvc.perform(get("/dashboard").header("Authorization", bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.books").value(2))
				.andExpect(jsonPath("$.recentBooks.length()").value(2))
				.andExpect(jsonPath("$.recentBooks[0].name").value("Newer Dashboard Book"))
				.andExpect(jsonPath("$.recentBooks[1].name").value("Older Dashboard Book"));
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

	private int createBook(Users user, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/book").header("Authorization", bearer(user))
				.contentType(MediaType.APPLICATION_JSON).content(bookJson(name)))
			.andExpect(status().isCreated()).andReturn();
		return idFromResponse(result.getResponse().getContentAsString());
	}

	private int createAuthor(Users user, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/author").header("Authorization", bearer(user))
				.contentType(MediaType.APPLICATION_JSON).content(authorJson(name)))
			.andExpect(status().isCreated()).andReturn();
		return idFromResponse(result.getResponse().getContentAsString());
	}

	private int createCategory(Users user, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/category").header("Authorization", bearer(user))
				.contentType(MediaType.APPLICATION_JSON).content(categoryJson(name)))
			.andExpect(status().isCreated()).andReturn();
		return idFromResponse(result.getResponse().getContentAsString());
	}

	private int idFromResponse(String responseBody) {
		int dataIndex = responseBody.indexOf("\"data\"");
		assertTrue(dataIndex >= 0, "Response did not contain data: " + responseBody);
		Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(responseBody.substring(dataIndex));
		assertTrue(matcher.find(), "Response did not contain data.id: " + responseBody);
		return Integer.parseInt(matcher.group(1));
	}

	private String userJson(String name, String email, long phone, String password, Role role) {
		String passwordField = password == null ? "" : ",\"password\":\"" + password + "\"";
		String roleField = role == null ? "" : ",\"role\":\"" + role.name() + "\"";
		return "{\"name\":\"" + name + "\",\"profile\":{\"email\":\"" + email
				+ "\",\"phone\":" + phone + "}" + passwordField + roleField + "}";
	}

	private String bookJson(String name) {
		return "{\"name\":\"" + name + "\",\"price\":19.95}";
	}

	private String authorJson(String name) {
		return "{\"name\":\"" + name + "\"}";
	}

	private String categoryJson(String name) {
		return "{\"name\":\"" + name + "\"}";
	}
}
