package com.likelion.yonsei.daedongje.domain.booth.dto;

import com.likelion.yonsei.daedongje.domain.booth.entity.Menu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

// 메뉴 응답 DTO
@Getter
@Builder
@Schema(description = "메뉴 응답")
public class MenuResponse {

    @Schema(description = "메뉴 ID", example = "1")
    private Long id;

    @Schema(description = "부스 ID", example = "1")
    private Long boothId;

    @Schema(description = "메뉴 이름", example = "핫도그")
    private String name;

    @Schema(description = "메뉴 설명", example = "맛있는 핫도그")
    private String description;

    @Schema(description = "메뉴 가격", example = "5000")
    private Integer price;

    @Schema(description = "메뉴 이미지 URL", example = "https://image.com/menu.png")
    private String imageUrl;

    @Schema(description = "품절 여부", example = "false")
    private Boolean isSoldOut;

    @Schema(description = "메뉴 표시 순서", example = "1")
    private Integer displayOrder;

    // Menu 엔티티를 MenuResponse DTO로 변환
    public static MenuResponse from(Menu menu) {
        return MenuResponse.builder()
                .id(menu.getId())
                .boothId(menu.getBooth().getId())
                .name(menu.getName())
                .description(menu.getDescription())
                .price(menu.getPrice())
                .imageUrl(menu.getImageUrl())
                .isSoldOut(menu.getIsSoldOut())
                .displayOrder(menu.getDisplayOrder())
                .build();
    }
}
