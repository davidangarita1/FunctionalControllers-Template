package co.com.sofka.FunctionalControllers.Repositories;

import co.com.sofka.FunctionalControllers.Collections.Dato;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface Repositorio extends ReactiveMongoRepository<Dato, String> {
}