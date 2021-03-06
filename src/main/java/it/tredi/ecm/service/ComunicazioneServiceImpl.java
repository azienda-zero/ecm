package it.tredi.ecm.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import it.tredi.ecm.dao.entity.Account;
import it.tredi.ecm.dao.entity.Accreditamento;
import it.tredi.ecm.dao.entity.Comunicazione;
import it.tredi.ecm.dao.entity.ComunicazioneResponse;
import it.tredi.ecm.dao.entity.File;
import it.tredi.ecm.dao.entity.Persona;
import it.tredi.ecm.dao.entity.Provider;
import it.tredi.ecm.dao.enumlist.ProfileEnum;
import it.tredi.ecm.dao.repository.ComunicazioneRepository;
import it.tredi.ecm.dao.repository.ComunicazioneResponseRepository;
import it.tredi.ecm.utils.Utils;
import it.tredi.ecm.web.bean.RicercaComunicazioneWrapper;

@Service
public class ComunicazioneServiceImpl implements ComunicazioneService {
	private static Logger LOGGER = LoggerFactory.getLogger(ComunicazioneServiceImpl.class);

	@Autowired private AccountService accountService;
	@Autowired private ComunicazioneRepository comunicazioneRepository;
	@Autowired private ComunicazioneResponseRepository comunicazioneResponseRepository;
	@Autowired private EmailService emailService;
	@Autowired private AccreditamentoService accreditamentoService;
	@PersistenceContext EntityManager entityManager;

	@Override
	public Comunicazione getComunicazioneById(Long id) {
		return comunicazioneRepository.findOne(id);
	}

	@Override
	public int countAllComunicazioniRicevuteByAccountId(Long id) {
		Account user = accountService.getUserById(id);
		if(user.isProviderVisualizzatore()) {
			Account providerComunicazioni = accountService.getAccountComunicazioniProviderForProvider(user.getProvider());
			return comunicazioneRepository.countAllComunicazioniRicevuteByAccount(providerComunicazioni);
		}
		if(!user.isSegreteria()) {
			return comunicazioneRepository.countAllComunicazioniRicevuteByAccount(user);
		}
		else {
			//l'utente è anche segreteria quindi amplio la query con le comunicazioni inviate alla segreteria
			Account segreteriaComunicazioni = accountService.getAccountComunicazioniSegretereria();
			return comunicazioneRepository.countAllComunicazioniRicevuteByAccountOrBySegreteria(user, segreteriaComunicazioni);
		}
	}

	@Override
	public int countAllComunicazioniInviateByAccountId(Long id) {
		Account user = accountService.getUserById(id);
		Account accountComunicazioni = null;
		if(user.isProviderVisualizzatore()) {
			accountComunicazioni = accountService.getAccountComunicazioniProviderForProvider(user.getProvider());
		}
		else {
			//user appartiene alla segreteria dal momento che gli altri utenti possono aprire comunicazioni solo verso la segreteria
			accountComunicazioni = accountService.getAccountComunicazioniSegretereria();
		}
		//non ci sono altri casi poichè le altre tipologia di utenti non possono inviare comunicazioni, ma solo riceverle
		return comunicazioneRepository.countAllComunicazioniInviateForAccount(accountComunicazioni);
	}

	@Override
	public int countAllComunicazioniChiuseByAccountId(Long id) {
		Account user = accountService.getUserById(id);
		if(user.isProviderVisualizzatore()) {
			Account providerComunicazioni = accountService.getAccountComunicazioniProviderForProvider(user.getProvider());
			return comunicazioneRepository.countAllComunicazioniChiuseByGroup(providerComunicazioni);
		}
		if(!user.isSegreteria()) {
			return comunicazioneRepository.countAllComunicazioniChiuseByAccount(user);
		}
		else {
			//l'utente è anche segreteria quindi amplio la query con le comunicazioni inviate alla segreteria
			//controllo se l'utente è nei destinatari o è il mittente, se il fake della segreteria è nei destinatari o se è stato inviata una comunicazione per suo conto;
			Account segreteriaComunicazioni = accountService.getAccountComunicazioniSegretereria();
			return comunicazioneRepository.countAllComunicazioniChiuseByAccountOrByGroup(user, segreteriaComunicazioni);
		}
	}

