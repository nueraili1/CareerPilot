package com.careerpilot.service;

import com.careerpilot.dto.ResumeParseResponse;
import java.io.IOException;
import java.util.Set;
import org.apache.tika.exception.TikaException;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeParseService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParseService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt");

    private final Tika tika = new Tika();

    public ResumeParseResponse parse(MultipartFile file) throws IOException, TikaException {
        validate(file);
        long startedAt = System.currentTimeMillis();
        String text = tika.parseToString(file.getInputStream());
        String cleaned = text.replaceAll("\\s{3,}", "\n\n").trim();
        log.info("Resume parsed: fileName={}, size={}, chars={}, costMs={}",
                file.getOriginalFilename(), file.getSize(), cleaned.length(), System.currentTimeMillis() - startedAt);
        return new ResumeParseResponse(file.getOriginalFilename(), cleaned);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传简历文件");
        }

        String fileName = file.getOriginalFilename();
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            throw new IllegalArgumentException("文件名不合法");
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 PDF、DOCX、TXT 简历文件");
        }
    }
}
