package com.nailconnect.api.security;
import java.util.UUID;
public record UserPrincipal(UUID id,String email,String role) {}
