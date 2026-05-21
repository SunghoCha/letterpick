package com.sungho.letterpick.newsletter.adapter.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sungho.letterpick.common.domain.Email;
import com.sungho.letterpick.newsletter.adapter.persistence.NewslettersRepository;
import com.sungho.letterpick.newsletter.domain.Newsletter;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(0)
@Profile("local")
@ConditionalOnProperty(name = "letterpick.seed.newsletter.enabled", havingValue = "true")
public class NewsletterSeedRunner implements ApplicationRunner {

    private static final String SEED_PATH = "/seed/newsletters.json";

    private final NewslettersRepository newslettersRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (newslettersRepository.count() > 0) {
            log.info("Newsletter 시드 이미 적재됨. skip (current count > 0)");
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (InputStream in = getClass().getResourceAsStream(SEED_PATH)) {
            if (in == null) {
                throw new IllegalStateException("시드 파일을 찾을 수 없습니다: " + SEED_PATH);
            }

            List<NewsletterSeed> seeds = objectMapper.readValue(in, new TypeReference<>() {});

            List<Newsletter> newsletters = seeds.stream()
                    .map(seed -> Newsletter.register(
                            seed.name(),
                            seed.description(),
                            seed.imageUrl(),
                            NewsletterCategory.valueOf(seed.letterPickCategory()),
                            seed.subscribeUrl(),
                            seed.mainPageUrl(),
                            new Email(seed.email())
                    ))
                    .toList();

            newslettersRepository.saveAll(newsletters);
            log.info("Newsletter 시드 적재 완료: {}개", newsletters.size());
        }
    }
}
