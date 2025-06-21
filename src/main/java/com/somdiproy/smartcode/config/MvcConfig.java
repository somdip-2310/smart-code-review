package com.somdiproy.smartcode.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 * Adds common model attributes to all views
 * 
 * @author Somdip Roy
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CommonModelAttributesInterceptor());
    }

    /**
     * Interceptor to add common attributes to all views
     */
    private static class CommonModelAttributesInterceptor implements HandlerInterceptor {
        
        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                             Object handler, ModelAndView modelAndView) throws Exception {
            if (modelAndView != null && modelAndView.hasView()) {
                // Add request URI for use in templates
                modelAndView.addObject("requestURI", request.getRequestURI());
                modelAndView.addObject("requestURL", request.getRequestURL().toString());
                modelAndView.addObject("contextPath", request.getContextPath());
                
                // Add current year for copyright
                modelAndView.addObject("currentYear", java.time.Year.now().getValue());
            }
        }
    }
}