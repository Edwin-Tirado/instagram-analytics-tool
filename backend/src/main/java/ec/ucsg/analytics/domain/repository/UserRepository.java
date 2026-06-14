package ec.ucsg.analytics.domain.repository;

import ec.ucsg.analytics.domain.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE AppUser u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.email = :email")
    void incrementFailedAttempts(@Param("email") String email);

    @Modifying
    @Query("UPDATE AppUser u SET u.failedLoginAttempts = 0, u.locked = false, u.lockTime = null WHERE u.email = :email")
    void resetLoginAttempts(@Param("email") String email);
}
