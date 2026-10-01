package com.sast.readtrack.dto;
import jakarta.validation.constraints.*;
public final class Requests {
    private Requests() {}
    public record Credentials(@NotBlank @Size(max=50) String username,
                              @NotBlank @Size(max=72) String password) {}
    public record NewBook(@NotBlank @Size(max=200) String title,
                          @Size(max=100) String author,
                          @NotNull @Positive Integer totalPages) {}
    public record Progress(@NotNull @PositiveOrZero Integer readPages) {}
}
