package com.pentagon.LibraryManagement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.pentagon.LibraryManagement.DAO.UsersDAO;
import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.Repository.AuthorRepository;
import com.pentagon.LibraryManagement.Repository.BookRepository;
import com.pentagon.LibraryManagement.Repository.CategoryRepository;
import com.pentagon.LibraryManagement.Repository.UserRepository;
import com.pentagon.LibraryManagement.Service.AuthorService;
import com.pentagon.LibraryManagement.Service.BookService;
import com.pentagon.LibraryManagement.Service.CategoryService;
import com.pentagon.LibraryManagement.Service.ProfileService;
import com.pentagon.LibraryManagement.Service.UsersService;
import com.pentagon.LibraryManagement.dto.BookCreateRequest;
import com.pentagon.LibraryManagement.entity.Author;
import com.pentagon.LibraryManagement.entity.Book;
import com.pentagon.LibraryManagement.entity.Category;
import com.pentagon.LibraryManagement.entity.Profile;
import com.pentagon.LibraryManagement.entity.Role;
import com.pentagon.LibraryManagement.entity.Users;
import com.pentagon.LibraryManagement.security.JwtUtil;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityVerificationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtUtil jwtUtil;

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Value("${app.cors.allowed-origin}")
	private String allowedOrigin;

	@MockitoBean
	private BookService bookService;

	@MockitoBean
	private UsersService usersService;

	@MockitoBean
	private AuthorService authorService;

	@MockitoBean
	private CategoryService categoryService;

	@MockitoBean
	private ProfileService profileService;

	@MockitoBean
	private UsersDAO usersDAO;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private BookRepository bookRepository;

	@MockitoBean
	private AuthorRepository authorRepository;

	@MockitoBean
	private CategoryRepository categoryRepository;

	@Test
	void unauthenticatedRequestToProtectedEndpointReturns401() throws Exception {
		mockMvc.perform(get("/books"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void corsAllowsOnlyTheConfiguredFrontendOrigin() throws Exception {
		mockMvc.perform(options("/books")
				.header("Origin", allowedOrigin)
				.header("Access-Control-Request-Method", "GET"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", allowedOrigin));

		mockMvc.perform(options("/books")
				.header("Origin", "https://untrusted.example")
				.header("Access-Control-Request-Method", "GET"))
			.andExpect(status().isForbidden());
	}

	@Test
	void unauthenticatedRequestToAdminEndpointReturns401() throws Exception {
		mockMvc.perform(post("/user")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Reader\",\"password\":\"secret123\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void memberRequestToAdminEndpointReturns403() throws Exception {
		String token = tokenFor("member@example.com", Role.MEMBER);

		mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/user")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Reader\",\"password\":\"secret123\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void librarianRequestToAdminEndpointReturns403() throws Exception {
		String token = tokenFor("librarian@example.com", Role.LIBRARIAN);

		mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/user")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Reader\",\"password\":\"secret123\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanAccessAdminOnlyEndpoints() throws Exception {
		String token = tokenFor("admin@example.com", Role.ADMIN);
		when(usersService.getAllUser()).thenReturn(usersOk());
		when(usersService.addUser(any(Users.class))).thenReturn(userCreated());

		mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
		mockMvc.perform(post("/user")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Reader\",\"password\":\"secret123\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void onlyAdminCanUpdateOrDeleteUsers() throws Exception {
		when(usersService.updateUser(any(Users.class))).thenReturn(userUpdated());
		when(usersService.deleteUser(7)).thenReturn(userDeleted());

		for (Role role : List.of(Role.MEMBER, Role.LIBRARIAN)) {
			String token = tokenFor(role.name().toLowerCase() + "@example.com", role);
			mockMvc.perform(put("/user/7")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"Updated\"}"))
					.andExpect(status().isForbidden());
			mockMvc.perform(delete("/user/7").header("Authorization", "Bearer " + token))
					.andExpect(status().isForbidden());
		}

		String adminToken = tokenFor("admin@example.com", Role.ADMIN);
		mockMvc.perform(put("/user/7")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Updated\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/user/7").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
	}

	@Test
	void memberCanAccessPermittedCatalogGets() throws Exception {
		String token = tokenFor("member@example.com", Role.MEMBER);
		stubCatalogReads();

		mockMvc.perform(get("/books").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
		mockMvc.perform(get("/authors").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
		mockMvc.perform(get("/categories").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void librarianCanAccessPermittedCatalogGets() throws Exception {
		String token = tokenFor("librarian@example.com", Role.LIBRARIAN);
		stubCatalogReads();

		mockMvc.perform(get("/books").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
		mockMvc.perform(get("/authors").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
		mockMvc.perform(get("/categories").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void memberCannotCreateBooks() throws Exception {
		String token = tokenFor("member@example.com", Role.MEMBER);

		mockMvc.perform(post("/book")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New Book\",\"price\":12.5}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void memberCannotUpdateBooks() throws Exception {
		String token = tokenFor("member@example.com", Role.MEMBER);

		mockMvc.perform(put("/book/1")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Updated Book\",\"price\":15}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void memberCannotDeleteBooks() throws Exception {
		String token = tokenFor("member@example.com", Role.MEMBER);

		mockMvc.perform(delete("/book/1").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	@Test
	void librarianCanCreateUpdateAndDeleteBooks() throws Exception {
		String token = tokenFor("librarian@example.com", Role.LIBRARIAN);
		when(bookService.addBook(any(BookCreateRequest.class))).thenReturn(bookCreated());
		when(bookService.updateBook(anyInt(), any(Book.class))).thenReturn(bookOk());
		when(bookService.deleteBook(anyInt())).thenReturn(bookOk());

		mockMvc.perform(post("/book")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New Book\",\"price\":12.5}"))
				.andExpect(status().isCreated());
		mockMvc.perform(put("/book/1")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Updated Book\",\"price\":15}"))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/book/1").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void loginIsPublicAndInvalidCredentialsDoNotAuthenticate() throws Exception {
		Users user = account("member@example.com", Role.MEMBER);
		user.setId(4);
		user.setName("Member");
		user.setPassword("$2a$10$existinghashshouldnotbereturned");
		when(usersService.login("member@example.com", "secret123")).thenReturn(user);
		when(usersService.login("member@example.com", "wrong-password")).thenReturn(null);

		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"member@example.com\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Login Successful"))
				.andExpect(jsonPath("$.data.token").isNotEmpty())
				.andExpect(jsonPath("$.data.password").doesNotExist())
				.andExpect(jsonPath("$.data.user.password").doesNotExist())
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("$2a$10$existinghashshouldnotbereturned"))));

		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"member@example.com\",\"password\":\"wrong-password\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid Email or Password"));
	}

	@Test
	void postUserIsAdminOnly() throws Exception {
		when(usersService.addUser(any(Users.class))).thenReturn(userCreated());

		mockMvc.perform(post("/user")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Reader\",\"password\":\"secret123\"}"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/user")
				.header("Authorization", "Bearer " + tokenFor("member@example.com", Role.MEMBER))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Reader\",\"password\":\"secret123\"}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/user")
				.header("Authorization", "Bearer " + tokenFor("librarian@example.com", Role.LIBRARIAN))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Reader\",\"password\":\"secret123\"}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/user")
				.header("Authorization", "Bearer " + tokenFor("admin@example.com", Role.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Reader\",\"password\":\"secret123\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void legacyBorrowEndpointRequiresAuthenticationAndThenReturnsGone() throws Exception {
		when(bookService.borrow(1, 1)).thenReturn(bookGone());

		mockMvc.perform(get("/bookbr").param("b_id", "1").param("u_id", "1"))
				.andExpect(status().isUnauthorized());

		for (Role role : List.of(Role.MEMBER, Role.LIBRARIAN, Role.ADMIN)) {
			mockMvc.perform(get("/bookbr")
					.param("b_id", "1")
					.param("u_id", "1")
					.header("Authorization", "Bearer " + tokenFor(role.name().toLowerCase() + "@example.com", role)))
					.andExpect(status().isGone());
		}
	}

	@Test
	void dashboardRequiresAuthenticationAndAllowsEachRole() throws Exception {
		mockMvc.perform(get("/dashboard"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/dashboard")
				.header("Authorization", "Bearer " + tokenFor("member@example.com", Role.MEMBER)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/dashboard")
				.header("Authorization", "Bearer " + tokenFor("librarian@example.com", Role.LIBRARIAN)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/dashboard")
				.header("Authorization", "Bearer " + tokenFor("admin@example.com", Role.ADMIN)))
				.andExpect(status().isOk());
	}

	@Test
	void malformedExpiredAndBadSignatureTokensAreRejected() throws Exception {
		mockMvc.perform(get("/books").header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isUnauthorized());

		SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
		String expired = Jwts.builder()
				.subject("member@example.com")
				.claim("role", "MEMBER")
				.issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
				.expiration(new Date(System.currentTimeMillis() - 3_600_000))
				.signWith(key)
				.compact();
		mockMvc.perform(get("/books").header("Authorization", "Bearer " + expired))
				.andExpect(status().isUnauthorized());

		SecretKey otherKey = Keys.hmacShaKeyFor("differentsecretkeydifferentsecret12".getBytes());
		String wrongSignature = Jwts.builder()
				.subject("member@example.com")
				.claim("role", "MEMBER")
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 3_600_000))
				.signWith(otherKey)
				.compact();
		mockMvc.perform(get("/books").header("Authorization", "Bearer " + wrongSignature))
				.andExpect(status().isUnauthorized());

		String guestRole = Jwts.builder()
				.subject("member@example.com")
				.claim("role", "GUEST")
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 3_600_000))
				.signWith(key)
				.compact();
		when(usersDAO.findByEmail("member@example.com")).thenReturn(account("member@example.com", Role.MEMBER));
		mockMvc.perform(get("/books").header("Authorization", "Bearer " + guestRole))
				.andExpect(status().isUnauthorized());
	}

	private void stubCatalogReads() {
		when(bookService.getAllBooks()).thenReturn(bookListOk());
		when(authorService.getAllAuthor()).thenReturn(authorListOk());
		when(categoryService.getAll()).thenReturn(categoryListOk());
	}

	private String tokenFor(String email, Role role) {
		when(usersDAO.findByEmail(email)).thenReturn(account(email, role));
		return jwtUtil.generateToken(email, role.name());
	}

	private Users account(String email, Role role) {
		Users user = new Users();
		user.setName(email);
		user.setRole(role);
		user.setPassword("$2a$10$existinghashshouldnotbereturned");
		Profile profile = new Profile();
		profile.setEmail(email);
		user.setProfile(profile);
		return user;
	}

	private ResponseEntity<ResponseStructure<List<Users>>> usersOk() {
		ResponseStructure<List<Users>> body = new ResponseStructure<>();
		body.setStatuscode(HttpStatus.OK.value());
		body.setMessage("All Users Fetched Successfully");
		body.setData(List.of());
		return ResponseEntity.ok(body);
	}

	private ResponseEntity<ResponseStructure<Users>> userCreated() {
		ResponseStructure<Users> body = new ResponseStructure<>();
		body.setStatuscode(HttpStatus.CREATED.value());
		body.setMessage("User Added Successfully");
		Users created = new Users();
		created.setId(9);
		created.setName("Reader");
		body.setData(created);
		return new ResponseEntity<>(body, HttpStatus.CREATED);
	}

	private ResponseEntity<ResponseStructure<Users>> userUpdated() {
		ResponseStructure<Users> body = new ResponseStructure<>();
		body.setStatuscode(HttpStatus.OK.value());
		body.setMessage("User Updated Successfully");
		body.setData(new Users());
		return ResponseEntity.ok(body);
	}

	private ResponseEntity<ResponseStructure<String>> userDeleted() {
		ResponseStructure<String> body = new ResponseStructure<>();
		body.setStatuscode(HttpStatus.OK.value());
		body.setMessage("User Deleted Successfully");
		body.setData("User Deleted");
		return ResponseEntity.ok(body);
	}

	private ResponseEntity<ResponseStructure<Book>> bookOk() {
		ResponseStructure<Book> body = new ResponseStructure<>();
		body.setStatuscode(HttpStatus.OK.value());
		body.setMessage("ok");
		body.setData(new Book());
		return ResponseEntity.ok(body);
	}

	private ResponseEntity<ResponseStructure<Book>> bookGone() {
		ResponseStructure<Book> body = new ResponseStructure<>();
		body.setStatuscode(HttpStatus.GONE.value());
		body.setMessage("Direct borrowing is disabled. Submit a request via POST /borrow-requests");
		body.setData(null);
		return new ResponseEntity<>(body, HttpStatus.GONE);
	}

	private ResponseEntity<ResponseStructure<Book>> bookCreated() {
		ResponseStructure<Book> body = new ResponseStructure<>();
		body.setStatuscode(HttpStatus.CREATED.value());
		body.setMessage("Book added...!");
		body.setData(new Book());
		return new ResponseEntity<>(body, HttpStatus.CREATED);
	}

	private ResponseEntity<ResponseStructure<List<Book>>> bookListOk() {
		ResponseStructure<List<Book>> body = new ResponseStructure<>();
		body.setStatuscode(HttpStatus.OK.value());
		body.setMessage("book fetched...!");
		body.setData(List.of());
		return ResponseEntity.ok(body);
	}

	private ResponseEntity<ResponseStructure<List<Author>>> authorListOk() {
		ResponseStructure<List<Author>> body = new ResponseStructure<>();
		body.setStatuscode(HttpStatus.OK.value());
		body.setMessage("author fetched...!");
		body.setData(List.of());
		return ResponseEntity.ok(body);
	}

	private ResponseEntity<ResponseStructure<List<Category>>> categoryListOk() {
		ResponseStructure<List<Category>> body = new ResponseStructure<>();
		body.setStatuscode(HttpStatus.OK.value());
		body.setMessage("Categories fetched...!");
		body.setData(List.of());
		return ResponseEntity.ok(body);
	}
}
