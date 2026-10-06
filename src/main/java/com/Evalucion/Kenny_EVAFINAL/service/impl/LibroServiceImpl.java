package com.Evalucion.Kenny_EVAFINAL.service.impl;

import com.Evalucion.Kenny_EVAFINAL.dto.libro.LibroRequest;
import com.Evalucion.Kenny_EVAFINAL.dto.libro.LibroResponse;
import com.Evalucion.Kenny_EVAFINAL.entity.Libro;
import com.Evalucion.Kenny_EVAFINAL.exception.ConflictException;
import com.Evalucion.Kenny_EVAFINAL.exception.ResourceNotFoundException;
import com.Evalucion.Kenny_EVAFINAL.mapper.LibroMapper;
import com.Evalucion.Kenny_EVAFINAL.repository.LibroRepository;
import com.Evalucion.Kenny_EVAFINAL.repository.LibroSpecifications;
import com.Evalucion.Kenny_EVAFINAL.service.LibroService;
import com.Evalucion.Kenny_EVAFINAL.util.PageUtils;
import com.Evalucion.Kenny_EVAFINAL.util.StockValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LibroServiceImpl implements LibroService {

    private static final Sort ORDEN_POR_DEFECTO = Sort.by(Sort.Direction.ASC, "titulo");

    private final LibroRepository libroRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<LibroResponse> listar(String titulo, String categoria, Pageable pageable) {
        Pageable normalizado = PageUtils.normalizar(pageable, ORDEN_POR_DEFECTO);
        Specification<Libro> filtros = LibroSpecifications.conFiltros(titulo, categoria);
        return libroRepository.findAll(filtros, normalizado).map(LibroMapper::aResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public LibroResponse obtenerPorId(Long id) {
        return LibroMapper.aResponse(buscarActivo(id));
    }

    @Override
    @Transactional
    public LibroResponse crear(LibroRequest request) {
        String isbn = normalizarIsbn(request.isbn());
        validarIsbnUnico(isbn, null);

        Libro libro = Libro.builder()
                .isbn(isbn)
                .titulo(request.titulo().trim())
                .autor(request.autor().trim())
                .categoria(request.categoria().trim())
                .stockTotal(request.stockTotal())
                .stockDisponible(request.stockDisponible())
                .eliminado(false)
                .build();

        // REGLA 8 antes de persistir
        StockValidator.validar(libro);

        Libro guardado = libroRepository.save(libro);
        log.info("Libro creado: #{} ({}) stock={}/{}",
                guardado.getId(), guardado.getIsbn(), guardado.getStockDisponible(), guardado.getStockTotal());
        return LibroMapper.aResponse(guardado);
    }

    @Override
    @Transactional
    public LibroResponse actualizar(Long id, LibroRequest request) {
        Libro libro = buscarActivo(id);

        String isbn = normalizarIsbn(request.isbn());
        validarIsbnUnico(isbn, id);

        libro.setIsbn(isbn);
        libro.setTitulo(request.titulo().trim());
        libro.setAutor(request.autor().trim());
        libro.setCategoria(request.categoria().trim());
        libro.setStockTotal(request.stockTotal());
        libro.setStockDisponible(request.stockDisponible());

        // REGLA 8: nunca stockDisponible > stockTotal ni valores negativos
        StockValidator.validar(libro);

        Libro actualizado = libroRepository.save(libro);
        log.info("Libro actualizado: #{} stock={}/{}",
                actualizado.getId(), actualizado.getStockDisponible(), actualizado.getStockTotal());
        return LibroMapper.aResponse(actualizado);
    }

    /**
     * Eliminacion <b>logica</b>: se marca {@code eliminado=true} y desaparece del
     * catalogo, pero las filas de prestamos historicos siguen apuntando al libro.
     * Una eliminacion fisica romperia la integridad referencial del historial.
     */
    @Override
    @Transactional
    public void eliminar(Long id) {
        Libro libro = buscarActivo(id);
        libro.setEliminado(true);
        libroRepository.save(libro);
        log.info("Libro #{} ({}) eliminado logicamente", libro.getId(), libro.getIsbn());
    }

    private Libro buscarActivo(Long id) {
        return libroRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro", id));
    }

    /** REGLA: el ISBN es unico; la comprobacion se hace en Java y en la BD. */
    private void validarIsbnUnico(String isbn, Long idExcluido) {
        boolean existe = (idExcluido == null)
                ? libroRepository.existsByIsbn(isbn)
                : libroRepository.existsByIsbnAndIdNot(isbn, idExcluido);
        if (existe) {
            throw new ConflictException("Ya existe un libro con el ISBN " + isbn);
        }
    }

    private String normalizarIsbn(String isbn) {
        return isbn == null ? null : isbn.trim();
    }
}
