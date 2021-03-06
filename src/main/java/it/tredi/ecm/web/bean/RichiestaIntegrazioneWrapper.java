package it.tredi.ecm.web.bean;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import it.tredi.ecm.dao.entity.FieldEditabileAccreditamento;
import it.tredi.ecm.dao.enumlist.IdFieldEnum;
import it.tredi.ecm.dao.enumlist.SubSetFieldEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RichiestaIntegrazioneWrapper {
	private long accreditamentoId;
	private SubSetFieldEnum subset;
	private Long objRef;
	private Set<FieldEditabileAccreditamento> fullLista = new HashSet<FieldEditabileAccreditamento>();
	private Set<IdFieldEnum> selected = new HashSet<IdFieldEnum>();

	//Mappa per gestire l'inserimento delle note relative ai FieldEditabileAccreditamento
	private Map<IdFieldEnum, String> mappaNoteFieldEditabileAccreditamento = new HashMap<IdFieldEnum, String>();
}
