package com.tacticaldistrict.command.hierarchy.dto;

import java.util.List;

public record FocusTreeResponse(
        TreeMode mode,
        TreeNodeDto selectedNode,
        List<TreeNodeDto> parentPath,
        List<TreeNodeDto> siblings,
        List<TreeNodeDto> children
) {
}
