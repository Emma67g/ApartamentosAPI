package com.blue.apartamentos.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blue.apartamentos.models.ClienteModel;
import com.blue.apartamentos.models.ClienteModel.ClienteStatus;
import com.blue.apartamentos.models.ClienteModel.TipoCliente;
import com.blue.apartamentos.models.PropiedadModel;
import com.blue.apartamentos.models.PropiedadModel.EstadoPropiedad;
import com.blue.apartamentos.models.PropiedadModel.TipoPropiedad;
import com.blue.apartamentos.repositories.IClienteRepository;
import com.blue.apartamentos.repositories.IDisponibilidadRepository;
import com.blue.apartamentos.repositories.IHabitacionRepository;
import com.blue.apartamentos.repositories.IImagenPropiedadRepository;
import com.blue.apartamentos.repositories.IPropiedadRepository;
import com.blue.apartamentos.repositories.IReservaRepository;

@Service
@Transactional(readOnly = true)
public class PropiedadService {

    private static final String PAIS_POR_DEFECTO = "México";

    private final IPropiedadRepository propiedadRepository;
    private final IClienteRepository clienteRepository;
    private final IHabitacionRepository habitacionRepository;
    private final IReservaRepository reservaRepository;
    private final IImagenPropiedadRepository imagenRepository;
    private final IDisponibilidadRepository disponibilidadRepository;

    public PropiedadService(IPropiedadRepository propiedadRepository, IClienteRepository clienteRepository,
            IHabitacionRepository habitacionRepository, IReservaRepository reservaRepository,
            IImagenPropiedadRepository imagenRepository, IDisponibilidadRepository disponibilidadRepository) {
        this.propiedadRepository = propiedadRepository;
        this.clienteRepository = clienteRepository;
        this.habitacionRepository = habitacionRepository;
        this.reservaRepository = reservaRepository;
        this.imagenRepository = imagenRepository;
        this.disponibilidadRepository = disponibilidadRepository;
    }

    public List<PropiedadModel> listar(TipoPropiedad tipo, Long propietarioId, String ciudad,
            EstadoPropiedad estado) {
        return propiedadRepository.buscar(tipo, propietarioId, Validaciones.textoONull(ciudad), estado);
    }

    public PropiedadModel obtener(Long id) {
        return buscar(id);
    }

    @Transactional
    public PropiedadModel crear(PropiedadModel datos) {
        PropiedadModel propiedad = new PropiedadModel();
        propiedad.setPropietario(obtenerPropietarioValido(datos));
        aplicar(propiedad, datos);
        return propiedadRepository.save(propiedad);
    }

    @Transactional
    public PropiedadModel actualizar(Long id, PropiedadModel datos) {
        PropiedadModel propiedad = buscar(id);
        Long propietarioId = Validaciones.exigirId(datos.getPropietario().getId(), "propietario.id");
        if (!propiedad.getPropietario().getId().equals(propietarioId)) {
            propiedad.setPropietario(obtenerPropietarioValido(datos));
        }
        aplicar(propiedad, datos);
        return propiedadRepository.save(propiedad);
    }

    @Transactional
    public void eliminar(Long id) {
        PropiedadModel propiedad = buscar(id);
        if (habitacionRepository.existsByPropiedad_Id(id) || reservaRepository.existsByPropiedad_Id(id)) {
            throw new ConflictoException("La propiedad tiene habitaciones o reservas registradas y no puede "
                    + "eliminarse. Cambie su estado a NO_DISPONIBLE.");
        }
        imagenRepository.deleteByPropiedad_Id(id);
        disponibilidadRepository.deleteByPropiedad_Id(id);
        propiedadRepository.delete(propiedad);
    }

    private PropiedadModel buscar(Long id) {
        return propiedadRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Propiedad", id));
    }

    private ClienteModel obtenerPropietarioValido(PropiedadModel datos) {
        Long propietarioId = Validaciones.exigirId(datos.getPropietario().getId(), "propietario.id");
        ClienteModel propietario = clienteRepository.findById(propietarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente (propietario)", propietarioId));
        if (propietario.getTipo() == TipoCliente.CLIENTE) {
            throw new SolicitudInvalidaException(
                    "El cliente " + propietarioId + " es de tipo CLIENTE; solo un PROPIETARIO o una EMPRESA "
                            + "puede registrar propiedades.");
        }
        if (propietario.getEstatus() != ClienteStatus.ACTIVO) {
            throw new SolicitudInvalidaException(
                    "El propietario está " + propietario.getEstatus() + " y no puede registrar propiedades.");
        }
        return propietario;
    }

    /** Copia solo los campos editables (el propietario se asigna aparte). */
    private void aplicar(PropiedadModel p, PropiedadModel datos) {
        p.setTipo(datos.getTipo());
        p.setTitulo(datos.getTitulo().trim());
        p.setDescripcion(Validaciones.textoONull(datos.getDescripcion()));
        p.setDireccion(datos.getDireccion().trim());
        p.setCiudad(datos.getCiudad().trim());
        p.setEstadoFederativa(datos.getEstadoFederativa().trim());
        p.setCodigoPostal(datos.getCodigoPostal());
        String pais = Validaciones.textoONull(datos.getPais());
        p.setPais(pais != null ? pais : PAIS_POR_DEFECTO);
        p.setLatitud(datos.getLatitud());
        p.setLongitud(datos.getLongitud());
        p.setPrecioNoche(datos.getPrecioNoche());
        p.setCapacidad(datos.getCapacidad());
        p.setNumHabitaciones(datos.getNumHabitaciones());
        p.setNumBanos(datos.getNumBanos());
        p.setMetrosCuadrados(datos.getMetrosCuadrados());
        p.setComodidades(Validaciones.textoONull(datos.getComodidades()));
        p.setReglas(Validaciones.textoONull(datos.getReglas()));
        if (datos.getEstado() != null) {
            p.setEstado(datos.getEstado());
        }
    }
}
