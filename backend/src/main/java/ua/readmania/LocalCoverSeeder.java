package ua.readmania;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.readmania.domain.catalog.Book;
import ua.readmania.domain.catalog.BookCover;
import ua.readmania.domain.catalog.BookCoverRepository;
import ua.readmania.domain.catalog.BookRepository;

import java.io.InputStream;
import java.util.Optional;

@Component
@Profile({"local", "dev", "default"})
public class LocalCoverSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalCoverSeeder.class);

    private final BookRepository bookRepository;
    private final BookCoverRepository bookCoverRepository;

    public LocalCoverSeeder(BookRepository bookRepository, BookCoverRepository bookCoverRepository) {
        this.bookRepository = bookRepository;
        this.bookCoverRepository = bookCoverRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:seed-covers/*.*");

            if (resources.length == 0) {
                resources = resolver.getResources("classpath*:seed-covers/*");
            }

            if (resources.length == 0) {
                log.info("No seed cover images found in classpath*:seed-covers/");
                return;
            }

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;

                int dotIndex = filename.lastIndexOf('.');
                if (dotIndex <= 0) continue;

                String isbn = filename.substring(0, dotIndex);
                String extension = filename.substring(dotIndex + 1).toLowerCase();
                String contentType = switch (extension) {
                    case "png" -> "image/png";
                    case "webp" -> "image/webp";
                    case "gif" -> "image/gif";
                    default -> "image/jpeg";
                };

                Optional<Book> bookOpt = bookRepository.findByIsbn(isbn);
                if (bookOpt.isPresent()) {
                    Book book = bookOpt.get();
                    if (!bookCoverRepository.existsById(book.getId())) {
                        try (InputStream is = resource.getInputStream()) {
                            byte[] data = is.readAllBytes();
                            BookCover cover = new BookCover(book, contentType, data);
                            bookCoverRepository.save(cover);
                            log.info("Seeded cover for book '{}' (ISBN: {})", book.getTitle(), isbn);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to seed book covers", e);
        }
    }
}
