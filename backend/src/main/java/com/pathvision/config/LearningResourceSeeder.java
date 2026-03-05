package com.pathvision.config;

import com.pathvision.entity.LearningResource;
import com.pathvision.repository.LearningResourceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LearningResourceSeeder {

    @Bean
    CommandLineRunner seedLearningResources(LearningResourceRepository repository) {
        return args -> {
            if (repository.countByActiveTrue() > 0) {
                return;
            }

            List<LearningResource> seed = List.of(
                    create("CS50x: Introduction to Computer Science", "Harvard / edX", "Course", "Beginner", "https://cs50.harvard.edu/x/", "engineering_cse_it"),
                    create("Python for Everybody (Full Video)", "freeCodeCamp", "YouTube", "Beginner", "https://www.youtube.com/watch?v=rfscVS0vtbw", "engineering_cse_it"),
                    create("Machine Learning", "NPTEL", "NPTEL", "Intermediate", "https://nptel.ac.in/courses/106/106/106106139/", "engineering_ai_ds"),
                    create("Data Analysis with Python", "freeCodeCamp", "Course", "Intermediate", "https://www.freecodecamp.org/learn/data-analysis-with-python/", "engineering_ai_ds"),
                    create("Data Structures and Algorithms in Java", "NPTEL", "NPTEL", "Intermediate", "https://nptel.ac.in/courses/106/105/106105225/", "arts_science_computer_science"),
                    create("Responsive Web Design", "freeCodeCamp", "Course", "Beginner", "https://www.freecodecamp.org/learn/2022/responsive-web-design/", "arts_science_computer_science"),
                    create("Principles of Management", "NPTEL", "NPTEL", "Beginner", "https://nptel.ac.in/courses/110/105/110105146/", "arts_science_commerce_bba"),
                    create("Introduction to Psychology", "Yale / Coursera", "Course", "Beginner", "https://www.coursera.org/learn/introduction-psychology", "arts_science_media_psychology")
            );

            repository.saveAll(seed);
        };
    }

    private LearningResource create(
            String title,
            String provider,
            String source,
            String level,
            String url,
            String interestKey
    ) {
        LearningResource item = new LearningResource();
        item.setTitle(title);
        item.setProvider(provider);
        item.setSource(source);
        item.setLevel(level);
        item.setUrl(url);
        item.setInterestKey(interestKey);
        item.setActive(true);
        return item;
    }
}
