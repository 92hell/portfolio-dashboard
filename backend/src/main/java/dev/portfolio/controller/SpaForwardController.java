package dev.portfolio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Lets client-side routes (e.g. /experience) load the React app instead of 404-ing. */
@Controller
public class SpaForwardController {

    // Single-segment, dot-free paths only, so /assets/*.js and /api/** are never swallowed.
    @GetMapping("/{path:^(?!api$|actuator$|assets$)[^.]*}")
    public String forward() {
        return "forward:/index.html";
    }
}
