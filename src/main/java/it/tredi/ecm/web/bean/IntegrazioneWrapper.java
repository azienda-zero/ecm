package it.tredi.ecm.web.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.tredi.ecm.dao.entity.FieldIntegrazioneAccreditamento;
import it.tredi.ecm.dao.enumlist.IdFieldEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntegrazioneWrapper {
	private long accreditamentoId;
	private List<FieldIntegrazioneAccreditamento> fullLista = new ArrayList<FieldIntegrazioneAccreditamento>();
	private Map<IdFieldEnum,FieldIntegrazioneAccreditamento> map = new HashMap<IdFieldEnum, FieldIntegrazioneAccreditamento>();
}
