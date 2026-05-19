package com.likelion.yonsei.daedongje.domain.booth.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.common.exception.CommonErrorCode;
import com.likelion.yonsei.daedongje.domain.auth.entity.AdminRole;
import com.likelion.yonsei.daedongje.domain.auth.support.AdminSessionUser;
import com.likelion.yonsei.daedongje.domain.booth.dto.MenuResponse;
import com.likelion.yonsei.daedongje.domain.booth.entity.Booth;
import com.likelion.yonsei.daedongje.domain.booth.entity.BoothStatus;
import com.likelion.yonsei.daedongje.domain.booth.entity.Menu;
import com.likelion.yonsei.daedongje.domain.booth.exception.BoothErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.exception.MenuErrorCode;
import com.likelion.yonsei.daedongje.domain.booth.repository.BoothRepository;
import com.likelion.yonsei.daedongje.domain.booth.repository.MenuRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    private static final Long BOOTH_ID = 1L;
    private static final Long BOOTH_ADMIN_ID = 10L;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private BoothRepository boothRepository;

    @InjectMocks
    private MenuService menuService;

    @Test
    @DisplayName("재정렬은 요청한 순서대로 displayOrder를 1..N으로 부여한다")
    void reorderAssignsDisplayOrderFollowingRequestedSequence() {
        Booth booth = booth(BOOTH_ID, BOOTH_ADMIN_ID);
        Menu menu10 = menu(10L, booth, 1);
        Menu menu20 = menu(20L, booth, 2);
        Menu menu30 = menu(30L, booth, 3);
        when(boothRepository.findById(BOOTH_ID)).thenReturn(Optional.of(booth));
        when(menuRepository.findByBoothIdForUpdate(BOOTH_ID))
                .thenReturn(new ArrayList<>(List.of(menu10, menu20, menu30)));

        List<MenuResponse> result = menuService.reorder(superAdmin(), BOOTH_ID, List.of(30L, 10L, 20L));

        assertThat(menu30.getDisplayOrder()).isEqualTo(1);
        assertThat(menu10.getDisplayOrder()).isEqualTo(2);
        assertThat(menu20.getDisplayOrder()).isEqualTo(3);
        assertThat(result).extracting(MenuResponse::getId).containsExactly(30L, 10L, 20L);
        assertThat(result).extracting(MenuResponse::getDisplayOrder).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("재정렬은 임시 음수값으로 먼저 옮긴 뒤 최종값을 쓰는 2단계로 동작한다")
    void reorderWritesInTwoStagesUsingTemporaryNegativeValues() {
        Booth booth = booth(BOOTH_ID, BOOTH_ADMIN_ID);
        Menu menu10 = menu(10L, booth, 1);
        Menu menu20 = menu(20L, booth, 2);
        List<Menu> menus = new ArrayList<>(List.of(menu10, menu20));
        when(boothRepository.findById(BOOTH_ID)).thenReturn(Optional.of(booth));
        when(menuRepository.findByBoothIdForUpdate(BOOTH_ID)).thenReturn(menus);

        List<Integer> stageOneOrders = new ArrayList<>();
        doAnswer(invocation -> {
            if (stageOneOrders.isEmpty()) {
                menus.forEach(menu -> stageOneOrders.add(menu.getDisplayOrder()));
            }
            return null;
        }).when(menuRepository).flush();

        menuService.reorder(superAdmin(), BOOTH_ID, List.of(20L, 10L));

        assertThat(stageOneOrders).allMatch(order -> order < 0);
        assertThat(stageOneOrders).doesNotHaveDuplicates();
        verify(menuRepository, times(2)).flush();
    }

    @Test
    @DisplayName("존재하지 않는 부스 재정렬 요청은 BOOTH_NOT_FOUND 예외를 던진다")
    void reorderThrowsWhenBoothNotFound() {
        when(boothRepository.findById(BOOTH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.reorder(superAdmin(), BOOTH_ID, List.of(1L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BoothErrorCode.BOOTH_NOT_FOUND));
    }

    @Test
    @DisplayName("BOOTH 관리자가 담당하지 않는 부스를 재정렬하면 FORBIDDEN 예외를 던진다")
    void reorderRejectsBoothAdminAccessingAnotherBooth() {
        Booth booth = booth(BOOTH_ID, BOOTH_ADMIN_ID);
        when(boothRepository.findById(BOOTH_ID)).thenReturn(Optional.of(booth));
        AdminSessionUser otherBoothAdmin = new AdminSessionUser(999L, AdminRole.BOOTH, "other");

        assertThatThrownBy(() -> menuService.reorder(otherBoothAdmin, BOOTH_ID, List.of(1L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("부스 메뉴 일부가 빠진 목록은 INVALID_MENU_REORDER_REQUEST 예외를 던진다")
    void reorderRejectsMenuIdsMissingABoothMenu() {
        Booth booth = booth(BOOTH_ID, BOOTH_ADMIN_ID);
        when(boothRepository.findById(BOOTH_ID)).thenReturn(Optional.of(booth));
        when(menuRepository.findByBoothIdForUpdate(BOOTH_ID))
                .thenReturn(new ArrayList<>(List.of(menu(10L, booth, 1), menu(20L, booth, 2))));

        assertThatThrownBy(() -> menuService.reorder(superAdmin(), BOOTH_ID, List.of(10L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MenuErrorCode.INVALID_MENU_REORDER_REQUEST));
    }

    @Test
    @DisplayName("다른 부스 메뉴 id가 섞인 목록은 INVALID_MENU_REORDER_REQUEST 예외를 던진다")
    void reorderRejectsMenuIdsContainingForeignId() {
        Booth booth = booth(BOOTH_ID, BOOTH_ADMIN_ID);
        when(boothRepository.findById(BOOTH_ID)).thenReturn(Optional.of(booth));
        when(menuRepository.findByBoothIdForUpdate(BOOTH_ID))
                .thenReturn(new ArrayList<>(List.of(menu(10L, booth, 1), menu(20L, booth, 2))));

        assertThatThrownBy(() -> menuService.reorder(superAdmin(), BOOTH_ID, List.of(10L, 999L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MenuErrorCode.INVALID_MENU_REORDER_REQUEST));
    }

    @Test
    @DisplayName("중복된 메뉴 id가 포함된 목록은 INVALID_MENU_REORDER_REQUEST 예외를 던진다")
    void reorderRejectsMenuIdsContainingDuplicate() {
        Booth booth = booth(BOOTH_ID, BOOTH_ADMIN_ID);
        when(boothRepository.findById(BOOTH_ID)).thenReturn(Optional.of(booth));
        when(menuRepository.findByBoothIdForUpdate(BOOTH_ID))
                .thenReturn(new ArrayList<>(List.of(menu(10L, booth, 1), menu(20L, booth, 2))));

        assertThatThrownBy(() -> menuService.reorder(superAdmin(), BOOTH_ID, List.of(10L, 10L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MenuErrorCode.INVALID_MENU_REORDER_REQUEST));
    }

    private AdminSessionUser superAdmin() {
        return new AdminSessionUser(1L, AdminRole.SUPER, "super");
    }

    private Booth booth(Long id, Long adminId) {
        Booth booth = Booth.create(
                adminId, "부스", "동아리", "소개",
                2, LocalTime.of(11, 0), LocalTime.of(20, 0),
                null, 1, BoothStatus.OPEN,
                false, null, false, null, null, null, false, null);
        ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }

    private Menu menu(Long id, Booth booth, Integer displayOrder) {
        Menu menu = Menu.builder()
                .booth(booth)
                .name("menu" + id)
                .price(1000)
                .displayOrder(displayOrder)
                .build();
        ReflectionTestUtils.setField(menu, "id", id);
        return menu;
    }
}
