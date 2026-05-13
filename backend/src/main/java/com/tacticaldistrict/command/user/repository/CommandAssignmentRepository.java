package com.tacticaldistrict.command.user.repository;

import com.tacticaldistrict.command.user.entity.CommandAssignmentEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommandAssignmentRepository extends JpaRepository<CommandAssignmentEntity, Long> {

    @Query("""
            select ca
            from CommandAssignmentEntity ca
            where ca.soldierId = :soldierId
              and ca.startsAt <= :today
              and (ca.endsAt is null or ca.endsAt >= :today)
            order by ca.primary desc, ca.startsAt desc
            """)
    List<CommandAssignmentEntity> findActiveBySoldierId(
            @Param("soldierId") Long soldierId,
            @Param("today") LocalDate today
    );
}
