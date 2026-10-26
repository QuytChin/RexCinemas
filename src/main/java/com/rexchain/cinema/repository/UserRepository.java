package com.rexchain.cinema.repository;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.entity.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User,Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<User>{
    @Query("select coalesce(sum(u.loyaltyPoints),0) from User u")
    long sumAvailablePoints();
    @Query("select coalesce(sum(u.lifetimePoints),0) from User u")
    long sumLifetimePoints();
    @Query("select coalesce(sum(u.redeemedPoints),0) from User u")
    long sumRedeemedPoints();
    Optional<User> findByEmailIgnoreCase(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmailIgnoreCaseForUpdate(@Param("email") String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByPhone(String phone);
    boolean existsByNationalId(String nationalId);
    List<User> findByRole(Role role);
    long countByCinemaIdAndRoleAndEnabledTrue(Long cinemaId, Role role);
    List<User> findByCinemaIdAndRole(Long cinemaId, Role role);
    List<User> findByCinemaId(Long cinemaId);

    @Query("select u from User u where lower(u.email) = lower(:identifier) or lower(u.username) = lower(:identifier) or u.phone = :identifier")
    List<User> findAllByLoginIdentifier(@Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