	@Override
	public int countAllComunicazioniStoricoByAccountId(Long id) {
		Account user = accountService.getUserById(id);
		if(user.isProviderVisualizzatore()) {
			Account providerComunicazioni = accountService.getAccountComunicazioniProviderForProvider(user.getProvider());
			return comunicazioneRepository.countAllComunicazioniStoricoByGroup(providerComunicazioni);
		}
		if(!user.isSegreteria()) {
			return comunicazioneRepository.countAllComunicazioniStoricoByAccount(user);
		}
		else {
			//l'utente è anche segreteria quindi amplio la query con le comunicazioni inviate alla segreteria
			//controllo se l'utente è nei destinatari o è il mittente, se il fake della segreteria è nei destinatari o se è stato inviata una comunicazione per suo conto;
			Account segreteriaComunicazioni = accountService.getAccountComunicazioniSegretereria();
			return comunicazioneRepository.countAllComunicazioniStoricoByAccountOrByGroup(user, segreteriaComunicazioni);
		}
	}

	@Override
	public int countAllComunicazioniNonRisposteByAccountId(Long id) {
		Account user = accountService.getUserById(id);
		if(user.isProviderVisualizzatore()) {
			Account providerComunicazioni = accountService.getAccountComunicazioniProviderForProvider(user.getProvider());
			return comunicazioneRepository.countAllComunicazioniNonRisposteByGroup(providerComunicazioni);
		}
		if(!user.isSegreteria()) {
			return comunicazioneRepository.countAllComunicazioniNonRisposteByAccount(user);
		}
		else {
			//l'utente è anche segreteria quindi amplio la query con le comunicazioni inviate alla segreteria
			//controllo se l'utente è nei destinatari o è il mittente, se il fake della segreteria è nei destinatari o se è stato inviata una comunicazione per suo conto;
			Account segreteriaComunicazioni = accountService.getAccountComunicazioniSegretereria();
			return comunicazioneRepository.countAllComunicazioniNonRisposteByAccountOrByGroup(user, segreteriaComunicazioni);
		}
	}

	@Override
	public int countAllComunicazioniAperteByAccountId(Long id) {
		Account user = accountService.getUserById(id);
		if(user.isProviderVisualizzatore()) {
			Account providerComunicazioni = accountService.getAccountComunicazioniProviderForProvider(user.getProvider());
			return comunicazioneRepository.countAllComunicazioniAperteByGroup(providerComunicazioni);
		}
		if(!user.isSegreteria()) {
			return comunicazioneRepository.countAllComunicazioniAperteByAccount(user);
		}
		else {
			//l'utente è anche segreteria quindi amplio la query con le comunicazioni inviate alla segreteria
			//controllo se l'utente è nei destinatari o è il mittente, se il fake della segreteria è nei destinatari o se è stato inviata una comunicazione per suo conto;
			Account segreteriaComunicazioni = accountService.getAccountComunicazioniSegretereria();
			return comunicazioneRepository.countAllComunicazioniAperteByAccountOrByGroup(user, segreteriaComunicazioni);
		}
	}

	@Override
	public Set<Comunicazione> getAllComunicazioniRicevuteByAccount(Account user) {
		if(user.isProviderVisualizzatore()) {
			Account providerComunicazioni = accountService.getAccountComunicazioniProviderForProvider(user.getProvider());
			return comunicazioneRepository.findAllComunicazioniRicevuteByAccount(providerComunicazioni);
		}
		if(!user.isSegreteria()) {
			return comunicazioneRepository.findAllComunicazioniRicevuteByAccount(user);
		}
		else {
			//l'utente è anche segreteria quindi amplio la query con le comunicazioni inviate alla segreteria
			Account segreteriaComunicazioni = accountService.getAccountComunicazioniSegretereria();
			return comunicazioneRepository.findAllComunicazioniRicevuteByAccountOrBySegreteria(user, segreteriaComunicazioni);
		}
	}

