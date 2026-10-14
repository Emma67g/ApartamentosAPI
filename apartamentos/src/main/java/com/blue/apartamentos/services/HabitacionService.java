package com.blue.apartamentos.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blue.apartamentos.models.HabitacionModel;
import com.blue.apartamentos.models.HabitacionModel.EstadoHabitacion;
import com.blue.apartamentos.models.HabitacionModel.TipoHabitacion;
import com.blue.apartamentos.models.PropiedadModel;
import com.blue.apartamentos.repositories.IDisponibilidadRepository;
import com.blue.apartamentos.repositories.IHabitacionRepository;
import com.blue.apartamentos.repositories.IPropiedadRepository;
import com.blue.apartamentos.repositories.IReservaRepository;

@Service
@Transactional(readOnly = true)
public class HabitacionService {

    private final IHabitacionRepository habitacionRepository;
    private final IPropiedadRepository propiedadRepository;
    private final IReservaRepository reservaRepository;
    private final IDisponibilidadRepository disponibilidadRepository;

    public HabitacionService(IHabitacionRepository habitacionRepository, IPropiedadRepository propiedadRepository,
            IReservaRepository reservaRepository, IDisponibilidadRepository disponibilidadRepository) {
        this.habitacionRepository = habitacionRepository;
        this.propiedadRepository = propiedadRepository;
        this.reservaRepository = reservaRepository;
        this.disponibilidadRepository = disponibilidadRepository;
    }

    public List<HabitacionModel> listar(Long propiedadId, EstadoHabitacion estado, TipoHabitacion tipo) {
        return habitacionRepository.buscar(propiedadId, estado, tipo);
    }

    public HabitacionModel obtener(Long id) {
        return buscar(id);
    }

    @Transactional
    public HabitacionModel crear(HabitacionModel datos) {
        Long propiedadId = Validaciones.exigirId(datos.getPropiedad().getId(), "propiedad.id");
        PropiedadModel propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Propiedad", propiedadId));
        String numero = datos.getNumero().trim();
        if (habitacionRepository.existsByPropiedad_IdAndNumero(propiedadId, numero)) {
            throw new ConflictoException(
                    "La propiedad " + propiedadId + " ya tiene una habitación con el número " + numero + ".");
        }
        HabitacionModel habitacion = new HabitacionModel();
        habitacion.setPropiedad(propiedad);
        aplicar(habitacion, datos, numero);
        return habitacionRepository.save(habitacion);
    }

    @Transactional
    public HabitacionModel actualizar(Long id, HabitacionModel datos) {
        HabitacionModel habitacion = buscar(id);
        Long propiedadId = Validaciones.exigirId(datos.getPropiedad().getId(), "propiedad.id");
        if (!habitacion.getPropiedad().getId().equals(propiedadId)) {
            throw new SolicitudInvalidaException("Una habitación no puede cambiarse a otra propiedad.");
        }
        String numero = datos.getNumero().trim();
        if (habitacionRepository.existsByPropiedad_IdAndNumeroAndIdNot(propiedadId, numero, id)) {
            throw new ConflictoException(
                    "La propiedad " + propiedadId + " ya tiene otra habitación con el número " + numero + ".");
        }
        aplicar(habitacion, datos, numero);
        return habitacionRepository.save(habitacion);
    }

    /** PATCH: del cuerpo solo se usa el campo estado. */
    @Transactional
    public HabitacionModel cambiarEstado(Long id, EstadoHabitacion nuevoEstado) {
        if (nuevoEstado == null) {
            throw new SolicitudInvalidaException(
                    "Debe indicar el estado (DISPONIBLE, NO_DISPONIBLE, MANTENIMIENTO u OCUPADA).");
        }
        HabitacionModel habitacion = buscar(id);
        habitacion.setEstado(nuevoEstado);
        return habitacionRepository.save(habitacion);
    }

    @Transactional
    public void eliminar(Long id) {
        HabitacionModel habitacion = buscar(id);
        if (reservaRepository.existsByHabitacion_Id(id)) {
            throw new ConflictoException("La habitación tiene reservas registradas y no puede eliminarse. "
                    + "Cambie su estado a NO_DISPONIBLE.");
        }
        disponibilidadRepository.deleteByHabitacion_Id(id);
        habitacionRepository.delete(habitacion);
    }

    private HabitacionModel buscar(Long id) {
        return habitacionRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Habitación", id));
    }

    private void aplicar(HabitacionModel h, HabitacionModel datos, String numero) {
        h.setNumero(numero);
        h.setNombre(Validaciones.textoONull(datos.getNombre()));
        h.setTipo(datos.getTipo());
        h.setCapacidad(datos.getCapacidad());
        h.setPrecioNoche(datos.getPrecioNoche());
        h.setNumBanos(datos.getNumBanos());
        h.setMetrosCuadrados(datos.getMetrosCuadrados());
        h.setComodidades(Validaciones.textoONull(datos.getComodidades()));
        h.setDescripcion(Validaciones.textoONull(datos.getDescripcion()));
        if (datos.getEstado() != null) {
            h.setEstado(datos.getEstado());
        }
    }
}
