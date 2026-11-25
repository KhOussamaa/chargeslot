package com.chargeslot.service.conflict;

import com.chargeslot.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ConflictEngine {
    private final List<ConflictPolicy> policies;
    public ConflictEngine(List<ConflictPolicy> policies) { this.policies = policies; }
    public List<Conflict> detect(ConflictContext context) { return policies.stream().flatMap(p -> p.check(context).stream()).toList(); }
    public void requireNoConflicts(ConflictContext context) {
        List<Conflict> conflicts = detect(context);
        if (!conflicts.isEmpty()) throw new ApiException(HttpStatus.CONFLICT, conflicts.getFirst().code(), conflicts.getFirst().message(), conflicts);
    }
}
