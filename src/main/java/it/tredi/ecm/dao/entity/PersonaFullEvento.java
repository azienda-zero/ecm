package it.tredi.ecm.dao.entity;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.tredi.ecm.dao.enumlist.RuoloPersonaEventoEnum;
import it.tredi.ecm.utils.Utils;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PersonaFullEvento extends BaseEntity{
	private static Logger LOGGER = LoggerFactory.getLogger(PersonaFullEvento.class);
	
	@Embedded
	private AnagraficaFullEventoBase anagrafica;
	
	@ManyToOne
	private Evento eventoResponsabileSegreteriaOrganizzativa;
	
	public PersonaFullEvento() {
		
	}
	
	public PersonaFullEvento(AnagraficaFullEvento anagrafica){
		try{
			this.anagrafica = (AnagraficaFullEventoBase) anagrafica.getAnagrafica().clone();
		}catch (Exception ex){
			LOGGER.error(Utils.getLogMessage("Errore cast AnagraficaFullEventoBase"), ex);
		}
	}
}