	@Override
	public Set<Comunicazione> getAllComunicazioniInviateByAccount(Account user) {
		Account accountComunicazioni = null;
		if(user.isProviderVisualizzatore()) {
			accountComunicazioni = accountService.getAccountComunicazioniProviderForProvider(user.getProvider());
		}
		else {
			//user appartiene alla segreteria dal momento che gli altri utenti possono aprire comunicazioni solo verso la segreteria
			accountComunicazioni = accountService.getAccountComunicazioniSegretereria();
		}
		//non ci sono altri casi poichè le altre tipologia di utenti non possono inviare comunicazioni, ma solo riceverle
		return comunicazioneRepository.findAllComunicazioniInviateForAccount(accountComunicazioni);
	}

	@Override
	public Set<Comunicazione> getAllComunicazioniChiuseByAccount(Account user) {
		if(user.isProviderVisualizzatore()) {
			Account providerComunicazioni = accountService.getAccountComunicazioniProviderForProvider(user.getProvider());
			return comunicazioneRepository.findAllComunicazioniChiuseByGroup(providerComunicazioni);
		}
		if(!user.isSegreteria()) {
			return comunicazioneRepository.findAllComunicazioniChiuseByAccount(user);
		}
		else {
			//l'utente è anche segreteria quindi amplio la query con le comunicazioni inviate alla segreteria
			//controllo se l'utente è nei destinatari o è il mittente, se il fake della segreteria è nei destinatari o se è stato inviata una comunicazione per suo conto;
			Account segreteriaComunicazioni = accountService.getAccountComunicazioniSegretereria();
			return comunicazioneRepository.findAllComunicazioniChiuseByAccountOrByGroup(user, segreteriaComunicazioni);
		}
	}

	@Override
	public Set<Comunicazione> getAllComunicazioniByAccount(Account user) {
		if(user.isProviderVisualizzatore()) {
			Account providerComunicazioni = accountService.getAccountComunicazioniProviderForProvider(user.getProvider());
			return comunicazioneRepository.findAllComunicazioniStoricoByGroup(providerComunicazioni);
		}
		if(!user.isSegreteria()) {
			return comunicazioneRepository.findAllComunicazioniStoricoByAccount(user);
		}
		else {
			//l'utente è anche segreteria quindi amplio la query con le comunicazioni inviate alla segreteria
			//controllo se l'utente è nei destinatari o è il mittente, se il fake della segreteria è nei destinatari o se è stato inviata una comunicazione per suo conto;
			Account segreteriaComunicazioni = accountService.getAccountComunicazioniSegretereria();
			return comunicazioneRepository.findAllComunicazioniStoricoByAccountOrByGroup(user, segreteriaComunicazioni);
		}
	}

	@Override
	public Set<Comunicazione> getAllComunicazioniNonRisposteByAccount(Account user) {
		if(user.isProviderVisualizzatore()) {
			Account providerComunicazioni = accountService.getAccountComunicazioniProviderForProvider(user.getProvider());
			return comunicazioneRepository.findAllComunicazioniNonRisposteByGroup(providerComunicazioni);
		}
		if(!user.isSegreteria()) {
			return comunicazioneRepository.findAllComunicazioniNonRisposteByAccount(user);
		}
		else {
			//l'utente è anche segreteria quindi amplio la query con le comunicazioni inviate alla segreteria
			//controllo se l'utente è nei destinatari o è il mittente, se il fake della segreteria è nei destinatari o se è stato inviata una comunicazione per suo conto;
			Account segreteriaComunicazioni = accountService.getAccountComunicazioniSegretereria();
			return comunicazioneRepository.findAllComunicazioniNonRisposteByAccountOrByGroup(user, segreteriaComunicazioni);
		}
	}

	@Override
	public Set<Comunicazione> getAllComunicazioniAperteByAccount(Account user) {
		if(user.isProviderVisualizzatore()) {
			Account providerComunicazioni = accountService.getAccountComunicazioniProviderForProvider(user.getProvider());
			return comunicazioneRepository.findAllComunicazioniAperteByGroup(providerComunicazioni);
		}
		if(!user.isSegreteria()) {
			return comunicazioneRepository.findAllComunicazioniAperteByAccount(user);
		}
		else {
			//l'utente è anche segreteria quindi amplio la query con le comunicazioni inviate alla segreteria
			//controllo se l'utente è nei destinatari o è il mittente, se il fake della segreteria è nei destinatari o se è stato inviata una comunicazione per suo conto;
			Account segreteriaComunicazioni = accountService.getAccountComunicazioniSegretereria();
			return comunicazioneRepository.findAllComunicazioniAperteByAccountOrByGroup(user, segreteriaComunicazioni);
		}
	}

