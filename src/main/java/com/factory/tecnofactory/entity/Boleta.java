package com.factory.tecnofactory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boleta")
@Getter
@Setter
@NoArgsConstructor
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_boleta")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "total_boleta", nullable = false)
    private Long totalBoleta;

    // true = vigente, false = anulada
    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    // Sin orphanRemoval: una boleta emitida no pierde líneas
    @OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    private List<DetalleBoleta> detalles = new ArrayList<>();
}
