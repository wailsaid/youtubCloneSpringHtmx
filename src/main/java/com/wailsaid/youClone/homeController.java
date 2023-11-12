package com.wailsaid.youClone;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.annotation.Resources;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Range;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class homeController {

  final private videoRepository repository;

  @GetMapping
  String getHome(Model m) {
    var paths =
        Arrays.asList(Paths.get("./uploads/videos").toFile().listFiles())
            .stream()
            .map(f -> f.getPath())
            .toList();
    m.addAttribute("VideoList", repository.findAll()
                                    .stream()
                                    .filter(v -> paths.contains(v.getPath()))
                                    .toList());

    // m.addAttribute("VideoList", repository.findAll());
    return "index";
  }

  @HxRequest
  @GetMapping()
  String getHomeContent(Model m) {
    var paths =
        Arrays.asList(Paths.get("./uploads/videos").toFile().listFiles())
            .stream()
            .map(f -> f.getPath())
            .toList();
    m.addAttribute("VideoList", repository.findAll()
                                    .stream()
                                    .filter(v -> paths.contains(v.getPath()))
                                    .toList());
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

    Path path =
        Paths.get("./uploads/videos/" + videoBits.getOriginalFilename());

    Files.copy(videoBits.getInputStream(), path,
               StandardCopyOption.REPLACE_EXISTING);

    Video v = Video.builder()
                  .title(title)
                  .description(description)
                  .path(path.toString())
                  .build();

    repository.save(v);

    return "components :: upload-Done";
  }

  @GetMapping(path = "/watch/{fileId}")
  String watchPage(@PathVariable("fileId") Long id, Model m) {

    m.addAttribute("video", repository.findById(id).get());
    return "video";
  }

  @HxRequest
  @GetMapping(path = "/watch/{fileId}")
  String watchComp(@PathVariable("fileId") Long id, Model m) {

    m.addAttribute("video", repository.findById(id).get());
    return "video :: video";
  }

  @GetMapping(path = "/stream2/{fileId}", produces = "video/mp4")
  @ResponseBody
  ResponseEntity<InputStreamResource> stream(@PathVariable("fileId") Long id,
                                             @RequestHeader HttpHeaders headers)
      throws IOException {

    var video = repository.findById(id).orElse(null);
    if (video == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    var is = new FileInputStream(video.getPath());
    var randomAccessFile = new RandomAccessFile(video.getPath(), "r");
    var contentLength = is.available();

    HttpRange range = headers.getRange().isEmpty()
                          ? HttpRange.createByteRange(0, contentLength - 1)
                          : headers.getRange().get(0);

    var start = range.getRangeStart(contentLength);
    var end = range.getRangeEnd(contentLength);

    long rangelength = Math.min(1024 * 1024, end - start + 1);

    var respH = new HttpHeaders();
    respH.set("Content-Range",
              String.format("bytes %d-%d/%d", start, start + rangelength - 1,
                            contentLength));

    // var isr = new InputStreamResource(is);
    InputStreamResource isr = new InputStreamResource(
        Channels.newInputStream(randomAccessFile.getChannel())) {
      @Override
      public InputStream getInputStream() {
        try {
          randomAccessFile.seek(start);
          return super.getInputStream();
        } catch (IOException e) {
          throw new RuntimeException("Error seeking in the file", e);
        }
      }

      @Override
      public long contentLength() {
        return rangelength;
      }
    };

    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
        .headers(respH)
        .contentType(MediaType.valueOf("video/mp4"))
        .body(isr);
  }

  // **************************************************************************
  // ************************************************************************
  @GetMapping(path = "/stream/{fileId}")
  @ResponseBody
  ResponseEntity<StreamingResponseBody>
  videoStream(@PathVariable("fileId") Long id,
              @RequestHeader(value = "Range", required = false) String range)
      throws Exception {
    try {
      Video v = repository.findById(id).orElse(null);
      if (v == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }

      StreamingResponseBody responseStream;
      String filePathString = v.getPath();
      Path filePath = Paths.get(filePathString);
      Long fileSize = Files.size(filePath);
      byte[] buffer = new byte[1024];
      final HttpHeaders responseHeaders = new HttpHeaders();

      if (range == null) {
        responseHeaders.add("Content-Type", "video/mp4");
        responseHeaders.add("Content-Length", fileSize.toString());
        responseStream = os -> {
          RandomAccessFile file = new RandomAccessFile(filePathString, "r");
          try (file) {
            long pos = 0;
            file.seek(pos);
            while (pos < fileSize - 1) {
              file.read(buffer);
              os.write(buffer);
              pos += buffer.length;
            }
            os.flush();
          } catch (Exception e) {
          }
        };

        return new ResponseEntity<StreamingResponseBody>(
            responseStream, responseHeaders, HttpStatus.OK);
      }

      String[] ranges = range.split("-");
      Long rangeStart = Long.parseLong(ranges[0].substring(6));
      Long rangeEnd;
      if (ranges.length > 1) {
        rangeEnd = Long.parseLong(ranges[1]);
      } else {
        rangeEnd = fileSize - 1;
      }

      if (fileSize < rangeEnd) {
        rangeEnd = fileSize - 1;
      }

      String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
      responseHeaders.add("Content-Type", "video/mp4");
      responseHeaders.add("Content-Length", contentLength);
      responseHeaders.add("Accept-Ranges", "bytes");
      responseHeaders.add("Content-Range", "bytes"
                                               + " " + rangeStart + "-" +
                                               rangeEnd + "/" + fileSize);
      final Long _rangeEnd = rangeEnd;
      responseStream = os -> {
        RandomAccessFile file = new RandomAccessFile(filePathString, "r");
        try (file) {
          long pos = rangeStart;
          file.seek(pos);
          while (pos < _rangeEnd) {
            file.read(buffer);
            os.write(buffer);
            pos += buffer.length;
          }
          os.flush();
        } catch (Exception e) {
        }
      };

      return new ResponseEntity<StreamingResponseBody>(
          responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT);
    } catch (FileNotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } catch (ClientAbortException cae) {
      // The client disconnected; this is a normal situation
      System.err.println("disconnected");
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      // Log the exception or handle it as needed for your application
      // You may want to log that the client disconnected to track usage
      // or provide more user-friendly feedback if needed
    } catch (IOException e) {

      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}

@Repository
interface videoRepository extends JpaRepository<Video, Long> {}
