package co.com.sofka.FunctionalControllers.UseCases;

import co.com.sofka.FunctionalControllers.DTOs.DatoDTO;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface GuardarDato {
    public Mono<String> apply(DatoDTO datoDTO);
}
