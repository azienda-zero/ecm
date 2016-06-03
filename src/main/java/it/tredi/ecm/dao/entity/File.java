package it.tredi.ecm.dao.entity;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import it.tredi.ecm.dao.enumlist.Costanti;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="file")
@Getter
@Setter
public class File extends BaseEntity {
	private String nomeFile;
	private byte[] data;
	
	@Column(name = "creato")
	private LocalDate dataCreazione;
	
	private String tipo;
	@ManyToOne
	private Persona persona;
	//TODO probabilmente non serve...esiste una sola persona per provider
	// avrebbe senso solo in ottica di ottimizzazione delle performance
	@ManyToOne
	private Provider provider;
	
	public File(){
		this.tipo = "";
		this.nomeFile = "";
	}
	
	public boolean isCV(){
		return this.tipo.equals(Costanti.FILE_CV);
	}
	public boolean isDELEGA(){
		return this.tipo.equals(Costanti.FILE_DELEGA);
	}
	public boolean isATTONOMINA(){
		return this.tipo.equals(Costanti.FILE_ATTO_NOMINA);
	}
	public boolean isESTRATTOBILANCIOFORMAZIONE(){
		return this.tipo.equals(Costanti.FILE_ESTRATTO_BILANCIO_FORMAZIONE);
	}
	public boolean isBUDGETPREVISIONALE(){
		return this.tipo.equals(Costanti.FILE_BUDGET_PREVISIONALE);
	}
	
}