	@Override
	public List<Comunicazione> getUltimi10MessaggiNonLetti(Long id) {
		Page<Comunicazione> results = comunicazioneRepository.findMessaggiNonLettiOrderByDataUltimaModificaDesc(id, new PageRequest(0, 10));
		List<Comunicazione> ultimi10MessaggiNonLetti= results.getContent();
		for(Comunicazione c : ultimi10MessaggiNonLetti) {
			c.getDataUltimaModifica().toString();
		}
		return ultimi10MessaggiNonLetti;
	}

	@Override
	public long countAllMessaggiNonLetti(Long id) {
		return comunicazioneRepository.countAllMessaggiNonLetti(id);
	}

	@Override
	public long getIdUltimaComunicazioneRicevuta(Long id) {
		Page<Comunicazione> results = comunicazioneRepository.findMessaggiNonLettiOrderByDataUltimaModificaDesc(id, new PageRequest(0, 1));
		List<Comunicazione> ultimoMessaggioNonLetto = results.getContent();
		if (!ultimoMessaggioNonLetto.isEmpty())
			return ultimoMessaggioNonLetto.get(0).getId();
		else return 0;
	}

	//controlla l'utente se segreteria o provider (gli unici a poter creare da 0 una comunicazine)
	//e restituisce la lista di tutti i possibili destinatari
	@Override
	public Map<String, Set<Account>> getAllDestinatariDisponibili(Long id) {
		Map<String, Set<Account>> destinatariDisponibili = new HashMap<String, Set<Account>>();
		Account richiedente = accountService.getUserById(id);
		//caso segreteria invia a tutti (tranne ad altri segretari)
		if(richiedente.isSegreteria()) {
			Set<Account> providerSet = new HashSet<Account>();
			Set<Account> commissioneSet = new HashSet<Account>();
			Set<Account> refereeSet = new HashSet<Account>();
			Set<Account> osservatoriSet = new HashSet<Account>();
			Set<Account> allUsers = accountService.getAllUsers();
			for(Account a : allUsers) {
				if(!a.isSegreteria() && a.isProviderAccountComunicazioni()) {
					try {
						Accreditamento accreditamento = accreditamentoService.getAccreditamentoAttivoForProvider(a.getProvider().getId());
						if(accreditamento != null && a.getProvider().isAttivo())
							providerSet.add(a);
					}catch (Exception e) {
						// se genera eccezione smplicemente non includo il provider nella lista
					}
				}
				if(!a.isSegreteria() && a.isCommissioneEcm()) {
					commissioneSet.add(a);
				}
				if(!a.isSegreteria() && a.isReferee()) {
					refereeSet.add(a);
				}
				if(!a.isSegreteria() && a.isComponenteOsservatorioEcm()) {
					osservatoriSet.add(a);
				}
			}
			destinatariDisponibili.put("Provider", providerSet);
			destinatariDisponibili.put("Commissione ECM", commissioneSet);
			destinatariDisponibili.put("Referee ECM", refereeSet);
			destinatariDisponibili.put("Osservatori ECM", osservatoriSet);
		}
		return destinatariDisponibili;
	}

