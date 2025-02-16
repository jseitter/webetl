package io.webetl.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError() {
        // Check if the request is for a static resource
        @SuppressWarnings("null")
        String path = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
            .getRequest().getServletPath();
        
        if (path.contains(".")) {
            return null; // Let Spring handle 404 for resources
        }
        
        return "forward:/index.html";
    }
} 