package com.wailsaid.youClone.control;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * upcontrol
 */
@RestController
@RequestMapping("/foo")
public class upcontrol {

  @GetMapping
  String foo() {

    return "";
  }
}
