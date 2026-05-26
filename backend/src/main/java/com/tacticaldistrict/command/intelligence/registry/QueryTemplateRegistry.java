package com.tacticaldistrict.command.intelligence.registry;

import com.tacticaldistrict.command.intelligence.dto.QueryTemplateMetadataDto;
import com.tacticaldistrict.command.intelligence.model.QueryTemplate;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QueryTemplateRegistry {

    public List<QueryTemplateMetadataDto> templates() {
        return Arrays.stream(QueryTemplate.values())
                .map(QueryTemplate::metadata)
                .toList();
    }

    public QueryTemplate get(String code) {
        return QueryTemplate.valueOf(code);
    }
}
