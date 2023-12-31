package com.wailsaid.youClone.control;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.wailsaid.youClone.Video;
import com.wailsaid.youClone.service.VideoService;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class PagesController {


  @GetMapping
  String getHome() {

    return "index";
  }

  @HxRequest
  @GetMapping
  String getHomeContent() {

    return "index :: main";
  }

  @GetMapping("upload")
  String uploadPage() {
    return "upload";
  }

  @HxRequest
  @GetMapping("upload")
  String uploadForm() {
    return "upload :: form";
  }

  @PostMapping("upload")
  String uploadVideo(@RequestParam("title") String title,
      @RequestParam("description") String description,
      @RequestParam("videoBits") MultipartFile videoBits)
      throws IOException {

    Path path = Paths.get("./uploads/videos/" + videoBits.getOriginalFilename());

    Files.copy(videoBits.getInputStream(), path,
        StandardCopyOption.REPLACE_EXISTING);

    Video v = Video.builder()
        .title(title)
        .description(description)
        .path(path.toString())
        .build();


    return "components :: upload-Done";
  }

  @GetMapping(path = "/watch/{fileId}")
  String watchPage(@PathVariable("fileId") Long id, Model m) {

    return "video";
  }

  @HxRequest
  @GetMapping(path = "/watch/{fileId}")
  String watchComp(@PathVariable("fileId") Long id, Model m) {

    return "video :: video";
  }

  private final VideoService vservice;

  @GetMapping(path = "/stream3/video1", produces = "video/mp4")
  @ResponseBody
  public Mono<Resource> stream3(@RequestHeader("Range") String range) {
    System.out.println("range is : " + range);

    return vservice.StreamVideo();

  }

 }
