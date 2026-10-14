package com.blue.apartamentos.services;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blue.apartamentos.models.ClienteModel;
import com.blue.apartamentos.models.ClienteModel.ClienteStatus;
import com.blue.apartamentos.models.ClienteModel.TipoCliente;
import com.blue.apartamentos.repositories.IClienteRepository;
import com.blue.apartamentos.repositories.IPropiedadRepository;
import com.blue.apartamentos.repositories.IReservaRepository;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final IClienteRepository clienteRepository;
    private final IPropiedadRepository propiedadRepository;
    private final IReservaRepository reservaRepository;

    public ClienteService(IClienteRepository clienteRepository, IPropiedadRepository propiedadRepository,
            IReservaRepository reservaRepository) {
        this.clienteRepository = clienteRepository;
        this.propiedadRepository = propiedadRepository;
        this.reservaRepository = reservaRepository;
    }

    public List<ClienteModel> listar(TipoCliente tipo, ClienteStatus estatus) {
        return clienteRepository.buscar(tipo, estatus);
    }

    public ClienteModel obtener(Long id) {
        return buscar(id);
    }

    @Transactional
    public ClienteModel crear(ClienteModel datos) {
        String email = datos.getEmail().trim().toLowerCase(Locale.ROOT);
        String ine = normalizarIne(datos.getIne());
        if (clienteRepository.existsByEmail(email)) {
            throw new ConflictoException("Ya existe un cliente con el email " + email + ".");
        }
        if (ine != null && clienteRepository.existsByIne(ine)) {
            throw new ConflictoException("Ya existe un cliente con el INE " + ine + ".");
        }
        ClienteModel cliente = new ClienteModel();
        aplicar(cliente, datos, email, ine);
        return clienteRepository.save(cliente);
    }

    @Transactional
    public ClienteModel actualizar(Long id, ClienteModel datos) {
        ClienteModel cliente = buscar(id);
        String email = datos.getEmail().trim().toLowerCase(Locale.ROOT);
        String ine = normalizarIne(datos.getIne());
        if (clienteRepository.existsByEmailAndIdNot(email, id)) {
            throw new ConflictoException("Ya existe otro cliente con el email " + email + ".");
        }
        if (ine != null && clienteRepository.existsByIneAndIdNot(ine, id)) {
            throw new ConflictoException("Ya existe otro cliente con el INE " + ine + ".");
        }
        aplicar(cliente, datos, email, ine);
        return clienteRepository.save(cliente);
    }

    @Transactional
    public void eliminar(Long id) {
        ClienteModel cliente = buscar(id);
        if (propiedadRepository.existsByPropietario_Id(id) || reservaRepository.existsByCliente_Id(id)) {
            throw new ConflictoException("El cliente tiene propiedades o reservas registradas y no puede eliminarse. "
                    + "Cambie su estatus a INACTIVO.");
        }
        clienteRepository.delete(cliente);
    }

    private ClienteModel buscar(Long id) {
        return clienteRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }

    /** Copia solo los campos editables: id, fecha de registro y demás datos del servidor nunca vienen del cliente. */
    private void aplicar(ClienteModel cliente, ClienteModel datos, String email, String ine) {
        cliente.setTipo(datos.getTipo());
        cliente.setNombres(datos.getNombres().trim());
        cliente.setApellidos(datos.getApellidos().trim());
        cliente.setEmail(email);
        cliente.setTelefono(Validaciones.textoONull(datos.getTelefono()));
        cliente.setFechaNacimiento(datos.getFechaNacimiento());
        cliente.setIne(ine);
        cliente.setDireccion(Validaciones.textoONull(datos.getDireccion()));
        if (datos.getEstatus() != null) {
            cliente.setEstatus(datos.getEstatus());
        }
    }

    private String normalizarIne(String ine) {
        String limpio = Validaciones.textoONull(ine);
        return limpio == null ? null : limpio.toUpperCase(Locale.ROOT);
    }
}
