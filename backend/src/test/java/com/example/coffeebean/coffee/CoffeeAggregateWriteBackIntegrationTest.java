package com.example.coffeebean.coffee;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.coffeebean.brew.BrewRecord;
import com.example.coffeebean.brew.BrewRecordCreateRequest;
import com.example.coffeebean.brew.BrewRecordIdResponse;
import com.example.coffeebean.brew.BrewRecordMapper;
import com.example.coffeebean.brew.BrewRecordService;
import com.example.coffeebean.brew.BrewRecordUpdateRequest;
import com.example.coffeebean.review.CoffeeReview;
import com.example.coffeebean.review.CoffeeReviewCreateRequest;
import com.example.coffeebean.review.CoffeeReviewIdResponse;
import com.example.coffeebean.review.CoffeeReviewMapper;
import com.example.coffeebean.review.CoffeeReviewService;
import com.example.coffeebean.review.CoffeeReviewUpdateRequest;
import com.example.coffeebean.user.User;
import com.example.coffeebean.user.UserMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CoffeeAggregateWriteBackIntegrationTest {

    @Autowired
    private CoffeeBeanService coffeeBeanService;

    @Autowired
    private CoffeeBeanMapper coffeeBeanMapper;

    @Autowired
    private CoffeeReviewService coffeeReviewService;

    @Autowired
    private CoffeeReviewMapper coffeeReviewMapper;

    @Autowired
    private BrewRecordService brewRecordService;

    @Autowired
    private BrewRecordMapper brewRecordMapper;

    @Autowired
    private UserMapper userMapper;

    private Long userId;

    @BeforeEach
    void setUp() {
        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "admin")
                .last("LIMIT 1"));
        assertThat(admin).isNotNull();
        userId = admin.getId();
    }

    @Test
    void refreshesReviewAggregatesAfterCreateUpdateAndDelete() {
        Long coffeeBeanId = createCoffeeBean("review aggregate").id();
        insertOtherUserReview(coffeeBeanId, "5.0");

        Long firstReviewId = createReview(coffeeBeanId, "4.5").id();
        assertCoffeeCache(coffeeBeanId, 1, "4.5", 0);

        Long secondReviewId = createReview(coffeeBeanId, "3.5").id();
        assertCoffeeCache(coffeeBeanId, 2, "4.0", 0);

        updateReview(firstReviewId, "4.0");
        assertCoffeeCache(coffeeBeanId, 2, "3.8", 0);

        assertThat(coffeeReviewService.delete(userId, secondReviewId)).isTrue();
        assertCoffeeCache(coffeeBeanId, 1, "4.0", 0);

        assertThat(coffeeReviewService.delete(userId, firstReviewId)).isTrue();
        assertCoffeeCache(coffeeBeanId, 0, null, 0);
    }

    @Test
    void refreshesBrewAggregatesAfterCreateUpdateAndDelete() {
        Long coffeeBeanId = createCoffeeBean("brew aggregate").id();
        insertOtherUserBrew(coffeeBeanId);

        Long firstBrewId = createBrewRecord(coffeeBeanId, "V60").id();
        assertCoffeeCache(coffeeBeanId, 0, null, 1);

        Long secondBrewId = createBrewRecord(coffeeBeanId, "French Press").id();
        assertCoffeeCache(coffeeBeanId, 0, null, 2);

        updateBrewRecord(firstBrewId, "V60");
        assertCoffeeCache(coffeeBeanId, 0, null, 2);

        assertThat(brewRecordService.delete(userId, secondBrewId)).isTrue();
        assertCoffeeCache(coffeeBeanId, 0, null, 1);

        assertThat(brewRecordService.delete(userId, firstBrewId)).isTrue();
        assertCoffeeCache(coffeeBeanId, 0, null, 0);
    }

    private CoffeeBeanIdResponse createCoffeeBean(String suffix) {
        CoffeeBeanCreateRequest request = new CoffeeBeanCreateRequest();
        request.setName("[TEST] " + suffix);
        request.setOrigin("Ethiopia");
        request.setCurrency("CNY");
        request.setStatus("UNOPENED");
        return coffeeBeanService.create(userId, request);
    }

    private CoffeeReviewIdResponse createReview(Long coffeeBeanId, String overallRating) {
        CoffeeReviewCreateRequest request = new CoffeeReviewCreateRequest();
        request.setOverallRating(new BigDecimal(overallRating));
        request.setContent("[TEST] review");
        return coffeeReviewService.create(userId, coffeeBeanId, request);
    }

    private void updateReview(Long reviewId, String overallRating) {
        CoffeeReviewUpdateRequest request = new CoffeeReviewUpdateRequest();
        request.setOverallRating(new BigDecimal(overallRating));
        request.setContent("[TEST] review update");
        assertThat(coffeeReviewService.update(userId, reviewId, request)).isTrue();
    }

    private BrewRecordIdResponse createBrewRecord(Long coffeeBeanId, String brewMethod) {
        BrewRecordCreateRequest request = new BrewRecordCreateRequest();
        request.setBrewMethod(brewMethod);
        request.setResultSummary("[TEST] brew");
        return brewRecordService.create(userId, coffeeBeanId, request);
    }

    private void updateBrewRecord(Long brewRecordId, String brewMethod) {
        BrewRecordUpdateRequest request = new BrewRecordUpdateRequest();
        request.setBrewMethod(brewMethod);
        request.setResultSummary("[TEST] brew update");
        assertThat(brewRecordService.update(userId, brewRecordId, request)).isTrue();
    }

    private void insertOtherUserReview(Long coffeeBeanId, String overallRating) {
        CoffeeReview review = new CoffeeReview();
        review.setUserId(userId + 1000);
        review.setCoffeeBeanId(coffeeBeanId);
        review.setOverallRating(new BigDecimal(overallRating));
        review.setDeleted(0);
        coffeeReviewMapper.insert(review);
    }

    private void insertOtherUserBrew(Long coffeeBeanId) {
        BrewRecord brewRecord = new BrewRecord();
        brewRecord.setUserId(userId + 1000);
        brewRecord.setCoffeeBeanId(coffeeBeanId);
        brewRecord.setBrewMethod("Other User");
        brewRecord.setDeleted(0);
        brewRecordMapper.insert(brewRecord);
    }

    private void assertCoffeeCache(
            Long coffeeBeanId,
            Integer expectedReviewCount,
            String expectedOverallRating,
            Integer expectedBrewCount) {
        CoffeeBean coffeeBean = coffeeBeanMapper.selectById(coffeeBeanId);

        assertThat(coffeeBean.getReviewCount()).isEqualTo(expectedReviewCount);
        if (expectedOverallRating == null) {
            assertThat(coffeeBean.getOverallRating()).isNull();
        } else {
            assertThat(coffeeBean.getOverallRating()).isEqualByComparingTo(new BigDecimal(expectedOverallRating));
        }
        assertThat(coffeeBean.getBrewCount()).isEqualTo(expectedBrewCount);
    }
}
