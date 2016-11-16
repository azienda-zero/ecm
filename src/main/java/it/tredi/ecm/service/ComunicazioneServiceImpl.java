package it.tredi.ecm.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import it.tredi.ecm.dao.entity.Account;
import it.tredi.ecm.dao.entity.Comunicazione;
import it.tredi.ecm.dao.entity.ComunicazioneResponse;
import it.tredi.ecm.dao.entity.File;
import it.tredi.ecm.dao.enumlist.ProfileEnum;
import it.tredi.ecm.dao.repository.ComunicazioneRepository;
import it.tredi.ecm.dao.repository.ComunicazioneResponseRepository;
import it.tredi.ecm.utils.Utils;

@Service
public class ComunicazioneServiceImpl implements ComunicazioneService {
	private static Logger LOGGER = LoggerFactory.getLogger(ComunicazioneServiceImpl.class);

	@Autowired private AccountService accountService;
	@Autowired private ProviderService providerService;
	@Autowired private ComunicazioneRepository comunicazioneRepository;
	@Autowired private ComunicazioneResponseRepository comunicazioneResponseRepository;

	@Override
	public Comunicazione getComunicazioneById(Long id) {
		return comunicazioneRepository.findOne(id);
	}

	@Override
	public int countAllComunicazioniRicevuteByAccountId(Long id) {
		Account user = accountService.getUserById(id);
		return comunicazioneRepository.countAllComunicazioniReceivedByAccount(user);
	}

	@Override
	public int countAllComunicazioniInviateByAccountId(Long id) {
		Account user = accountService.getUserById(id);
		return comunicazioneRepository.countAllComunicazioniByMittente(user);
	}

	@Override
	public int countAllComunicazioniBloccateByAccountId(Long id) {
		Account user = accountService.getUserById(id);
		return comunicazioneRepository.countAllComunicazioniChiuseForUser(user);
	}

	@Override
	public Comunicazione getUltimaComunicazioneCreata(Long id) {
		Account user = accountService.getUserById(id);
		return comunicazioneRepository.findFirstByMittenteOrderByDataCreazioneDesc(user);
	}

	@Override
	public List<Comunicazione> getUltimi10MessaggiNonLetti(Long id) {
		Page<Comunicazione> results = comunicazioneRepository.findMessaggiNonLettiOrderByDataCreazioneDesc(id, new PageRequest(0, 10));
		List<Comunicazione> ultimi10MessaggiNonLetti= results.getContent();
		return ultimi10MessaggiNonLetti;
	}

	@Override
	public long countAllMessaggiNonLetti(Long id) {
		return comunicazioneRepository.countAllMessaggiNonLetti(id);
	}

	@Override
	public long getIdUltimaComunicazioneRicevuta(Long id) {
		Page<Comunicazione> results = comunicazioneRepository.findMessaggiNonLettiOrderByDataCreazioneDesc(id, new PageRequest(0, 1));
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
				if(!a.isSegreteria() && a.isProviderUserAdmin()) {
					providerSet.add(a);
				}
				if(!a.isSegreteria() && a.isCommissioneEcm()) {
					commissioneSet.add(a);
				}
				if(!a.isSegreteria() && a.isReferee()) {
					refereeSet.add(a);
				}
				if(!a.isSegreteria() && a.isOsservatoreEcm()) {
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

	//salvataggio comunicazione, controllo mittente, se è provider aggiungo i segretari ai destinatari
	//aggiungo anche la data di invio
	@Override
	public void send(Comunicazione comunicazione, File allegato) {
		if(comunicazione.getMittente().isProvider()) {
			comunicazione.setDestinatari(accountService.getUserByProfileEnum(ProfileEnum.SEGRETERIA));
		}
		//inserisco gli id dei destinatari tra gli utenti che non hanno ancora letto la comunicazione
		Set<Long> utentiCheDevonoLeggere =  new HashSet<Long>();
		for (Account a : comunicazione.getDestinatari()) {
			utentiCheDevonoLeggere.add(a.getId());
		}
		comunicazione.setUtentiCheDevonoLeggere(utentiCheDevonoLeggere);
		comunicazione.setDataCreazione(LocalDateTime.now());
		comunicazione.setDataUltimaModifica(LocalDateTime.now());
		if (allegato != null && !allegato.isNew())
			comunicazione.setAllegatoComunicazione(allegato);
		comunicazioneRepository.save(comunicazione);
	}

	@Override
	public boolean canAccountRespondToComunicazione(Account account, Comunicazione comunicazione) {
		if(!comunicazione.isChiusa() && (comunicazione.getDestinatari().contains(account) || comunicazione.getMittente().equals(account)))
			return true;
		else return false;
	}

	@Override
	public boolean canAccountCloseComunicazione(Account account, Comunicazione comunicazione) {
		if(!comunicazione.isChiusa() && account.isSegreteria())
			return true;
		return false;
	}

	@Override
	public void contrassegnaComeLetta(Long id) {
		Comunicazione comunicazione = getComunicazioneById(id);
		Set<Long> utentiCheDevonoLeggere = comunicazione.getUtentiCheDevonoLeggere();
		utentiCheDevonoLeggere.remove(Utils.getAuthenticatedUser().getAccount().getId());
		comunicazione.setUtentiCheDevonoLeggere(utentiCheDevonoLeggere);
		comunicazioneRepository.save(comunicazione);
	}

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
		utentiCheDevonoLeggere.add(comunicazione.getMittente().getId());
		//aggiungo tutti i destinatari alla lista degli id utente che devono rileggere la comunicazione
		for (Account a : comunicazione.getDestinatari()) {
			utentiCheDevonoLeggere.add(a.getId());
		}
		//rimuovo l'utente che ha creato la risposta
		utentiCheDevonoLeggere.remove(Utils.getAuthenticatedUser().getAccount().getId());
		//e salvo
		comunicazioneResponseRepository.save(risposta);
		comunicazione.setRisposte(risposte);
		comunicazioneRepository.save(comunicazione);
	}

	@Override
	public Set<Comunicazione> getAllComunicazioniRicevuteByAccount(Account user) {
		return comunicazioneRepository.findAllComunicazioniByDestinatario(user);
	}

	@Override
	public Set<Comunicazione> getAllComunicazioniInviateByAccount(Account user) {
		return comunicazioneRepository.findAllComunicazioneByMittente(user);
	}

	@Override
	public Set<Comunicazione> getAllComunicazioniChiuseByAccount(Account user) {
		return comunicazioneRepository.findAllComunicazioneChiusaByUser(user);
	}

	@Override
	public void chiudiComunicazioneById(Long id) {
		Comunicazione comunicazione = comunicazioneRepository.findOne(id);
		comunicazione.setChiusa(true);
		comunicazioneRepository.save(comunicazione);
	}

	@Override
	public int countAllComunicazioniByAccountId(Long id) {
		Account user = accountService.getUserById(id);
		return comunicazioneRepository.countAllComunicazioniByAccount(user);
	}

	@Override
	public Set<Comunicazione> getAllComunicazioniByAccount(Account user) {
		return comunicazioneRepository.findAllComunicazioneByUser(user);
	}

	@Override
	public Set<Comunicazione> getAllComunicazioniNonLetteByAccount(Account user) {
		return comunicazioneRepository.findAllComunicazioneNonLetteOrderByDataCreazioneDesc(user.getId());
	}
}
