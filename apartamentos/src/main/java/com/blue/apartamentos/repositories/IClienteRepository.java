package com.blue.apartamentos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.blue.apartamentos.models.ClienteModel;
import com.blue.apartamentos.models.ClienteModel.ClienteStatus;
import com.blue.apartamentos.models.ClienteModel.TipoCliente;

@Repository
public interface IClienteRepository extends JpaRepository<ClienteModel, Long> {

    @Query("""
            SELECT c FROM ClienteModel c
            WHERE (:tipo IS NULL OR c.tipo = :tipo)
              AND (:estatus IS NULL OR c.estatus = :estatus)
            ORDER BY c.apellidos, c.nombres
            """)
    List<ClienteModel> buscar(@Param("tipo") TipoCliente tipo, @Param("estatus") ClienteStatus estatus);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByIne(String ine);

    boolean existsByIneAndIdNot(String ine, Long id);
}
