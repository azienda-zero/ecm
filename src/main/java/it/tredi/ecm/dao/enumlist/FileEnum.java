package it.tredi.ecm.dao.enumlist;

import lombok.Getter;

@Getter
public enum FileEnum {
	FILE_DELEGA (1, "Delega"),
	FILE_CV (2, "CV"),
	FILE_ATTO_NOMINA (3, "Atto di nomina"),
	FILE_ESTRATTO_BILANCIO_FORMAZIONE (4, "Estratto del bilancio relativo alla formazione"),
	FILE_ESTRATTO_BILANCIO_COMPLESSIVO (5, "Estratto del bilancio complessivo degli ultimi 3 anni"),
	FILE_FUNZIONIGRAMMA (7, "Funzionigramma"),
	FILE_ORGANIGRAMMA (8, "Organigramma"),
	FILE_ATTO_COSTITUTIVO (9, "Atto Costitutivo e statuto"),
	FILE_ESPERIENZA_FORMAZIONE (10, "Esperienza formazione in ambito sanitario"),
	FILE_UTILIZZO (11, "Utilizzo di sedi, strutture ed attrezzature di altro soggetto"),
	FILE_SISTEMA_INFORMATICO (12, "Sistema informatico dedicato alla formazione"),
	FILE_PIANO_QUALITA (13, "Piano di Qualità"),
	FILE_DICHIARAZIONE_LEGALE (14, "Dichiarazione del Legale Rappresentante attestante la veridicità della documentazione"),
	FILE_MODELLO_ATTO_COSTITUTIVO (15, ""),
	FILE_MODELLO_ESPERIENZA_FORMAZIONE (16, ""),
	FILE_MODELLO_UTILIZZO (17, ""),
	FILE_MODELLO_SISTEMA_INFORMATICO (18, ""),
	FILE_MODELLO_PIANO_QUALITA (19, ""),
	FILE_MODELLO_DICHIARAZIONE_LEGALE (20, ""),
	FILE_DICHIARAZIONE_ESCLUSIONE (21, ""),
	FILE_VERBALE_VALUTAZIONE_SUL_CAMPO (22, "Verbale della valutazione sul campo"),

	FILE_NOTE_OSSERVAZIONI_INTEGRAZIONE (23, "Note e osservazioni integrazione"),
	FILE_NOTE_OSSERVAZIONI_PREAVVISO_RIGETTO (24, "Note e osservazioni preavviso rigetto"),

	FILE_ALLEGATO_COMUNICAZIONE (25, "Allegato Comunicazione"),
	FILE_ALLEGATO_RISPOSTA (26, "Allegato Risposta"),

	FILE_ACCREDITAMENTO_PROVVISORIO_DECRETO_ACCREDITAMENTO (27, "Decreto accreditamento provvisorio"),
	FILE_ACCREDITAMENTO_PROVVISORIO_DECRETO_DINIEGO (28, "Decreto diniego accreditamento provvisorio"),
	FILE_ACCREDITAMENTO_PROVVISORIO_INTEGRAZIONE (29, "Integrazione accreditamento provvisorio"),
	FILE_ACCREDITAMENTO_PROVVISORIO_PREAVVISO_RIGETTO (30, "Preavviso rigetto accreditamento provvisorio"),

	//ENGINEERING TEST FILE
	FILE_DA_FIRMARE(99, "");

	private int id;
	private String nome;

	private FileEnum(int id, String nome){
		this.id = id;
		this.nome = nome;
	}
}
