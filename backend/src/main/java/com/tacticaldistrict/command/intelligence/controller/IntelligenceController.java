package com.tacticaldistrict.command.intelligence.controller;

import com.tacticaldistrict.command.intelligence.dto.ExecuteQueryRequest;
import com.tacticaldistrict.command.intelligence.dto.QueryResultDto;
import com.tacticaldistrict.command.intelligence.dto.QueryTemplateMetadataDto;
import com.tacticaldistrict.command.intelligence.model.QueryTemplate;
import com.tacticaldistrict.command.intelligence.service.QueryExecutorService;
import com.tacticaldistrict.command.intelligence.service.QueryTemplateService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/intelligence")
@RequiredArgsConstructor
public class IntelligenceController {

    private final QueryTemplateService queryTemplateService;
    private final QueryExecutorService queryExecutorService;

    @GetMapping("/templates")
    public List<QueryTemplateMetadataDto> templates() {
        return queryTemplateService.templates();
    }

    @PostMapping("/queries/{code}/execute")
    public QueryResultDto execute(
            @PathVariable String code,
            @RequestBody ExecuteQueryRequest request
    ) {
        QueryTemplate template = queryTemplateService.template(code);
        return queryExecutorService.execute(template, request);
    }

    @PostMapping("/queries/{code}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable String code,
            @RequestBody ExecuteQueryRequest request
    ) {
        QueryTemplate template = queryTemplateService.template(code);
        return queryExecutorService.exportCsv(template, request);
    }
}
