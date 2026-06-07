package sn.immosn.backend.shared.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

import org.springframework.data.domain.Page;

public record PagedResponse <T>(
    List<T> data,
    long totalElements,
    int totalPages,
    int currentPage,
    int pageSize,
    @JsonProperty("isFirst") boolean isFirst,
    @JsonProperty("isLast") boolean isLast
) {
    public static <T> PagedResponse<T> fromPage(Page<T> page){
        return new PagedResponse<>(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize(),
            page.isFirst(),
            page.isLast()
        );
    }
}