	//tiommi 04-04-2017 : dashboard in comune tra gli utenti
	//tiommi 06/03/2017 a seguito richiesta MEV in cui le comunicazioni sono gestite per gruppi provider e segreteria
	//salvataggio comunicazione, controllo mittente, se è provider aggiungo i segretari ai destinatari
	//aggiungo anche la data di invio
	@Override
	public void send(Comunicazione comunicazione, File allegato) throws Exception {
		//inserisco gli id dei destinatari tra gli utenti che non hanno ancora letto la comunicazione
		Set<Long> utentiCheDevonoLeggere =  new HashSet<Long>();
		//gestioni mittente NON segreteria (possono inviare solo a segreteria)
		if(!comunicazione.getMittente().isSegreteria()) {
			//aggiungo ai destinatari il fake account segreteria comunicazioni
			Set<Account> destinatari = new HashSet<Account>();
			destinatari.add(accountService.getAccountComunicazioniSegretereria());
			comunicazione.setDestinatari(destinatari);
			comunicazione.setInviatoAllaSegreteria(true);
			//inserisco negli utenti che devono leggere tutti gli utenti segreteria
			Set<Account> membriSegreteria = accountService.getUserByProfileEnum(ProfileEnum.SEGRETERIA);
			for(Account s : membriSegreteria)
				utentiCheDevonoLeggere.add(s.getId());
			//se ad inviare è un provider
			if(comunicazione.getMittente().isProvider()) {
				//aggiungo il fake account del provider
				Account providerComunicazioni = accountService.getAccountComunicazioniProviderForProvider(comunicazione.getMittente().getProvider());
				comunicazione.setFakeAccountComunicazioni(providerComunicazioni);
			}
		}
		//gestione mittente segreteria
		else {
			comunicazione.setInviatoAllaSegreteria(false);
			for (Account a : comunicazione.getDestinatari()) {
				//tiommi 06/03/2017 a seguito richiesta MEV in cui le comunicazioni sono gestite per gruppi provider e segreteria
				//se trovo un provider aggiungo agli utenti che devono leggere tutti gli utenti del provider
				//N.B. la segreteria può aprire comunicazioni con il provider solo attraverso fake account comunicazione provider
				if(a.isProviderAccountComunicazioni()) {
					Set<Account> providerUsers = accountService.getAllByProviderId(a.getProvider().getId());
					for(Account pu : providerUsers)
						if(!pu.isProviderAccountComunicazioni())
							utentiCheDevonoLeggere.add(pu.getId());
					inviaMailLegaleProvider(a.getProvider());
				}
				else
					utentiCheDevonoLeggere.add(a.getId());
			}
			//aggiungo il fake account della segreteria
			Account segreteriaComunicazioni = accountService.getAccountComunicazioniSegretereria();
			comunicazione.setFakeAccountComunicazioni(segreteriaComunicazioni);
		}
		comunicazione.setUtentiCheDevonoLeggere(utentiCheDevonoLeggere);
		comunicazione.setDataCreazione(LocalDateTime.now());
		comunicazione.setDataUltimaModifica(LocalDateTime.now());
		if (allegato != null && !allegato.isNew())
			comunicazione.setAllegatoComunicazione(allegato);
		comunicazioneRepository.save(comunicazione);
	}

	//contollo se il provider ha il legale rappresentante e il delegato del legale rappresentante e in caso
	//positivo li avvisa che è stata inviata una nuova comunicazione al loro Provider
	private void inviaMailLegaleProvider(Provider provider) {
		Persona legale = provider.getLegaleRappresentante();
		Persona delegato = provider.getDelegatoLegaleRappresentante();
		if(legale != null) {
			try {
				emailService.inviaNotificaNuovaComunicazioneForProvider(legale.getAnagrafica().getFullName(), legale.getAnagrafica().getEmail());
			}
			catch (Exception ex) {
				LOGGER.error(Utils.getLogMessage("ERRORE nell'invio della mail al legale rappresentante del Provider " + provider.getId()));
			}
		}
		if(delegato != null) {
			try {
				emailService.inviaNotificaNuovaComunicazioneForProvider(delegato.getAnagrafica().getFullName(), delegato.getAnagrafica().getEmail());
			}
			catch (Exception ex) {
				LOGGER.error(Utils.getLogMessage("ERRORE nell'invio della mail al delegato del legale rappresentante del Provider " + provider.getId()));
			}
		}
	}

	@Override
	public boolean canAccountRespondToComunicazione(Account account, Comunicazione comunicazione) {
		if(comunicazione.isChiusa())
			return false;
		if(account.isSegreteria())
			return true;
		if(account.isProvider()){
			Account providerComunicazioni = accountService.getAccountComunicazioniProviderForProvider(account.getProvider());
			Set<Account> providerUsers = accountService.getAllByProviderId(account.getProvider().getId());
			if(comunicazione.getDestinatari().contains(providerComunicazioni) || providerUsers.contains(comunicazione.getMittente()))
				return true;
		}
		else
			if(comunicazione.getDestinatari().contains(account) || comunicazione.getMittente().equals(account))
				return true;
		return false;
	}

