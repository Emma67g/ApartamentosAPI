package com.blue.apartamentos.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "clientes")
public class ClienteModel {

    public enum TipoCliente {
        PROPIETARIO,
        CLIENTE,
        EMPRESA
    }

    public enum ClienteStatus {
        ACTIVO,
        INACTIVO,
        SUSPENDIDO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cliente", nullable = false, length = 20)
    @NotNull(message = "El tipo de cliente es obligatorio (PROPIETARIO, CLIENTE o EMPRESA)")
    private TipoCliente tipo = TipoCliente.CLIENTE;

    @Column(name = "nombre", nullable = false, length = 35)
    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 35, message = "El campo nombres admite máximo 35 caracteres")
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 35)
    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 35, message = "El campo apellidos admite máximo 35 caracteres")
    private String apellidos;

    @Column(name = "email", unique = true, nullable = false, length = 50)
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 50, message = "El email admite máximo 50 caracteres")
    private String email;

    @Column(name = "telefono", length = 15)
    @Pattern(regexp = "^[0-9+()\\-\\s]{7,15}$", message = "El teléfono debe tener entre 7 y 15 caracteres (dígitos, +, -, paréntesis)")
    private String telefono;

    @Column(name = "fecha_nacimiento")
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate fechaNacimiento;

    @Column(name = "numero_ine", unique = true, length = 20)
    @Size(max = 20, message = "El INE admite máximo 20 caracteres")
    private String ine;

    @Column(name = "direccion", length = 100)
    @Size(max = 100, message = "La dirección admite máximo 100 caracteres")
    private String direccion;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "estatus", nullable = false, length = 20)
    private ClienteStatus estatus = ClienteStatus.ACTIVO;

    @PrePersist
    void onPrePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
        if (estatus == null) {
            estatus = ClienteStatus.ACTIVO;
        }
    }

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoCliente getTipo() {
        return tipo;
    }

    public void setTipo(TipoCliente tipo) {
        this.tipo = tipo;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getIne() {
        return ine;
    }

    public void setIne(String ine) {
        this.ine = ine;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public ClienteStatus getEstatus() {
        return estatus;
    }

    public void setEstatus(ClienteStatus estatus) {
        this.estatus = estatus;
    }
}
