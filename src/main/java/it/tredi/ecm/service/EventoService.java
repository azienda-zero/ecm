package it.tredi.ecm.service;

import java.util.List;
import java.util.Set;

import it.tredi.ecm.dao.entity.Account;
import it.tredi.ecm.dao.entity.DettaglioAttivitaFAD;
import it.tredi.ecm.dao.entity.DettaglioAttivitaRES;
import it.tredi.ecm.dao.entity.Evento;
import it.tredi.ecm.dao.entity.File;
import it.tredi.ecm.dao.entity.Partner;
import it.tredi.ecm.dao.entity.PersonaEvento;
import it.tredi.ecm.dao.entity.PersonaFullEvento;
import it.tredi.ecm.dao.entity.ProgrammaGiornalieroRES;
import it.tredi.ecm.dao.entity.Sponsor;
import it.tredi.ecm.web.bean.EventoWrapper;

public interface EventoService {
	public Evento getEvento(Long id);
	public Set<Evento> getAllEventiFromProvider(Long providerId);
	public void save(Evento evento);
	public void delete(Long id);

	public void validaRendiconto(Long id, File rendiconto) throws Exception;
	public List<Evento> getAllEventi();
	public Set<Evento> getAllEventiForProviderId(Long providerId);
	public boolean canCreateEvento(Account account);
	public void inviaRendicontoACogeaps(Long id) throws Exception;
	public void statoElaborazioneCogeaps(Long id) throws Exception;
	public Evento handleRipetibiliAndAllegati(EventoWrapper eventoWrapper) throws Exception;
	public EventoWrapper prepareRipetibiliAndAllegati(EventoWrapper eventoWrapper);


	public void calculateAutoCompilingData(EventoWrapper eventoWrapper) throws Exception;
//	public float calcoloDurataEvento(EventoWrapper eventoWrapper);
//	public float calcoloCreditiEvento(EventoWrapper eventoWrapper);

	//TODO Questo metodo puo' diventare private
	public void retrieveProgrammaAndAddJoin(EventoWrapper eventoWrapper);
	public void aggiornaDati(EventoWrapper eventoWrapper);
	public Set<Evento> getAllEventiRieditabiliForProviderId(Long providerId);

	//TODO chiedere 1 mese di ferie almeno (joe19 mode on)
	public Evento prepareRiedizioneEvento(Evento evento) throws Exception;
	public int getLastEdizioneEventoByPrefix(String prefix);
	public List<PersonaEvento> copyPersonaListEvento(List<PersonaEvento> responsabili) throws CloneNotSupportedException;
	public PersonaFullEvento copyPersonaFullEvento(PersonaFullEvento responsabileSegreteria);
	public Set<Sponsor> copySponsorListEvento(Set<Sponsor> sponsors);
	public Set<Partner> copyPartnerListEvento(Set<Partner> partners);
	public List<DettaglioAttivitaFAD> copyProgrammaEventoFAD(List<DettaglioAttivitaFAD> programmaFAD, List<PersonaEvento> docentiEventoRieditato);

}
