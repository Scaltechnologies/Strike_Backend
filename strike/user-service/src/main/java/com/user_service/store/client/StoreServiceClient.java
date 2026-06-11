package com.user_service.store.client;

import com.user_service.common.dto.ApiResponse;
import com.user_service.store.dto.MenuCategoryResponse;
import com.user_service.store.dto.StoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StoreServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.vendor-url}")
    private String vendorServiceUrl;

    public List<StoreResponse> getAllActiveStores() {
        var ref = new ParameterizedTypeReference<ApiResponse<List<StoreResponse>>>() {};
        var response = restTemplate.exchange(
                vendorServiceUrl + "/api/stores", HttpMethod.GET, null, ref);
        return extractList(response.getBody());
    }

    public List<StoreResponse> getNearbyStores(double lat, double lng, double radiusKm) {
        String url = UriComponentsBuilder.fromUriString(vendorServiceUrl + "/api/stores/nearby")
                .queryParam("lat", lat)
                .queryParam("lng", lng)
                .queryParam("radiusKm", radiusKm)
                .toUriString();
        var ref = new ParameterizedTypeReference<ApiResponse<List<StoreResponse>>>() {};
        var response = restTemplate.exchange(url, HttpMethod.GET, null, ref);
        return extractList(response.getBody());
    }

    public List<StoreResponse> searchStores(String query, String category) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(vendorServiceUrl + "/api/stores/search");
        if (query != null && !query.isBlank())    builder.queryParam("q", query);
        if (category != null && !category.isBlank()) builder.queryParam("category", category);
        var ref = new ParameterizedTypeReference<ApiResponse<List<StoreResponse>>>() {};
        var response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, null, ref);
        return extractList(response.getBody());
    }

    public StoreResponse getStoreById(Long storeId) {
        var ref = new ParameterizedTypeReference<ApiResponse<StoreResponse>>() {};
        var response = restTemplate.exchange(
                vendorServiceUrl + "/api/stores/" + storeId, HttpMethod.GET, null, ref);
        ApiResponse<StoreResponse> body = response.getBody();
        return body != null ? body.getData() : null;
    }

    public List<MenuCategoryResponse> getStoreMenu(Long storeId) {
        var ref = new ParameterizedTypeReference<ApiResponse<List<MenuCategoryResponse>>>() {};
        var response = restTemplate.exchange(
                vendorServiceUrl + "/api/stores/" + storeId + "/menu", HttpMethod.GET, null, ref);
        return extractList(response.getBody());
    }

    private <T> List<T> extractList(ApiResponse<List<T>> response) {
        if (response != null && response.getData() != null) return response.getData();
        return Collections.emptyList();
    }
}