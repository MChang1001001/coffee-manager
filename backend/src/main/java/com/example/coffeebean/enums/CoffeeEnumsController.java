package com.example.coffeebean.enums;

import com.example.coffeebean.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enums")
public class CoffeeEnumsController {

    @GetMapping("/coffee")
    public ApiResponse<CoffeeEnumOptionsResponse> coffee() {
        return ApiResponse.success(CoffeeEnumOptionsResponse.defaults());
    }
}
