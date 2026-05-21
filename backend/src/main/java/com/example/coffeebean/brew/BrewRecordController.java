package com.example.coffeebean.brew;

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
public class BrewRecordController {

    private final BrewRecordService brewRecordService;

    public BrewRecordController(BrewRecordService brewRecordService) {
        this.brewRecordService = brewRecordService;
    }

    @PostMapping("/coffee-beans/{coffeeBeanId}/brew-records")
    public ApiResponse<BrewRecordIdResponse> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Positive(message = "coffeeBeanId must be greater than 0") @PathVariable Long coffeeBeanId,
            @Valid @RequestBody BrewRecordCreateRequest request) {
        return ApiResponse.success(brewRecordService.create(requireUserId(currentUser), coffeeBeanId, request));
    }

    @GetMapping("/coffee-beans/{coffeeBeanId}/brew-records")
    public ApiResponse<PageResponse<BrewRecordResponse>> listByCoffeeBean(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Positive(message = "coffeeBeanId must be greater than 0") @PathVariable Long coffeeBeanId,
            @Valid @ModelAttribute BrewRecordListQuery query) {
        return ApiResponse.success(brewRecordService.listByCoffeeBean(
                requireUserId(currentUser),
                coffeeBeanId,
                query));
    }

    @GetMapping("/brew-records/{id}")
    public ApiResponse<BrewRecordResponse> detail(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Positive(message = "brewRecordId must be greater than 0") @PathVariable Long id) {
        return ApiResponse.success(brewRecordService.getDetail(requireUserId(currentUser), id));
    }

    @PutMapping("/brew-records/{id}")
    public ApiResponse<Boolean> update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Positive(message = "brewRecordId must be greater than 0") @PathVariable Long id,
            @Valid @RequestBody BrewRecordUpdateRequest request) {
        return ApiResponse.success(brewRecordService.update(requireUserId(currentUser), id, request));
    }

    @DeleteMapping("/brew-records/{id}")
    public ApiResponse<Boolean> delete(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Positive(message = "brewRecordId must be greater than 0") @PathVariable Long id) {
        return ApiResponse.success(brewRecordService.delete(requireUserId(currentUser), id));
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser.id();
    }
}
