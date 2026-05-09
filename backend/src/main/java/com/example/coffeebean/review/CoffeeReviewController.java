package com.example.coffeebean.review;

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
@RequestMapping("/api")
public class CoffeeReviewController {

    private final CoffeeReviewService coffeeReviewService;

    public CoffeeReviewController(CoffeeReviewService coffeeReviewService) {
        this.coffeeReviewService = coffeeReviewService;
    }

    @PostMapping("/coffee-beans/{coffeeBeanId}/reviews")
    public ApiResponse<CoffeeReviewIdResponse> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Positive(message = "咖啡豆ID必须大于0") @PathVariable Long coffeeBeanId,
            @Valid @RequestBody CoffeeReviewCreateRequest request) {
        return ApiResponse.success(coffeeReviewService.create(requireUserId(currentUser), coffeeBeanId, request));
    }

    @GetMapping("/coffee-beans/{coffeeBeanId}/reviews")
    public ApiResponse<PageResponse<CoffeeReviewResponse>> listByCoffeeBean(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Positive(message = "咖啡豆ID必须大于0") @PathVariable Long coffeeBeanId,
            @Valid @ModelAttribute CoffeeReviewListQuery query) {
        return ApiResponse.success(coffeeReviewService.listByCoffeeBean(
                requireUserId(currentUser),
                coffeeBeanId,
                query));
    }

    @GetMapping("/reviews/{id}")
    public ApiResponse<CoffeeReviewResponse> detail(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Positive(message = "评价ID必须大于0") @PathVariable Long id) {
        return ApiResponse.success(coffeeReviewService.getDetail(requireUserId(currentUser), id));
    }

    @PutMapping("/reviews/{id}")
    public ApiResponse<Boolean> update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Positive(message = "评价ID必须大于0") @PathVariable Long id,
            @Valid @RequestBody CoffeeReviewUpdateRequest request) {
        return ApiResponse.success(coffeeReviewService.update(requireUserId(currentUser), id, request));
    }

    @DeleteMapping("/reviews/{id}")
    public ApiResponse<Boolean> delete(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Positive(message = "评价ID必须大于0") @PathVariable Long id) {
        return ApiResponse.success(coffeeReviewService.delete(requireUserId(currentUser), id));
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser.id();
    }
}
