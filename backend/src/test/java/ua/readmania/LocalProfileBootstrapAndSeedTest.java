package ua.readmania;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookCover;
import ua.readmania.domain.catalog.BookCoverRepository;
import ua.readmania.domain.catalog.BookRepository;
import ua.readmania.domain.user.Role;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;

import javax.sql.DataSource;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(TestcontainersConfiguration.class)
class LocalProfileBootstrapAndSeedTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LocalCoverSeeder localCoverSeeder;

    @Autowired
    private ManagerBootstrapInitializer managerBootstrapInitializer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCoverRepository bookCoverRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (bookRepository.count() == 0 || userRepository.findByEmailIgnoreCase("user@readmania.ua").isEmpty()) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("db/seed/R__dev_seed.sql"));
            populator.execute(dataSource);
        }
        managerBootstrapInitializer.run(null);
        localCoverSeeder.run(null);
    }

    @Test
    @DisplayName("Local profile seeds test manager and buyer accounts with valid password hashes")
    void localProfileUsersExist() {
        Optional<User> managerOpt = userRepository.findByEmailIgnoreCase("manager@readmania.ua");
        assertThat(managerOpt).isPresent();
        User manager = managerOpt.get();
        assertThat(manager.getRole()).isEqualTo(Role.MANAGER);
        assertThat(passwordEncoder.matches("Manager123!", manager.getPasswordHash())).isTrue();

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase("user@readmania.ua");
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(passwordEncoder.matches("User123!", user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("LocalCoverSeeder automatically seeds book covers in local profile")
    void localCoverSeederPopulatesCovers() throws Exception {
        String[] isbns = {
                "978-966-03-7000-1",
                "978-966-03-7000-2",
                "978-966-03-7000-3",
                "978-966-03-7000-4",
                "978-966-03-7000-5"
        };

        for (String isbn : isbns) {
            Optional<Book> bookOpt = bookRepository.findByIsbn(isbn);
            assertThat(bookOpt).isPresent();
            Book book = bookOpt.get();

            Optional<BookCover> coverOpt = bookCoverRepository.findById(book.getId());
            assertThat(coverOpt).isPresent();
            assertThat(coverOpt.get().getData()).isNotEmpty();

            mockMvc.perform(get("/api/catalog/books/{id}", book.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasCover", is(true)))
                    .andExpect(jsonPath("$.coverUrl", is("/api/catalog/books/" + book.getId() + "/cover")));

            mockMvc.perform(get("/api/catalog/books/{id}/cover", book.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG));
        }
    }
}
