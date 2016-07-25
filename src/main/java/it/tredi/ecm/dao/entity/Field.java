package it.tredi.ecm.dao.entity;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import it.tredi.ecm.dao.enumlist.IdFieldEnum;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class Field extends BaseEntity{
	@ManyToOne
	private Accreditamento accreditamento;
	
	@Enumerated(EnumType.STRING)
	private IdFieldEnum idField;
	private long objectReference = -1;
}