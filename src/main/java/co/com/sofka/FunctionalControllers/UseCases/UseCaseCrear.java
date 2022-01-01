package co.com.sofka.FunctionalControllers.UseCases;

import co.com.sofka.FunctionalControllers.Collections.Dato;
import co.com.sofka.FunctionalControllers.DTOs.DatoDTO;
import co.com.sofka.FunctionalControllers.Mappers.MapperUtils;
import co.com.sofka.FunctionalControllers.Repositories.Repositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Service
@Validated
public class UseCaseCrear implements GuardarDato {
    private final Repositorio repositorio;
    private final MapperUtils mapperUtils;

    @Autowired
    public UseCaseCrear(MapperUtils mapperUtils, Repositorio repositorio) {
        this.repositorio = repositorio;
        this.mapperUtils = mapperUtils;
    }

    @Override
    public Mono<String> apply(DatoDTO datoDTO) {
        return repositorio.save(mapperUtils.mapperToDato(null).apply(datoDTO)).map(Dato::getId);
    }
}