package com.example.coffeebean.brew;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.coffeebean.coffee.CoffeeBean;
import com.example.coffeebean.coffee.CoffeeBeanMapper;
import com.example.coffeebean.common.BusinessException;
import com.example.coffeebean.common.ErrorCode;
import com.example.coffeebean.common.PageResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BrewRecordServiceImpl extends ServiceImpl<BrewRecordMapper, BrewRecord>
        implements BrewRecordService {

    private final CoffeeBeanMapper coffeeBeanMapper;

    public BrewRecordServiceImpl(CoffeeBeanMapper coffeeBeanMapper) {
        this.coffeeBeanMapper = coffeeBeanMapper;
    }

    @Override
    public BrewRecordIdResponse create(Long userId, Long coffeeBeanId, BrewRecordCreateRequest request) {
        ensureOwnedCoffeeBean(userId, coffeeBeanId);

        BrewRecord brewRecord = new BrewRecord();
        brewRecord.setUserId(userId);
        brewRecord.setCoffeeBeanId(coffeeBeanId);
        fillCreateFields(brewRecord, request);
        brewRecord.setDeleted(0);
        save(brewRecord);
        return new BrewRecordIdResponse(brewRecord.getId());
    }

    @Override
    public PageResponse<BrewRecordResponse> listByCoffeeBean(
            Long userId,
            Long coffeeBeanId,
            BrewRecordListQuery query) {
        ensureOwnedCoffeeBean(userId, coffeeBeanId);

        Page<BrewRecord> pageRequest = Page.of(query.resolvedPage(), query.resolvedPageSize());
        Page<BrewRecord> pageResult = page(pageRequest, new LambdaQueryWrapper<BrewRecord>()
                .eq(BrewRecord::getUserId, userId)
                .eq(BrewRecord::getCoffeeBeanId, coffeeBeanId)
                .orderByDesc(BrewRecord::getCreatedAt)
                .orderByDesc(BrewRecord::getId));
        List<BrewRecordResponse> items = pageResult.getRecords()
                .stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(items, pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
    }

    @Override
    public BrewRecordResponse getDetail(Long userId, Long id) {
        return toResponse(findOwnedBrewRecord(userId, id));
    }

    @Override
    public boolean update(Long userId, Long id, BrewRecordUpdateRequest request) {
        findOwnedBrewRecord(userId, id);

        boolean updated = update(new LambdaUpdateWrapper<BrewRecord>()
                .eq(BrewRecord::getId, id)
                .eq(BrewRecord::getUserId, userId)
                .set(BrewRecord::getBrewMethod, requireText(request.getBrewMethod(), "brewMethod cannot be blank"))
                .set(BrewRecord::getBeanAmountGrams, request.getBeanAmountGrams())
                .set(BrewRecord::getWaterAmountMl, request.getWaterAmountMl())
                .set(BrewRecord::getRatio, normalize(request.getRatio()))
                .set(BrewRecord::getWaterTemperature, request.getWaterTemperature())
                .set(BrewRecord::getGrindSize, normalize(request.getGrindSize()))
                .set(BrewRecord::getBrewTimeSeconds, request.getBrewTimeSeconds())
                .set(BrewRecord::getResultSummary, normalize(request.getResultSummary()))
                .set(BrewRecord::getResultNotes, normalize(request.getResultNotes()))
                .set(BrewRecord::getIsRecommended, toRecommendedValue(request.getIsRecommended())));
        if (!updated) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Brew record not found");
        }
        return true;
    }

    @Override
    public boolean delete(Long userId, Long id) {
        findOwnedBrewRecord(userId, id);

        boolean removed = remove(new LambdaQueryWrapper<BrewRecord>()
                .eq(BrewRecord::getId, id)
                .eq(BrewRecord::getUserId, userId));
        if (!removed) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Brew record not found");
        }
        return true;
    }

    private void ensureOwnedCoffeeBean(Long userId, Long coffeeBeanId) {
        Long count = coffeeBeanMapper.selectCount(new LambdaQueryWrapper<CoffeeBean>()
                .eq(CoffeeBean::getId, coffeeBeanId)
                .eq(CoffeeBean::getUserId, userId));
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Coffee bean not found");
        }
    }

    private BrewRecord findOwnedBrewRecord(Long userId, Long id) {
        BrewRecord brewRecord = getOne(new LambdaQueryWrapper<BrewRecord>()
                .eq(BrewRecord::getId, id)
                .eq(BrewRecord::getUserId, userId)
                .last("LIMIT 1"));
        if (brewRecord == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Brew record not found");
        }
        return brewRecord;
    }

    private void fillCreateFields(BrewRecord brewRecord, BrewRecordCreateRequest request) {
        brewRecord.setBrewMethod(requireText(request.getBrewMethod(), "brewMethod cannot be blank"));
        brewRecord.setBeanAmountGrams(request.getBeanAmountGrams());
        brewRecord.setWaterAmountMl(request.getWaterAmountMl());
        brewRecord.setRatio(normalize(request.getRatio()));
        brewRecord.setWaterTemperature(request.getWaterTemperature());
        brewRecord.setGrindSize(normalize(request.getGrindSize()));
        brewRecord.setBrewTimeSeconds(request.getBrewTimeSeconds());
        brewRecord.setResultSummary(normalize(request.getResultSummary()));
        brewRecord.setResultNotes(normalize(request.getResultNotes()));
        brewRecord.setIsRecommended(toRecommendedValue(request.getIsRecommended()));
    }

    private BrewRecordResponse toResponse(BrewRecord brewRecord) {
        BrewRecordResponse response = new BrewRecordResponse();
        response.setId(brewRecord.getId());
        response.setCoffeeBeanId(brewRecord.getCoffeeBeanId());
        response.setBrewMethod(brewRecord.getBrewMethod());
        response.setBeanAmountGrams(brewRecord.getBeanAmountGrams());
        response.setWaterAmountMl(brewRecord.getWaterAmountMl());
        response.setRatio(brewRecord.getRatio());
        response.setWaterTemperature(brewRecord.getWaterTemperature());
        response.setGrindSize(brewRecord.getGrindSize());
        response.setBrewTimeSeconds(brewRecord.getBrewTimeSeconds());
        response.setResultSummary(brewRecord.getResultSummary());
        response.setResultNotes(brewRecord.getResultNotes());
        response.setIsRecommended(brewRecord.getIsRecommended() != null && brewRecord.getIsRecommended() == 1);
        response.setCreatedAt(brewRecord.getCreatedAt());
        response.setUpdatedAt(brewRecord.getUpdatedAt());
        return response;
    }

    private Integer toRecommendedValue(Boolean isRecommended) {
        return Boolean.TRUE.equals(isRecommended) ? 1 : 0;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String requireText(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
        return normalized;
    }
}
