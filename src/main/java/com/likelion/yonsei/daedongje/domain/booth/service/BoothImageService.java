package com.likelion.yonsei.daedongje.domain.booth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.common.exception.CommonErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothImageCreateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothImageResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.BoothImageUpdateRequest;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothImage;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothImageErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothImageRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BoothImageService {

    private final BoothImageRepository boothImageRepository;
    private final BoothRepository boothRepository;

    public BoothImageService(BoothImageRepository boothImageRepository, BoothRepository boothRepository) {
        this.boothImageRepository = boothImageRepository;
        this.boothRepository = boothRepository;
    }

    @Transactional
    public BoothImageResponse create(AdminSessionUser admin, Long boothId, BoothImageCreateRequest request) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

        validateAdminCanAccessBooth(admin, booth);
        validateDisplayOrderAvailable(boothId, request.displayOrder());

        BoothImage boothImage = BoothImage.create(boothId, request.imageUrl(), request.displayOrder());

        try {
            return BoothImageResponse.from(boothImageRepository.save(boothImage));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(BoothImageErrorCode.DUPLICATE_BOOTH_IMAGE_DISPLAY_ORDER);
        }
    }

    public List<BoothImageResponse> getListByBooth(Long boothId) {
        if (!boothRepository.existsById(boothId)) {
            throw new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND);
        }

        return boothImageRepository.findAllByBoothIdOrderByDisplayOrderAsc(boothId).stream()
                .map(BoothImageResponse::from)
                .toList();
    }

    public BoothImageResponse getById(Long boothId, Long id) {
        BoothImage boothImage = boothImageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BoothImageErrorCode.BOOTH_IMAGE_NOT_FOUND));

        validateBoothImageBelongsToBooth(boothImage, boothId);
        return BoothImageResponse.from(boothImage);
    }

    @Transactional
    public BoothImageResponse update(AdminSessionUser admin, Long boothId, Long id, BoothImageUpdateRequest request) {
        BoothImage boothImage = boothImageRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(BoothImageErrorCode.BOOTH_IMAGE_NOT_FOUND));

        validateBoothImageBelongsToBooth(boothImage, boothId);
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));
        validateAdminCanAccessBooth(admin, booth);

        if (request.displayOrder() != null
                && !request.displayOrder().equals(boothImage.getDisplayOrder())) {
            validateDisplayOrderAvailable(boothId, request.displayOrder());
        }

        String imageUrl = request.imageUrl() != null ? request.imageUrl() : boothImage.getImageUrl();
        Integer displayOrder = request.displayOrder() != null ? request.displayOrder() : boothImage.getDisplayOrder();

        try {
            boothImage.update(imageUrl, displayOrder);
            boothImageRepository.flush();
            return BoothImageResponse.from(boothImage);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(BoothImageErrorCode.DUPLICATE_BOOTH_IMAGE_DISPLAY_ORDER);
        }
    }

    @Transactional
    public void delete(AdminSessionUser admin, Long boothId, Long id) {
        BoothImage boothImage = boothImageRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(BoothImageErrorCode.BOOTH_IMAGE_NOT_FOUND));

        validateBoothImageBelongsToBooth(boothImage, boothId);
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));
        validateAdminCanAccessBooth(admin, booth);

        boothImageRepository.delete(boothImage);
    }

    private void validateDisplayOrderAvailable(Long boothId, Integer displayOrder) {
        if (boothImageRepository.existsByBoothIdAndDisplayOrder(boothId, displayOrder)) {
            throw new BusinessException(BoothImageErrorCode.DUPLICATE_BOOTH_IMAGE_DISPLAY_ORDER);
        }
    }

    private void validateAdminCanAccessBooth(AdminSessionUser admin, Booth booth) {
        if (admin.getRole() == AdminRole.BOOTH
                && !admin.getId().equals(booth.getAdminId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void validateBoothImageBelongsToBooth(BoothImage boothImage, Long boothId) {
        if (!boothImage.getBoothId().equals(boothId)) {
            throw new BusinessException(BoothImageErrorCode.BOOTH_IMAGE_NOT_FOUND);
        }
    }
}
