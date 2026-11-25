package com.chargeslot.service.conflict;
import java.util.List;
public interface ConflictPolicy { List<Conflict> check(ConflictContext context); }
