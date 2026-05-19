package com.likelion.yonsei.daedongje.domain.booth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.common.exception.CommonErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuCreateRequest;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuResponse;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuUpdateRequest;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.Menu;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.exception.MenuErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.MenuRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    public MenuResponse create(AdminSessionUser admin, Long boothId, MenuCreateRequest request) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

        validateAdminCanAccessBooth(admin, booth);

        if (menuRepository.existsByBoothIdAndDisplayOrder(boothId, request.displayOrder())) {
            throw new BusinessException(MenuErrorCode.DUPLICATE_MENU_DISPLAY_ORDER);
        }

        Menu menu = Menu.builder()
                .booth(booth)
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .imageUrl(request.imageUrl())
                .isSoldOut(request.isSoldOut() != null ? request.isSoldOut() : false)
                .displayOrder(request.displayOrder())
                .build();

        try {
            return MenuResponse.from(menuRepository.save(menu));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(MenuErrorCode.DUPLICATE_MENU_DISPLAY_ORDER);
        }
    }

    // 부스별 메뉴 목록 조회
    public List<MenuResponse> getListByBooth(Long boothId) {
        if (!boothRepository.existsById(boothId)) {
            throw new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND);
        }

        return menuRepository.findMenusByBoothId(boothId).stream()
                .map(MenuResponse::from)
                .toList();
    }

    // 메뉴 단건 조회
    public MenuResponse getById(Long boothId, Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

        validateMenuBelongsToBooth(menu, boothId);
        return MenuResponse.from(menu);
    }

    // 메뉴 수정
    @Transactional
    public MenuResponse update(AdminSessionUser admin, Long boothId, Long id, MenuUpdateRequest request) {
        Menu menu = menuRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

        validateMenuBelongsToBooth(menu, boothId);
        validateAdminCanAccessBooth(admin, menu.getBooth());

        if (request.displayOrder() != null
                && !request.displayOrder().equals(menu.getDisplayOrder())
                && menuRepository.existsByBoothIdAndDisplayOrder(menu.getBooth().getId(), request.displayOrder())) {
            throw new BusinessException(MenuErrorCode.DUPLICATE_MENU_DISPLAY_ORDER);
        }

        try {
            menu.update(
                    request.name(),
                    request.description(),
                    request.price(),
                    request.imageUrl(),
                    request.isSoldOut(),
                    request.displayOrder());
            menuRepository.flush();
            return MenuResponse.from(menu);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(MenuErrorCode.DUPLICATE_MENU_DISPLAY_ORDER);
        }
    }

    // 메뉴 삭제
    @Transactional
    public void delete(AdminSessionUser admin, Long boothId, Long id) {
        Menu menu = menuRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

        validateMenuBelongsToBooth(menu, boothId);
        validateAdminCanAccessBooth(admin, menu.getBooth());

        menuRepository.delete(menu);
    }

    // 메뉴 표시 순서 일괄 재정렬
    @Transactional
    public List<MenuResponse> reorder(AdminSessionUser admin, Long boothId, List<Long> menuIds) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND));

        validateAdminCanAccessBooth(admin, booth);

        List<Menu> menus = menuRepository.findByBoothIdForUpdate(boothId);
        Map<Long, Menu> menuById = menus.stream()
                .collect(Collectors.toMap(Menu::getId, Function.identity()));

        validateMenuIdsArePermutation(menuIds, menuById.keySet());

        // 1단계: uk_menus_booth_display_order UNIQUE 충돌을 피하기 위해 임시 음수값으로 이동
        // MySQL은 트랜잭션 안에서도 UNIQUE 제약을 행 단위로 즉시 검사하므로,
        // 최종값을 곧장 쓰면 기존 행과 순서가 겹쳐 충돌한다.
        for (int i = 0; i < menuIds.size(); i++) {
            menuById.get(menuIds.get(i)).changeDisplayOrder(-(i + 1));
        }
        menuRepository.flush();

        // 2단계: 요청 순서대로 최종 표시 순서(1..N)를 부여
        List<Menu> reordered = new ArrayList<>(menuIds.size());
        for (int i = 0; i < menuIds.size(); i++) {
            Menu menu = menuById.get(menuIds.get(i));
            menu.changeDisplayOrder(i + 1);
            reordered.add(menu);
        }

        try {
            menuRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(MenuErrorCode.DUPLICATE_MENU_DISPLAY_ORDER);
        }

        return reordered.stream()
                .map(MenuResponse::from)
                .toList();
    }

    // menuIds가 부스 메뉴 id의 완전한 순열인지 검증 (누락·이물질·중복 차단)
    private void validateMenuIdsArePermutation(List<Long> menuIds, Set<Long> boothMenuIds) {
        if (menuIds.size() != boothMenuIds.size()
                || !new HashSet<>(menuIds).equals(boothMenuIds)) {
            throw new BusinessException(MenuErrorCode.INVALID_MENU_REORDER_REQUEST);
        }
    }

    private void validateAdminCanAccessBooth(AdminSessionUser admin, Booth booth) {
        if (admin.getRole() == AdminRole.BOOTH
                && !admin.getId().equals(booth.getAdminId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void validateMenuBelongsToBooth(Menu menu, Long boothId) {
        if (!menu.getBooth().getId().equals(boothId)) {
            throw new BusinessException(MenuErrorCode.MENU_NOT_FOUND);
        }
    }
}
