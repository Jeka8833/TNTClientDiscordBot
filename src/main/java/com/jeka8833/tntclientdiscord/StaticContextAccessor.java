package com.jeka8833.tntclientdiscord;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class StaticContextAccessor {
    private static StaticContextAccessor instance;


    private final ApplicationContext applicationContext;

    public StaticContextAccessor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void registerInstance() {
        instance = this;
    }

    @NotNull
    public static <T> T getBean(Class<T> clazz) {
        return instance.applicationContext.getBean(clazz);
    }
}
