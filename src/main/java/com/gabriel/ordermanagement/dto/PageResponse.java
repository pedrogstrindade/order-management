package com.gabriel.ordermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Paginated API response")
public record PageResponse<T>(

        @Schema(
                description = "Resources contained in the current page"
        )
        List<T> content,

        @Schema(
                description = "Zero-based page number",
                example = "0",
                minimum = "0"
        )
        int page,

        @Schema(
                description = "Maximum number of elements per page",
                example = "20",
                minimum = "1"
        )
        int size,

        @Schema(
                description = "Total number of matching records",
                example = "35"
        )
        long totalElements,

        @Schema(
                description = "Total number of pages",
                example = "2"
        )
        int totalPages,

        @Schema(
                description = "Whether this is the first page",
                example = "true"
        )
        boolean first,

        @Schema(
                description = "Whether this is the last page",
                example = "false"
        )
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> result) {
        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }
}