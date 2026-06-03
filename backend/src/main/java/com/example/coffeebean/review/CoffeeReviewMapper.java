package com.example.coffeebean.review;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CoffeeReviewMapper extends BaseMapper<CoffeeReview> {

    @Select("""
            <script>
            SELECT
                coffee_bean_id AS coffeeBeanId,
                ROUND(AVG(overall_rating), 1) AS overallRating,
                COUNT(*) AS reviewCount
            FROM coffee_reviews
            WHERE user_id = #{userId}
                AND deleted = 0
                AND coffee_bean_id IN
                <foreach collection="coffeeBeanIds" item="coffeeBeanId" open="(" separator="," close=")">
                    #{coffeeBeanId}
                </foreach>
            GROUP BY coffee_bean_id
            </script>
            """)
    List<CoffeeReviewSummary> selectSummaries(
            @Param("userId") Long userId,
            @Param("coffeeBeanIds") List<Long> coffeeBeanIds);
}
