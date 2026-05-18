package com.tacticaldistrict.command.intelligence.service;

import com.tacticaldistrict.command.intelligence.dto.QueryTemplateMetadataDto;
import com.tacticaldistrict.command.intelligence.model.QueryTemplate;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class QueryTemplateService {

    public List<QueryTemplateMetadataDto> templates() {
        return Arrays.stream(QueryTemplate.values())
                .map(QueryTemplate::metadata)
                .toList();
    }

    public QueryTemplate template(String code) {
        return QueryTemplate.valueOf(code);
    }
}
