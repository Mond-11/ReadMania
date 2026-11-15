package ua.readmania;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(TestcontainersConfiguration.class)
class LocalProfileBootstrapAndSeedTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCoverRepository bookCoverRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        Optional<Book> kobzar = bookRepository.findByIsbn("978-966-03-7000-1");
        assertThat(kobzar).isPresent();

        Optional<BookCover> coverOpt = bookCoverRepository.findById(kobzar.get().getId());
        assertThat(coverOpt).isPresent();
        assertThat(coverOpt.get().getData()).isNotEmpty();

        mockMvc.perform(get("/api/catalog/books/{id}/cover", kobzar.get().getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }
}
