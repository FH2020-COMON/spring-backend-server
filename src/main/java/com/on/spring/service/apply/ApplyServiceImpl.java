package com.on.spring.service.apply;

import com.on.spring.entity.apply.Apply;
import com.on.spring.entity.apply.ApplyRepository;
import com.on.spring.entity.company.Company;
import com.on.spring.entity.company.CompanyRepository;
import com.on.spring.exception.ApplyNotFoundException;
import com.on.spring.exception.CompanyNotFoundException;
import com.on.spring.exception.FileIsNotFoundException;
import com.on.spring.payload.request.AddApplyRequest;
import com.on.spring.payload.response.ApplyResponse;
import com.on.spring.payload.response.ApplyListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplyServiceImpl implements ApplyService {
    private final ApplyRepository applyRepository;
    private final CompanyRepository companyRepository;

    @Value("${spring.file.path}")
    private String filePath;

    @Override
    public void uploadApply(AddApplyRequest request) {
        int cur = 1;

        Apply apply = applyRepository.save(
                Apply.builder()
                        .companyId(request.getCompanyId())
                        .companyName(request.getCompanyName())
                        .date(LocalDateTime.of(request.getYear(), request.getMonth(), request.getDay(), request.getHour(), request.getMinute()))
                        .imageNum(0)
                        .hashTag(request.getHashTag())
                        .build()
        );

        Long applyId = apply.getApplyId();

        try {
            request.getFiles().get(0).transferTo(new File(filePath + "apply/" + applyId + "/" + "preview.png"));
            request.getFiles().remove(0);
            for (MultipartFile file : request.getFiles()) {
                file.transferTo(new File(filePath + "apply/"+ applyId + "/" + cur + ".png"));
                cur++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        applyRepository.save(
            apply.addImageNum(cur)
        );
    }

    @Override
    public ApplyResponse viewApply(Long applyId) {
        return applyRepository.findByApplyId(applyId)
                .map(apply -> {
                    Duration diffTime = Duration.between(apply.getDate(), LocalDateTime.now());
                    return ApplyResponse.builder()
                            .applyName(apply.getApplyName())
                            .companyName(apply.getCompanyName())
                            .date(diffTime.toString())
                            .hashTag(apply.getHashTag())
                            .build();
                })
                .orElseThrow(ApplyNotFoundException::new);
    }

    @Override
    public MultipartFile viewApplyPreview(Long applyId) {
        MultipartFile file;

        try {
            file = new MockMultipartFile("preview.png", new FileInputStream(filePath + applyId + "/" + "preview.png"));
        } catch (IOException e) {
            throw new FileIsNotFoundException();
        }

        return file;
    }

    @Override
    public List<MultipartFile> viewApplyImages(Long applyId) {
        return applyRepository.findByApplyId(applyId)
                .map(apply -> {
                    List<MultipartFile> files = new ArrayList<>();
                    int cur = apply.getImageNum();
                    try {
                        for (int i = 1; i <= apply.getApplyId(); i++) {
                            MultipartFile file = new MockMultipartFile(cur + ".png", new FileInputStream(filePath + applyId + "/" + cur + ".png"));
                            files.add(file);
                        }
                        return files;
                    } catch (IOException e) {
                        throw new FileIsNotFoundException();
                    }
                })
                .orElseThrow(ApplyNotFoundException::new);
    }

    @Override
    public List<ApplyListResponse> companyApplyView() {
        List<ApplyListResponse> responses = new ArrayList<>();
        applyRepository.findAll().forEach(apply ->  {
            Duration diffTime = Duration.between(apply.getDate(), LocalDateTime.now());

            Company company = companyRepository.findByCompanyId(apply.getCompanyId())
                    .orElseThrow(CompanyNotFoundException::new);
            try {
                responses.add(
                        ApplyListResponse.builder()
                                .applyName(apply.getApplyName())
                                .companyName(apply.getCompanyName())
                                .companyId(apply.getCompanyId())
                                .dDay(diffTime.toDays())
                                .likes(company.getLikes())
                                .preview(new MockMultipartFile("preview.png", new FileInputStream(filePath + apply.getApplyId() + "/" + "preview.png")))
                                .build()
                );
            } catch (IOException e) {
                throw new FileIsNotFoundException();
            }
        });

        return responses;
    }
}
