package com.likelion.yonsei.daedongje.domain.info.service;

import com.likelion.yonsei.daedongje.common.exception.BusinessException;
import com.likelion.yonsei.daedongje.domain.info.dto.CreatorCreateRequest;
import com.likelion.yonsei.daedongje.domain.info.dto.CreatorResponse;
import com.likelion.yonsei.daedongje.domain.info.dto.CreatorUpdateRequest;
import com.likelion.yonsei.daedongje.domain.info.entity.Creator;
import com.likelion.yonsei.daedongje.domain.info.exception.CreatorErrorCode;
import com.likelion.yonsei.daedongje.domain.info.repository.CreatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreatorService {

    private final CreatorRepository creatorRepository;

    @Transactional
    public CreatorResponse createCreator(CreatorCreateRequest request) {
        Creator creator = Creator.create(
                request.partName(),
                request.departmentName(),
                request.studentId(),
                request.name(),
                request.displayOrder()
        );
        return CreatorResponse.from(creatorRepository.save(creator));
    }

    public List<CreatorResponse> getCreators() {
        return creatorRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(CreatorResponse::from)
                .toList();
    }

    public CreatorResponse getCreator(Long id) {
        return CreatorResponse.from(getCreatorEntity(id));
    }

    @Transactional
    public CreatorResponse updateCreator(Long id, CreatorUpdateRequest request) {
        Creator creator = getCreatorEntity(id);
        creator.update(
                request.partName(),
                request.departmentName(),
                request.studentId(),
                request.name(),
                request.displayOrder()
        );
        return CreatorResponse.from(creator);
    }

    @Transactional
    public void deleteCreator(Long id) {
        creatorRepository.delete(getCreatorEntity(id));
    }

    private Creator getCreatorEntity(Long id) {
        return creatorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CreatorErrorCode.CREATOR_NOT_FOUND));
    }
}
