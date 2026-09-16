package com.utms.common.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        // TODO: Extract from SecurityContext when Auth module is built
        // For now, return a default value for local development
        return Optional.of("system");
    }
}