	@Override
	public boolean canAccountSeeResponse(Account account, ComunicazioneResponse response) {
		if(account.isSegreteria())
			return true;
		if(account.isProvider()){
			Account providerComunicazioni = accountService.getAccountComunicazioniProviderForProvider(account.getProvider());
			Set<Account> providerUsers = accountService.getAllByProviderId(account.getProvider().getId());
			if(response.getDestinatari().contains(providerComunicazioni) || providerUsers.contains(response.getMittente()))
				return true;
		}
		else
			if(response.getDestinatari().contains(account) || response.getMittente().equals(account))
				return true;
		return false;
	}


	@Override
	public boolean canAccountCloseComunicazione(Account account, Comunicazione comunicazione) {
		if(!comunicazione.isChiusa() && account.isSegreteria())
			return true;
		return false;
	}

	//tiommi 04-04-2017 : dashboard in comune tra gli utenti
	@Override
	public void contrassegnaComeLetta(Long id) {
		Comunicazione comunicazione = getComunicazioneById(id);
		Set<Long> utentiCheDevonoLeggere = comunicazione.getUtentiCheDevonoLeggere();
		Account user = Utils.getAuthenticatedUser().getAccount();
		//tolgo l'utente che ha archiviato
		utentiCheDevonoLeggere.remove(user.getId());
		//se l'utente è provider tolgo tutti gli utenti del suo provider
		if(user.isProviderVisualizzatore()) {
			Set<Account> providerUsers = accountService.getAllByProviderId(user.getProvider().getId());
			for(Account pu : providerUsers)
				utentiCheDevonoLeggere.remove(pu.getId());
		}
		//se l'utente è segreteria tolgo tutti gli utenti segreteria
		else if(user.isSegreteria()) {
			Set<Account> segreteriaUsers = accountService.getUserByProfileEnum(ProfileEnum.SEGRETERIA);
			for(Account s : segreteriaUsers)
				utentiCheDevonoLeggere.remove(s.getId());
		}
		comunicazione.setUtentiCheDevonoLeggere(utentiCheDevonoLeggere);
		comunicazioneRepository.save(comunicazione);
	}

	//tiommi 04-04-2017 : dashboard in comune tra gli utenti
	//salvataggio risposta per la comunicazione il cui id è passato come parametro
	@Override
	public void reply(ComunicazioneResponse risposta, Long id, File allegato) {
		Comunicazione comunicazione = getComunicazioneById(id);
		Set<ComunicazioneResponse> risposte = comunicazione.getRisposte();
		risposta.setDataRisposta(LocalDateTime.now());
		comunicazione.setDataUltimaModifica(LocalDateTime.now());
		risposta.setComunicazione(comunicazione);
		if (allegato != null && !allegato.isNew())
			risposta.setAllegatoRisposta(allegato);
		risposte.add(risposta);
		Set<Long> utentiCheDevonoLeggere = comunicazione.getUtentiCheDevonoLeggere();
		//risposta di un utente NON segreteria, allerto i membri della segreteria
		if(!risposta.getMittente().isSegreteria()) {
			//aggiungo ai destinatari il fake account segreteria comunicazioni
			Set<Account> destinatari = new HashSet<Account>();
			destinatari.add(accountService.getAccountComunicazioniSegretereria());
			risposta.setDestinatari(destinatari);
			risposta.setInviatoAllaSegreteria(true);
			//allerto tutti i segreteria (dashboard)
			for(Account a : accountService.getUserByProfileEnum(ProfileEnum.SEGRETERIA))
				utentiCheDevonoLeggere.add(a.getId());
			if(risposta.getMittente().isProvider()) {
				//aggiungo il fake account del provider come mittente
				Account providerComunicazioni = accountService.getAccountComunicazioniProviderForProvider(risposta.getMittente().getProvider());
				risposta.setFakeAccountComunicazioni(providerComunicazioni);
				//rimuovo tutti gli utenti del provider dagli utenti che devono leggere
				Set<Account> providerUsers = accountService.getAllByProviderId(risposta.getMittente().getProvider().getId());
				for(Account pu : providerUsers)
					utentiCheDevonoLeggere.remove(pu.getId());
			}
		}
		//risposta di un utente segreteria, allerto tutti i destinatari della risposta
		// se tra questi vi sono provider, allerto tutti gli utenti del provider.
		else{
			risposta.setInviatoAllaSegreteria(false);
			for(Account a : risposta.getDestinatari())
				//allerto della nuova comunicazione
				if(a.isProviderAccountComunicazioni()) {
					Set<Account> providerUsers = accountService.getAllByProviderId(a.getProvider().getId());
					for(Account pu : providerUsers)
						utentiCheDevonoLeggere.add(pu.getId());
				}
				else
					utentiCheDevonoLeggere.add(a.getId());
			//aggiungo il fake account della segreteria
			Account segreteriaComunicazioni = accountService.getAccountComunicazioniSegretereria();
			risposta.setFakeAccountComunicazioni(segreteriaComunicazioni);
			//rimuovo tutti gli utenti segreteria dagli utenti che devono leggere
			Set<Account> segreteriaUsers = accountService.getUserByProfileEnum(ProfileEnum.SEGRETERIA);
			for(Account s : segreteriaUsers)
				utentiCheDevonoLeggere.remove(s.getId());
		}
		//tolgo l'utente che ha mandato la risposta dagli utenti che devono leggere
		utentiCheDevonoLeggere.remove(risposta.getMittente().getId());
		comunicazione.setUtentiCheDevonoLeggere(utentiCheDevonoLeggere);
		comunicazioneResponseRepository.save(risposta);
		comunicazione.setRisposte(risposte);
		comunicazioneRepository.save(comunicazione);
	}

