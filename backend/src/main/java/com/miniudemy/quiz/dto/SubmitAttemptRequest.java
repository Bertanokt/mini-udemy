package com.miniudemy.quiz.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Sınav gönderme isteği. Cevapsız bırakılan soru listeye konmaz ve 0 puan alır. */
public record SubmitAttemptRequest(
        @NotNull(message = "Cevap listesi zorunludur.")
        List<@Valid @NotNull(message = "Cevap boş olamaz.") AnswerRequest> answers) {
}
