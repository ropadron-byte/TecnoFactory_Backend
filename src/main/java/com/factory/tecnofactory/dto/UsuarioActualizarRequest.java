package com.factory.tecnofactory.dto;

import com.factory.tecnofactory.validation.ConRun;
import com.factory.tecnofactory.validation.RunValido;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos que un administrador puede editar de otro usuario.
 * La clave es opcional: si viene vacía se conserva la actual.
 */
@RunValido
public record UsuarioActualizarRequest(

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

        // En blanco = conservar la actual. @Size(min=8) no aceptaría "", por eso se valida con @Pattern.
        @Pattern(regexp = "(?s)^$|^.{8,72}$", message = "La clave debe tener entre 8 y 72 caracteres")
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
        Integer idComuna,

        Boolean activo
) implements ConRun {

    @Override
    public String toString() {
        return "UsuarioActualizarRequest[run=" + run + "-" + runDv + ", correo=" + correo + "]";
    }
}
