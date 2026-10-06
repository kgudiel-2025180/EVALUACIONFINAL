package com.Evalucion.Kenny_EVAFINAL.controller;

import com.Evalucion.Kenny_EVAFINAL.dto.common.ApiResponse;
import com.Evalucion.Kenny_EVAFINAL.dto.common.PageResponse;
import com.Evalucion.Kenny_EVAFINAL.dto.libro.LibroRequest;
import com.Evalucion.Kenny_EVAFINAL.dto.libro.LibroResponse;
import com.Evalucion.Kenny_EVAFINAL.service.LibroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Catalogo de libros. La autorizacion por rol esta declarada en
 * {@code SecurityConfig}: escritura solo ADMIN, lectura cualquier rol autenticado.
 */
@RestController
@RequestMapping("/api/v1/libros")
@RequiredArgsConstructor
public class LibroController {

    private final LibroService libroService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LibroResponse>>> listar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String categoria,
            @PageableDefault(size = 10, sort = "titulo", direction = Sort.Direction.ASC) Pageable pageable) {

        PageResponse<LibroResponse> pagina = PageResponse.from(libroService.listar(titulo, categoria, pageable));
        return ResponseEntity.ok(ApiResponse.ok("Catalogo consultado", pagina));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LibroResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Libro obtenido", libroService.obtenerPorId(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LibroResponse>> crear(@Valid @RequestBody LibroRequest request) {
        LibroResponse creado = libroService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Libro registrado correctamente", creado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LibroResponse>> actualizar(@PathVariable Long id,
                                                                 @Valid @RequestBody LibroRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Libro actualizado correctamente",
                libroService.actualizar(id, request)));
    }

    /** 204 NO CONTENT: eliminacion logica que conserva el historial de prestamos. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        libroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