	//tiommi 04-04-2017 : dashboard in comune tra gli utenti
	@Override
	public void chiudiComunicazioneById(Long id) {
		Comunicazione comunicazione = comunicazioneRepository.findOne(id);
		comunicazione.setChiusa(true);
		//NB solo la segreteria può chiamare questo metodo, rimuovo tutti gli utenti segreteria (come se avesse letto)
		//ma non gli utenti provider
		Set<Long> utentiCheDevonoLeggere = comunicazione.getUtentiCheDevonoLeggere();
		Set<Account> segreteriaUsers = accountService.getUserByProfileEnum(ProfileEnum.SEGRETERIA);
		for(Account s : segreteriaUsers)
			utentiCheDevonoLeggere.remove(s.getId());
		comunicazione.setUtentiCheDevonoLeggere(utentiCheDevonoLeggere);
		comunicazioneRepository.save(comunicazione);
	}

	@Override
	public Set<Comunicazione> getAllComunicazioniNonLetteByAccount(Account user) {
		return comunicazioneRepository.findAllComunicazioneNonLetteOrderByDataUltimaModificaDesc(user.getId());
	}

	@Override
	public Set<Comunicazione> getAllComunicazioniByProvider(Provider provider) {
		Account providerComunicazione = accountService.getAccountComunicazioniProviderForProvider(provider);
		return comunicazioneRepository.findAllComunicazioniStoricoByGroup(providerComunicazione);
	}

	@Override
	public Set<Comunicazione> getAllComunicazioniNonRisposteFromProviderBySegreteria(Provider provider) {
		//lista delle comunicazioni dal provider alla segreteria alle quali la segreteria non ha ancora risposto
		Account providerComunicazione = accountService.getAccountComunicazioniProviderForProvider(provider);
		Account segreteriaComunicazione = accountService.getAccountComunicazioniSegretereria();
		return comunicazioneRepository.findAllComunicazioniNonRisposteFromGroupByGroup(providerComunicazione, segreteriaComunicazione);
	}

