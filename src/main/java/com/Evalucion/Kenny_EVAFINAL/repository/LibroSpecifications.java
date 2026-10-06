package com.Evalucion.Kenny_EVAFINAL.repository;

import com.Evalucion.Kenny_EVAFINAL.entity.Libro;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Especificaciones de busqueda del catalogo.
 *
 * <p>Se usa JPA Specifications en lugar de consultas con parametros anulables
 * ({@code (:param is null or ...)}) porque estas ultimas pueden generar en
 * PostgreSQL parametros de tipo indeterminado.</p>
 */
public final class LibroSpecifications {

    private LibroSpecifications() {
    }

    public static Specification<Libro> conFiltros(String titulo, String categoria) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(criteriaBuilder.isFalse(root.get("eliminado")));

            if (StringUtils.hasText(titulo)) {
                String patron = "%" + titulo.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("titulo")), patron));
            }
            if (StringUtils.hasText(categoria)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("categoria")),
                        categoria.trim().toLowerCase()));
            }
            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
