package ua.readmania.domain.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    default Optional<User> findByEmailLower(String email) {
        return findByEmailIgnoreCase(email);
    }

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByStatus(UserStatus status);

    @Query("SELECT u FROM User u WHERE u.status = :status AND (u.lastLoginAt IS NULL OR u.lastLoginAt < :inactiveBefore)")
    List<User> findInactiveUsers(@Param("status") UserStatus status, @Param("inactiveBefore") Instant inactiveBefore);

    @Query("SELECT u FROM User u WHERE u.role = ua.readmania.domain.user.Role.USER AND (" +
           "(u.status = ua.readmania.domain.user.UserStatus.ACTIVE AND COALESCE(u.lastLoginAt, u.createdAt) < :cutoff) OR " +
           "(u.status = ua.readmania.domain.user.UserStatus.BLOCKED AND u.blockedAt < :cutoff)) " +
           "ORDER BY COALESCE(u.lastLoginAt, COALESCE(u.blockedAt, u.createdAt)) ASC")
    List<User> findInactiveUsers(@Param("cutoff") Instant cutoff);
}
