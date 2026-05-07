package com.example.coffeebean.coffee;

import com.example.coffeebean.auth.CurrentUser;
import com.example.coffeebean.common.ApiResponse;
import com.example.coffeebean.common.BusinessException;
import com.example.coffeebean.common.ErrorCode;
import com.example.coffeebean.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/coffee-beans")
public class CoffeeBeanController {

    private final CoffeeBeanService coffeeBeanService;

    public CoffeeBeanController(CoffeeBeanService coffeeBeanService) {
        this.coffeeBeanService = coffeeBeanService;
    }

    @PostMapping
    public ApiResponse<CoffeeBeanIdResponse> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CoffeeBeanCreateRequest request) {
        return ApiResponse.success(coffeeBeanService.create(requireUserId(currentUser), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Positive(message = "咖啡豆ID必须大于0") @PathVariable Long id,
            @Valid @RequestBody CoffeeBeanUpdateRequest request) {
        return ApiResponse.success(coffeeBeanService.update(requireUserId(currentUser), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Positive(message = "咖啡豆ID必须大于0") @PathVariable Long id) {
        return ApiResponse.success(coffeeBeanService.delete(requireUserId(currentUser), id));
    }

    @GetMapping("/{id}")
    public ApiResponse<CoffeeBeanDetailResponse> detail(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Positive(message = "咖啡豆ID必须大于0") @PathVariable Long id) {
        return ApiResponse.success(coffeeBeanService.getDetail(requireUserId(currentUser), id));
    }

    @GetMapping
    public ApiResponse<PageResponse<CoffeeBeanListItemResponse>> list(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @ModelAttribute CoffeeBeanListQuery query) {
        return ApiResponse.success(coffeeBeanService.list(requireUserId(currentUser), query));
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser.id();
    }
}
