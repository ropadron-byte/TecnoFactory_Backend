package com.factory.tecnofactory.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UsuarioRequest(

        @NotNull(message = "El RUN es obligatorio")
        @Min(value = 1_000_000, message = "El RUN no es válido")
        @Max(value = 99_999_999, message = "El RUN no es válido")
        Integer run,

        @NotBlank(message = "El dígito verificador es obligatorio")
        @Pattern(regexp = "[0-9kK]", message = "El dígito verificador debe ser un número o K")
        String runDv,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        String nombre,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 255, message = "Los apellidos no pueden superar los 255 caracteres")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 255, message = "El correo no puede superar los 255 caracteres")
        String correo,

        @NotBlank(message = "La clave es obligatoria")
        @Size(min = 8, max = 72, message = "La clave debe tener entre 8 y 72 caracteres")
        String clave,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
        String direccion,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "\\+?[0-9]{8,14}", message = "El teléfono debe tener entre 8 y 14 dígitos, con + opcional")
        String telefono,

        @NotNull(message = "La comuna es obligatoria")
        Integer idComuna
) {

    @Override
    public String toString() {
        return "UsuarioRequest[correo=" + correo + ", run=" + run + "-" + runDv + "]";
    }
}