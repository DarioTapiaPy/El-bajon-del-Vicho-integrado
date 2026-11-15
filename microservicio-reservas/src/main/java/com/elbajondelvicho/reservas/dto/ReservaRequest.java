
package com.elbajondelvicho.reservas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaRequest(

        @NotNull(message = "La cantidad de personas es obligatoria")
        @Min(value = 1, message = "Minimo 1 persona")
        @Max(value = 5, message = "Maximo 5 personas")
        Integer personas,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La hora es obligatoria")
        LocalTime hora,

        @NotBlank(message = "La sucursal es obligatoria")
        @Size(max = 40, message = "Sucursal demasiado larga")
        String sucursal

) {
}
