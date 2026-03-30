package com.lgourabdash.portfolio;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AboutSectionService {

    private final AboutRepository aboutRepository;

    public AboutSectionService(AboutRepository aboutRepository) {
        this.aboutRepository = aboutRepository;
    }

    public AboutSection getAbout() {
        return aboutRepository.findAll().stream().findFirst().orElse(new AboutSection());
    }

    public AboutSection updateAbout(String content) {
        AboutSection about = aboutRepository.findAll().stream().findFirst().orElse(new AboutSection());
        about.setContent(content);
        return aboutRepository.save(about);
    }
}
