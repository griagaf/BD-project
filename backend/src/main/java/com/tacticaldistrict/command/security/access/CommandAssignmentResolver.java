package com.tacticaldistrict.command.security.access;

import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.entity.CommandAssignmentEntity;
import com.tacticaldistrict.command.user.repository.CommandAssignmentRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommandAssignmentResolver {

    private final CommandAssignmentRepository commandAssignmentRepository;

    @Transactional(readOnly = true)
    public List<CommandAssignmentScope> resolveActiveAssignments(Long soldierId) {
        if (soldierId == null) {
            return List.of();
        }

        return commandAssignmentRepository.findActiveBySoldierId(soldierId, LocalDate.now())
                .stream()
                .map(this::toScope)
                .toList();
    }

    private CommandAssignmentScope toScope(CommandAssignmentEntity entity) {
        return new CommandAssignmentScope(
                entity.getId(),
                entity.getSoldierId(),
                ObjectType.from(entity.getObjectType()),
                entity.getObjectId(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.isPrimary()
        );
    }
}
