package com.vendor_service.menu.category.dto.response;

import com.vendor_service.menu.enums.CategoryStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponse {

    private Long id;

    private String name;

    private String description;

    private String imageUrl;

    private Integer displayOrder;

    private CategoryStatus status;

    private Long storeId;
}