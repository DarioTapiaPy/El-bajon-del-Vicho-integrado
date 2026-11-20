package com.elbajondelvicho.pedidos.client;

import com.elbajondelvicho.pedidos.dto.ProductoCatalogo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CatalogoClient {

    private final RestClient restClient;

    public CatalogoClient(
            RestClient.Builder builder,
            @Value("${catalogo.base-url}") String baseUrl
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .build();
    }

    public ProductoCatalogo[] obtenerProductos() {
        try {
            ProductoCatalogo[] productos = restClient
                    .get()
                    .uri("/api/v1/productos")
                    .retrieve()
                    .body(ProductoCatalogo[].class);

            if (productos == null) {
                throw new IllegalStateException(
                        "El catalogo devolvio una respuesta vacia"
                );
            }

            return productos;

        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "No se pudo consultar el catalogo de productos",
                    ex
            );
        }
    }
}
