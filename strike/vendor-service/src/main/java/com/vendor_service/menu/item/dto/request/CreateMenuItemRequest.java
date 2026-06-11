package com.vendor_service.menu.item.dto.request;

import com.vendor_service.menu.enums.ItemAvailabilityStatus;
import com.vendor_service.menu.enums.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMenuItemRequest {

    @NotBlank(message = "Item name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    private BigDecimal price;

    private String imageUrl;

    private ItemType itemType;

    private ItemAvailabilityStatus availabilityStatus;

    @NotNull(message = "Category ID is required")
    private Long categoryId;
}