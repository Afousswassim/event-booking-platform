package com.eventbooking.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entité représentant un ticket numérique unique associé à une réservation.
 */
@Entity
@Table(name = "tickets")
public class Ticket implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_unique", nullable = false, unique = true, length = 100)
    private String codeUnique;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus statut = TicketStatus.VALIDE;

    @Column(name = "date_utilisation")
    private LocalDateTime dateUtilisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    public Ticket() {
    }

    public Ticket(String codeUnique, Reservation reservation) {
        this.codeUnique = codeUnique;
        this.reservation = reservation;
        this.statut = TicketStatus.VALIDE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodeUnique() {
        return codeUnique;
    }

    public void setCodeUnique(String codeUnique) {
        this.codeUnique = codeUnique;
    }

    public TicketStatus getStatut() {
        return statut;
    }

    public void setStatut(TicketStatus statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateUtilisation() {
        return dateUtilisation;
    }

    public void setDateUtilisation(LocalDateTime dateUtilisation) {
        this.dateUtilisation = dateUtilisation;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ticket ticket)) return false;
        return Objects.equals(id, ticket.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "id=" + id +
                ", codeUnique='" + codeUnique + '\'' +
                ", statut=" + statut +
                ", dateUtilisation=" + dateUtilisation +
                '}';
    }
}
