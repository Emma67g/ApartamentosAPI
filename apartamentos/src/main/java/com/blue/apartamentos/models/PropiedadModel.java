package com.blue.apartamentos.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "propiedades")
public class PropiedadModel {

    public enum TipoPropiedad {
        CASA,
        PISO
    }

    public enum EstadoPropiedad {
        DISPONIBLE,
        NO_DISPONIBLE,
        MANTENIMIENTO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_propiedad")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_propietario", nullable = false)
    @NotNull(message = "El propietario es obligatorio (propietario.id)")
    private ClienteModel propietario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_propiedad", nullable = false, length = 20)
    @NotNull(message = "El tipo de propiedad es obligatorio (CASA o PISO)")
    private TipoPropiedad tipo;

    @Column(name = "titulo", nullable = false, length = 100)
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 100, message = "El título admite máximo 100 caracteres")
    private String titulo;

    @Column(name = "descripcion", length = 500)
    @Size(max = 500, message = "La descripción admite máximo 500 caracteres")
    private String descripcion;

    @Column(name = "direccion", nullable = false, length = 150)
    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 150, message = "La dirección admite máximo 150 caracteres")
    private String direccion;

    @Column(name = "ciudad", nullable = false, length = 50)
    @NotBlank(message = "La ciudad es obligatoria")
    @Size(max = 50, message = "La ciudad admite máximo 50 caracteres")
    private String ciudad;

    @Column(name = "estado_federativa", nullable = false, length = 50)
    @NotBlank(message = "El estado federativo es obligatorio")
    @Size(max = 50, message = "El estado federativo admite máximo 50 caracteres")
    private String estadoFederativa;

    @Column(name = "codigo_postal", nullable = false, length = 5)
    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "\\d{5}", message = "El código postal debe tener 5 dígitos")
    private String codigoPostal;

    @Column(name = "pais", nullable = false, length = 50)
    @Size(max = 50, message = "El país admite máximo 50 caracteres")
    private String pais;

    @Column(name = "latitud")
    @DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90")
    private Double latitud;

    @Column(name = "longitud")
    @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180")
    private Double longitud;

    @Column(name = "precio_noche", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "El precio por noche es obligatorio")
    @Positive(message = "El precio por noche debe ser mayor a cero")
    @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 8 enteros y 2 decimales")
    private BigDecimal precioNoche;

    @Column(name = "capacidad", nullable = false)
    @Min(value = 1, message = "La capacidad mínima es 1 huésped")
    private int capacidad;

    @Column(name = "num_habitaciones", nullable = false)
    @Min(value = 1, message = "Debe tener al menos 1 habitación")
    private int numHabitaciones;

    @Column(name = "num_banos", nullable = false)
    @Min(value = 0, message = "El número de baños no puede ser negativo")
    private int numBanos;

    @Column(name = "metros_cuadrados", nullable = false)
    @Positive(message = "Los metros cuadrados deben ser mayores a cero")
    private int metrosCuadrados;

    @Column(name = "comodidades", length = 300)
    @Size(max = 300, message = "Las comodidades admite máximo 300 caracteres")
    private String comodidades;

    @Column(name = "reglas", length = 300)
    @Size(max = 300, message = "Las reglas admite máximo 300 caracteres")
    private String reglas;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPropiedad estado = EstadoPropiedad.DISPONIBLE;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void onPrePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaRegistro == null) {
            fechaRegistro = ahora;
        }
        fechaActualizacion = ahora;
        if (estado == null) {
            estado = EstadoPropiedad.DISPONIBLE;
        }
    }

    @PreUpdate
    void onPreUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClienteModel getPropietario() {
        return propietario;
    }

    public void setPropietario(ClienteModel propietario) {
        this.propietario = propietario;
    }

    public TipoPropiedad getTipo() {
        return tipo;
    }

    public void setTipo(TipoPropiedad tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getEstadoFederativa() {
        return estadoFederativa;
    }

    public void setEstadoFederativa(String estadoFederativa) {
        this.estadoFederativa = estadoFederativa;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public BigDecimal getPrecioNoche() {
        return precioNoche;
    }

    public void setPrecioNoche(BigDecimal precioNoche) {
        this.precioNoche = precioNoche;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public int getNumHabitaciones() {
        return numHabitaciones;
    }

    public void setNumHabitaciones(int numHabitaciones) {
        this.numHabitaciones = numHabitaciones;
    }

    public int getNumBanos() {
        return numBanos;
    }

    public void setNumBanos(int numBanos) {
        this.numBanos = numBanos;
    }

    public int getMetrosCuadrados() {
        return metrosCuadrados;
    }

    public void setMetrosCuadrados(int metrosCuadrados) {
        this.metrosCuadrados = metrosCuadrados;
    }

    public String getComodidades() {
        return comodidades;
    }

    public void setComodidades(String comodidades) {
        this.comodidades = comodidades;
    }

    public String getReglas() {
        return reglas;
    }

    public void setReglas(String reglas) {
        this.reglas = reglas;
    }

    public EstadoPropiedad getEstado() {
        return estado;
    }

    public void setEstado(EstadoPropiedad estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
