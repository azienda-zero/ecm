package it.tredi.ecm.dao.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ProgrammaGiornalieroRES extends BaseEntityDefaultId {
	//@ManyToOne
	//private EventoRES eventoRES;

	@DateTimeFormat (pattern = "dd/MM/yyyy")
	private LocalDate giorno;
	private SedeEvento sede;
	@OneToMany(cascade=CascadeType.ALL, orphanRemoval=true)
	@OrderBy("orarioInizio ASC")
	@JoinColumn(name = "programma_giornaliero_res_id")
	private List<DettaglioAttivitaRES> programma = new ArrayList<DettaglioAttivitaRES>();

}
