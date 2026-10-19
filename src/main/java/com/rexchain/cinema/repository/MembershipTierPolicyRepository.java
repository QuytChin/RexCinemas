package com.rexchain.cinema.repository;
import com.rexchain.cinema.entity.MembershipTier;
import com.rexchain.cinema.entity.MembershipTierPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface MembershipTierPolicyRepository extends JpaRepository<MembershipTierPolicy, Long> {
    List<MembershipTierPolicy> findAllByOrderByQualifyingThresholdAsc();
    Optional<MembershipTierPolicy> findByTier(MembershipTier tier);
}
