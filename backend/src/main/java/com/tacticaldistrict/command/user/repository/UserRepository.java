package com.tacticaldistrict.command.user.repository;

import com.tacticaldistrict.command.user.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    @Query("""
            select u
            from UserEntity u
            where (:search = ''
                   or lower(u.username) like lower(concat('%', :search, '%'))
                   or lower(u.displayName) like lower(concat('%', :search, '%')))
              and (:active is null or u.active = :active)
            """)
    Page<UserEntity> search(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
