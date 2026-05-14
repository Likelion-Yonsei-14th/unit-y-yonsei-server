package com.likelion.yonsei.daedongje.domain.info.entity;

import com.likelion.yonsei.daedongje.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "creators")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Creator extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "part_name", length = 100)
    private String partName;

    @Column(name = "department_name", length = 100)
    private String departmentName;

    @Column(name = "student_id", length = 20)
    private String studentId;

    @Column(length = 50)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    private Creator(
            String partName,
            String departmentName,
            String studentId,
            String name,
            Integer displayOrder
    ) {
        this.partName = partName;
        this.departmentName = departmentName;
        this.studentId = studentId;
        this.name = name;
        this.displayOrder = displayOrder;
    }

    public static Creator create(
            String partName,
            String departmentName,
            String studentId,
            String name,
            Integer displayOrder
    ) {
        return new Creator(
                normalize(partName),
                normalize(departmentName),
                normalize(studentId),
                normalize(name),
                displayOrder
        );
    }

    public void update(
            String partName,
            String departmentName,
            String studentId,
            String name,
            Integer displayOrder
    ) {
        this.partName = normalize(partName);
        this.departmentName = normalize(departmentName);
        this.studentId = normalize(studentId);
        this.name = normalize(name);
        this.displayOrder = displayOrder;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
