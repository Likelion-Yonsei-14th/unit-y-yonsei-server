package com.likelion.yonsei.daedongje.domain.booth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.common.exception.CommonErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuCreateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuUpdateRequest;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.Menu;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.MenuRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final BoothRepository boothRepository;

    public MenuService(MenuRepository menuRepository, BoothRepository boothRepository) {
        this.menuRepository = menuRepository;
        this.boothRepository = boothRepository;
    }

    // 메뉴 생성
    @Transactional
    public MenuResponse create(Long boothId, MenuCreateRequest request) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

        if (menuRepository.existsByBoothIdAndDisplayOrder(boothId, request.displayOrder())) {
            throw new BusinessException(CommonErrorCode.CONFLICT);
        }

        Menu menu = Menu.builder()
                .booth(booth)
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .imageUrl(request.imageUrl())
                .isSoldOut(request.isSoldOut())
                .displayOrder(request.displayOrder())
                .build();

        try {
            return MenuResponse.from(menuRepository.save(menu));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(CommonErrorCode.CONFLICT);
        }
    }

    // 부스별 메뉴 목록 조회
    public List<MenuResponse> getListByBooth(Long boothId) {
        boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

        return menuRepository.findMenusByBoothId(boothId).stream()
                .map(MenuResponse::from)
                .toList();
    }

    // 메뉴 단건 조회
    public MenuResponse getById(Long boothId, Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        validateMenuBelongsToBooth(menu, boothId);
        return MenuResponse.from(menu);
    }

    // 메뉴 수정
    @Transactional
    public MenuResponse update(Long boothId, Long id, MenuUpdateRequest request) {
        Menu menu = menuRepository.findByIdWithLock(id);

        if (menu == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        validateMenuBelongsToBooth(menu, boothId);

        if (request.displayOrder() != null
                && !request.displayOrder().equals(menu.getDisplayOrder())
                && menuRepository.existsByBoothIdAndDisplayOrder(menu.getBooth().getId(), request.displayOrder())) {
            throw new BusinessException(CommonErrorCode.CONFLICT);
        }

        try {
            menu.update(
                    request.name(),
                    request.description(),
                    request.price(),
                    request.imageUrl(),
                    request.isSoldOut(),
                    request.displayOrder());
            return MenuResponse.from(menu);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(CommonErrorCode.CONFLICT);
        }
    }

    // 메뉴 삭제
    @Transactional
    public void delete(Long boothId, Long id) {
        Menu menu = menuRepository.findByIdWithLock(id);

        if (menu == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        validateMenuBelongsToBooth(menu, boothId);

        menuRepository.delete(menu);
    }

    private void validateMenuBelongsToBooth(Menu menu, Long boothId) {
        if (!menu.getBooth().getId().equals(boothId)) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}
