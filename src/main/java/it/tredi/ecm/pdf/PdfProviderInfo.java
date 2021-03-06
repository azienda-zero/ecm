package it.tredi.ecm.pdf;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import it.tredi.ecm.dao.entity.Persona;
import it.tredi.ecm.dao.entity.Provider;
import it.tredi.ecm.dao.entity.Sede;
import it.tredi.ecm.dao.enumlist.Ruolo;

public class PdfProviderInfo {
	private String providerDenominazione = null;
	private String providerIndirizzo = null;
	private String providerCap = null;
	private String providerComune = null;
	private String providerProvincia = null;
	private String providerNomeLegaleRappresentante = null;
	private String providerCognomeLegaleRappresentante = null;
	private String providerPec = null;
	private String providerId = null;

	public PdfProviderInfo(Provider provider) {
			this.providerDenominazione = provider.getDenominazioneLegale();
			this.providerId = provider.getId().toString();
			for(Sede sede : provider.getSedi()) {
				if(sede.isSedeLegale()) {
					this.providerIndirizzo = sede.getIndirizzo();
					this.providerCap= sede.getCap();
					this.providerComune = sede.getComune();
					this.providerProvincia = sede.getProvincia();
				}
			}
			Persona legaleRappresentante = provider.getPersonaByRuolo(Ruolo.LEGALE_RAPPRESENTANTE);
			if(legaleRappresentante != null) {
				this.providerNomeLegaleRappresentante = legaleRappresentante.getAnagrafica().getNome();
				this.providerCognomeLegaleRappresentante = legaleRappresentante.getAnagrafica().getCognome();
				this.providerPec = legaleRappresentante.getAnagrafica().getPec();
			}
		}
	
	public PdfProviderInfo(String providerDenominazione,
		String providerIndirizzo,
		String providerCap,
		String providerComune,
		String providerProvincia,
		String providerNomeLegaleRappresentante,
		String providerCognomeLegaleRappresentante,
		String providerPec) {
		this.providerDenominazione = providerDenominazione;
		this.providerIndirizzo = providerIndirizzo;
		this.providerCap= providerCap;
		this.providerComune = providerComune;
		this.providerProvincia = providerProvincia;
		this.providerNomeLegaleRappresentante = providerNomeLegaleRappresentante;
		this.providerCognomeLegaleRappresentante = providerCognomeLegaleRappresentante;
		this.providerPec = providerPec;
	}

	public PdfProviderInfo(String providerDenominazione,
			String providerIndirizzo,
			String providerCap,
			String providerComune,
			String providerProvincia,
			String providerNomeLegaleRappresentante,
			String providerCognomeLegaleRappresentante,
			String providerPec,
			String providerId) {
			this.providerDenominazione = providerDenominazione;
			this.providerIndirizzo = providerIndirizzo;
			this.providerCap= providerCap;
			this.providerComune = providerComune;
			this.providerProvincia = providerProvincia;
			this.providerNomeLegaleRappresentante = providerNomeLegaleRappresentante;
			this.providerCognomeLegaleRappresentante = providerCognomeLegaleRappresentante;
			this.providerPec = providerPec;
			this.providerId = providerId;
	}

	public String getProviderDenominazione() {
		return providerDenominazione;
	}

	public void setProviderDenominazione(String providerDenominazione) {
		this.providerDenominazione = providerDenominazione;
	}

	public String getProviderIndirizzo() {
		return providerIndirizzo;
	}

	public void setProviderIndirizzo(String providerIndirizzo) {
		this.providerIndirizzo = providerIndirizzo;
	}

	public String getProviderCap() {
		return providerCap;
	}

	public void setProviderCap(String providerCap) {
		this.providerCap = providerCap;
	}

	public String getProviderComune() {
		return providerComune;
	}

	public void setProviderComune(String providerComune) {
		this.providerComune = providerComune;
	}

	public String getProviderProvincia() {
		return providerProvincia;
	}

	public void setProviderProvincia(String providerProvincia) {
		this.providerProvincia = providerProvincia;
	}

	public String getProviderNomeLegaleRappresentante() {
		return providerNomeLegaleRappresentante;
	}

	public void setProviderNomeLegaleRappresentante(
			String providerNomeLegaleRappresentante) {
		this.providerNomeLegaleRappresentante = providerNomeLegaleRappresentante;
	}

	public String getProviderCognomeLegaleRappresentante() {
		return providerCognomeLegaleRappresentante;
	}

	public void setProviderCognomeLegaleRappresentante(
			String providerCognomeLegaleRappresentante) {
		this.providerCognomeLegaleRappresentante = providerCognomeLegaleRappresentante;
	}

	public String getProviderPec() {
		return providerPec;
	}

	public void setProviderPec(String providerPec) {
		this.providerPec = providerPec;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

}
