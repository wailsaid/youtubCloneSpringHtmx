package com.wailsaid.youClone;

import java.io.IOException;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class homeController {


    final private videoRepository repository;

    @GetMapping
    String getHome() {
        return "index";
    }

    @GetMapping("/home")
    String getHomeContent() {
        return "index :: main";
    }

    @GetMapping("upload")
    String uploadPage(){
        return "upload";
    }

    @PostMapping("upload")
    String uploadVideo( @RequestParam("title") String title,
                        @RequestParam ("description")String description,
                        @RequestParam  ("videoBits") MultipartFile videoBits) throws IOException{
        System.out.println(videoBits.getContentType());

        var v = Video.builder()
        .title(title)
        .description(description)
        .videoBits(videoBits.getBytes())
        .build();

        repository.save(v);

        return "upload :: success";
    }

}


/**
 * InnerhomeController
 */
@Repository
interface videoRepository  extends JpaRepository<Video,Long> {


}
