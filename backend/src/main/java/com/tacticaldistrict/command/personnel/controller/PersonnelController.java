package com.tacticaldistrict.command.personnel.controller;

import com.tacticaldistrict.command.common.dto.PageResponse;
import com.tacticaldistrict.command.personnel.dto.ChainOfCommandNodeResponse;
import com.tacticaldistrict.command.personnel.dto.CreatePersonnelRequest;
import com.tacticaldistrict.command.personnel.dto.PersonnelFilter;
import com.tacticaldistrict.command.personnel.dto.PersonnelProfileResponse;
import com.tacticaldistrict.command.personnel.dto.PersonnelResponse;
import com.tacticaldistrict.command.personnel.dto.RankResponse;
import com.tacticaldistrict.command.personnel.dto.SpecialtyResponse;
import com.tacticaldistrict.command.personnel.dto.UpdatePersonnelRequest;
import com.tacticaldistrict.command.personnel.service.PersonnelService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/personnel")
@RequiredArgsConstructor
public class PersonnelController {

    private final PersonnelService personnelService;

    @GetMapping
    public PageResponse<PersonnelResponse> search(
            @ParameterObject PersonnelFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "lastName") Pageable pageable
    ) {
        return personnelService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'PERSONNEL', 'READ')")
    public PersonnelResponse findById(@PathVariable Long id) {
        return personnelService.findById(id);
    }

    @GetMapping("/{id}/profile")
    @PreAuthorize("hasPermission(#id, 'PERSONNEL', 'READ')")
    public PersonnelProfileResponse profile(@PathVariable Long id) {
        return personnelService.profile(id);
    }

    @GetMapping("/{id}/chain-of-command")
    @PreAuthorize("hasPermission(#id, 'PERSONNEL', 'READ')")
    public List<ChainOfCommandNodeResponse> chainOfCommand(@PathVariable Long id) {
        return personnelService.chainOfCommand(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonnelResponse create(@Valid @RequestBody CreatePersonnelRequest request) {
        return personnelService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'PERSONNEL', 'UPDATE')")
    public PersonnelResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePersonnelRequest request
    ) {
        return personnelService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'PERSONNEL', 'DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        personnelService.delete(id);
    }

    @GetMapping("/ranks")
    public List<RankResponse> ranks() {
        return personnelService.ranks();
    }

    @GetMapping("/specialties")
    public List<SpecialtyResponse> specialties() {
        return personnelService.specialties();
    }
}