	@Override
	public List<Comunicazione> cerca(RicercaComunicazioneWrapper wrapper) {
		String query = "";
		HashMap<String, Object> params = new HashMap<String, Object>();

		query ="SELECT c FROM Comunicazione c JOIN c.destinatari d JOIN c.mittente m";

		if(wrapper.getDenominazioneLegale() != null && !wrapper.getDenominazioneLegale().isEmpty()){
			//devo fare il join con la tabella provider
			query = Utils.QUERY_AND(query,"UPPER(d.provider.denominazioneLegale) LIKE :denominazioneLegale");
			params.put("denominazioneLegale", "%" + wrapper.getDenominazioneLegale().toUpperCase() + "%");
		}

		//USER ID (per non segreteria - REFEREE e COMMISSIONE)
		if(wrapper.getUserId() != null){
			query = Utils.QUERY_AND(query, "(d.id = :userId OR m.id = :userId)");
			params.put("userId", wrapper.getUserId());
		}

		//PROVIDER ID
		if(wrapper.getCampoIdProvider() != null){
			query = Utils.QUERY_AND(query, "(d.provider.id = :providerId OR m.provider.id = :providerId)");
			params.put("providerId", wrapper.getCampoIdProvider());
		}

		//OGGETTO
		if(wrapper.getOggetto() != null && !wrapper.getOggetto().isEmpty()){
			query = Utils.QUERY_AND(query, "UPPER(c.oggetto) LIKE :oggetto");
			params.put("oggetto", "%" + wrapper.getOggetto().toUpperCase() + "%");
		}

		//AMBITO
		if(wrapper.getAmbitiSelezionati() != null){
			query = Utils.QUERY_AND(query, "c.ambito IN (:ambitiSelezionati)");
			params.put("ambitiSelezionati", wrapper.getAmbitiSelezionati());
		}

		//TIPOLOGIA
		if(wrapper.getTipologieSelezionate() != null){
			query = Utils.QUERY_AND(query, "c.tipologia IN (:tipologieSelezionate)");
			params.put("tipologieSelezionate", wrapper.getTipologieSelezionate());
		}

		//DATA CREAZIONE
		if(wrapper.getDataCreazioneStart() != null){
			query = Utils.QUERY_AND(query, "c.dataCreazione >= :dataCreazioneStart");
			LocalDateTime ldt = Timestamp.valueOf(wrapper.getDataCreazioneStart().atStartOfDay()).toLocalDateTime();
			params.put("dataCreazioneStart", ldt);
		}

		if(wrapper.getDataCreazioneEnd() != null){
			query = Utils.QUERY_AND(query, "c.dataCreazione <= :dataCreazioneEnd");
			LocalDateTime ldt = Timestamp.valueOf(wrapper.getDataCreazioneEnd().plusDays(1).atStartOfDay()).toLocalDateTime();
			params.put("dataCreazioneEnd", ldt);
		}


		LOGGER.info(Utils.getLogMessage("Cerca Comunicazione: " + query));
		Query q = entityManager.createQuery(query, Comunicazione.class);

		Iterator<Entry<String, Object>> iterator = params.entrySet().iterator();
		while(iterator.hasNext()){
			Map.Entry<String, Object> pairs = iterator.next();
			q.setParameter(pairs.getKey(), pairs.getValue());
			LOGGER.info(Utils.getLogMessage(pairs.getKey() + ": " + pairs.getValue()));
		}

		List<Comunicazione> result = q.getResultList();

		return result;
	}

	@Override
	public HashMap<Long, Boolean> createMappaVisibilitaResponse(Account account, Comunicazione comunicazione) {
		HashMap<Long, Boolean> mappa = new HashMap<Long, Boolean>();
		for(ComunicazioneResponse cr : comunicazione.getRisposte()) {
			mappa.put(cr.getId(), canAccountSeeResponse(account, cr));
		}
		return mappa;
	}

	@Override
	public void archiviaSelezionati(Set<Long> ids) {
		Account user = Utils.getAuthenticatedUser().getAccount();
		for(Long id : ids) {
			Comunicazione comunicazione = comunicazioneRepository.findOne(id);
			if(comunicazione != null) {
				Set<Long> utentiCheDevonoLeggere = comunicazione.getUtentiCheDevonoLeggere();
				//tolgo l'utente che ha archiviato
				utentiCheDevonoLeggere.remove(user.getId());
				//se l'utente è provider tolgo tutti gli utenti del suo provider
				if(user.isProviderVisualizzatore()) {
					Set<Account> providerUsers = accountService.getAllByProviderId(user.getProvider().getId());
					for(Account pu : providerUsers)
						utentiCheDevonoLeggere.remove(pu.getId());
				}
				//se l'utente è segreteria tolgo tutti gli utenti segreteria
				else if(user.isSegreteria()) {
					Set<Account> segreteriaUsers = accountService.getUserByProfileEnum(ProfileEnum.SEGRETERIA);
					for(Account s : segreteriaUsers)
						utentiCheDevonoLeggere.remove(s.getId());
				}
				comunicazione.setUtentiCheDevonoLeggere(utentiCheDevonoLeggere);
				comunicazioneRepository.save(comunicazione);
			}
		}

	}

}
