package com.vendor_service.menu.item.controller;

import com.vendor_service.menu.item.dto.response.MenuItemResponse;
import com.vendor_service.menu.item.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/menu-items")
@RequiredArgsConstructor
public class InternalMenuItemController {

    private final MenuItemService menuItemService;

    @GetMapping("/store/{storeId}")
    public List<MenuItemResponse> getItemsByStore(@PathVariable Long storeId) {
        return menuItemService.getItemsByStore(storeId);
    }
}