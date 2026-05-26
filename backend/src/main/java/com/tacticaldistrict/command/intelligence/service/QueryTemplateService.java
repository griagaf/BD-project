package com.tacticaldistrict.command.intelligence.service;

import com.tacticaldistrict.command.intelligence.dto.QueryTemplateMetadataDto;
import com.tacticaldistrict.command.intelligence.model.QueryTemplate;
import com.tacticaldistrict.command.intelligence.registry.QueryTemplateRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueryTemplateService {

    private final QueryTemplateRegistry registry;

    public List<QueryTemplateMetadataDto> templates() {
        return registry.templates();
    }

    public QueryTemplate template(String code) {
        return registry.get(code);
    }
}
